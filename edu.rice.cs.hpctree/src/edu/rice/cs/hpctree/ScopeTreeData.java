package edu.rice.cs.hpctree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.slf4j.LoggerFactory;

import edu.rice.cs.hpcdata.experiment.metric.BaseMetric;
import edu.rice.cs.hpcdata.experiment.metric.IMetricManager;
import edu.rice.cs.hpcdata.experiment.metric.MetricValue;
import edu.rice.cs.hpcdata.experiment.scope.RootScope;
import edu.rice.cs.hpcdata.experiment.scope.Scope;


/******************************************************
 * 
 * Default ITreeData of hpcviewer tree 
 *
 ******************************************************/
public class ScopeTreeData implements IScopeTreeData
{
	/** list of current data. The list is dynamic **/
	private final MutableList<Scope>    listScopes;	

	private List<Integer> indexesNonEmptyMetrics;
	private IMetricManager metricManager;
	private Scope root;
	
	// attributes to handle sorting
	private int sortedColumn;
	private SortDirectionEnum sortDirection;

	
	/***
	 * Constructor to create a tree data based on the root
	 * @param root the root scope
	 * @param metricManager the metric manager of the experiment or database
	 */
	public ScopeTreeData(RootScope root, IMetricManager metricManager) {
		this.listScopes = FastList.newList();
		this.listScopes.add(root);
		this.metricManager = metricManager;
		this.root = root;
		
		// fix issue #277: use the root to get the metrics
		// if we use the scope, some exclusive metrics will disappear
		this.indexesNonEmptyMetrics = metricManager.getNonEmptyMetricIDs(root);
		
		clear();
	}
	
	
	@Override
	public void refreshAndShift(int shift) {
		this.sortedColumn += 1;
		
		// fix issue #277: use the root to get the metrics
		// if we use the scope, some exclusive metrics will disappear
		this.indexesNonEmptyMetrics = metricManager.getNonEmptyMetricIDs(root.getRootScope());
	}
	
	
	@Override
	public List<Scope> getList() {
		return listScopes;
	}
	
	
	@Override
	public void setRoot(Scope root) {
		listScopes.clear();		
		listScopes.add(root);
		this.root = root;
	}
		
	
	/****
	 * Get the root of this tree data
	 * @return RootScope
	 */
	@Override
	public Scope getRoot() {
		return root;
	}
	
	
	
	/** 
	 * Reset the data
	 */
	@Override
	public void clear() {
		this.sortDirection = SortDirectionEnum.DESC;
		this.sortedColumn  = 0;
		if (this.indexesNonEmptyMetrics != null && !this.indexesNonEmptyMetrics.isEmpty())
			this.sortedColumn = 1;
		
		// fix issue #277: use the root to get the metrics
		// if we use the scope, some exclusive metrics will disappear
		this.indexesNonEmptyMetrics = metricManager.getNonEmptyMetricIDs(root.getRootScope());
	}

	
	/***
	 * Method to notify to sort the data based on certain column and direction
	 * @param columnIndex the column index. Must be greater or equal to 0
	 * @param sortDirection {@code SortDirectionEnum}
	 * @param accumulate
	 */
	@Override
	public void sort(int columnIndex, SortDirectionEnum sortDirection, boolean accumulate) {
		this.sortDirection = sortDirection;
		
		// We only allow 2 types of sort: ascending and descending
		// other than that (case for none), we have to convert it		
		if (sortDirection.equals(SortDirectionEnum.NONE)) {
			if (columnIndex == sortedColumn) {
				this.sortDirection = this.sortDirection.equals(SortDirectionEnum.ASC)?
						 SortDirectionEnum.DESC : SortDirectionEnum.ASC;
			} else {
				this.sortDirection = SortDirectionEnum.DESC;
			}
		}
		sortedColumn = columnIndex;

		ColumnComparator comparator = getComparator(columnIndex, this.sortDirection);
		synchronized (listScopes) {
			try {
				listScopes.sort(comparator);
			} catch (IllegalArgumentException e) {
				// error in sorting the column
				LoggerFactory.getLogger(getClass()).error("column: " + columnIndex + ", direction: " + sortDirection, e);
			}
		}
	}
	
	
	protected ColumnComparator getComparator(int columnIndex, SortDirectionEnum sortDir) {
		assert(sortDir != SortDirectionEnum.NONE);
		
		return new ColumnComparator(this, columnIndex, sortDir);
	}
	
	

	@Override
	public int getSortedColumn() {
		return sortedColumn;
	}


	@Override
	public SortDirectionEnum getSortDirection() {
		return sortDirection;
	}
	
	
	@Override
	public IMetricManager getMetricManager() {
		return metricManager;
	}
	
	@Override
	public BaseMetric getMetric(int indexMetricColumn) {
		int id = indexesNonEmptyMetrics.get(indexMetricColumn);
		return metricManager.getMetric(id);
	}
	
			
	@Override
	public int getMetricCount() {
		return indexesNonEmptyMetrics.size();
	}
		

	
	protected boolean isRootScope(Scope scope) {
		return (scope == null) || 
			   (scope instanceof RootScope) || 
			   (scope.getClass() == root.getClass() &&
			   (scope.getCCTIndex() == root.getCCTIndex()) );
	}
	
	
	@Override
	public List<Scope> getPath(Scope node) {
		FastList<Scope> path = FastList.newList();
		Scope current = node;
		while(current != null  &&  !isRootScope(current)) {
			path.add(current);
			current = current.getParentScope();
		}
		return path.reverseThis();
	}
	
	@Override
	public int getDepthOfData(Scope object) {
		if (object == null || isRootScope(object)) return 0;
		
		int depth = 0;
		Scope scope = object;
		while (scope.getParentScope() != null && !isRootScope(scope)) {
			depth++;
			scope = scope.getParentScope();
		}
		return depth;
	}

	@Override
	public int getDepthOfData(int index) {
		return getDepthOfData(getDataAtIndex(index));
	}

	@Override
	public Scope getDataAtIndex(int index) {
		return listScopes.get(index);
	}

	
	@Override
	public int indexOf(Scope child) {
		return listScopes.indexOf(child);
	}

	
	@Override
	public int indexOfBasedOnCCT(int cctIndex) {
		for(int i=0; i<listScopes.size(); i++) {
			if (listScopes.get(i).getCCTIndex() == cctIndex)
				return i;
		}
		return -1;
	}
	
	@Override
	public boolean hasChildren(Scope object) {
		return object.getSubscopeCount()>0;
	}

	
	@Override
	public boolean hasChildren(int index) {
		return hasChildren(getDataAtIndex(index));
	}

	
	@Override
	public List<Scope> getChildren(Scope scope) {
		
		// if the node doesn't exist, it should be a critical error
		// should we throw an exception?		
		int numChildren = scope.getSubscopeCount();
		if (numChildren == 0)
			return new ArrayList<>(0);
		
		// get the children from the original tree, and sort them
		// based on the sorted column (either metric or tree column)
		List<Scope> children = scope.getChildren();
		final BaseMetric metric = sortedColumn == 0 ? null : getMetric(sortedColumn-1);
		children.sort((s1, s2) -> compareNodes(s1, s2, metric, sortDirection));
		
		return children;
	}

	
	@Override
	public List<Scope> getChildren(Scope object, boolean fullDepth) {
		return getChildren(object);
	}
	

	@Override
	public List<Scope> getChildren(int index) {
		Scope scope = getDataAtIndex(index);
		return getChildren(scope);
	}

	
	@Override
	public int getElementCount() {
		return listScopes.size();
	}

	
	@Override
	public boolean isValidIndex(int index) {
		return (index >= 0) && (index < listScopes.size());
	}

	
	/********************************************************************
	 * 
	 * comparator for the table. 
	 * If it's a metric column, we compare the value.
	 * If it's a tree column, we compare the name and its line number
	 *
	 ********************************************************************/
	private static class ColumnComparator implements Comparator<Scope> 
	{
		private final BaseMetric metric;
		private final SortDirectionEnum dir;
		private final IScopeTreeData treeData;
		
		public ColumnComparator(IScopeTreeData treeData, int columnIndex, SortDirectionEnum dir) {
			this.treeData = treeData;
			this.dir = dir;
			if (columnIndex == 0)
				metric = null;
			else
				metric = treeData.getMetric(columnIndex-1);
		}
		
		@Override
		public int compare(Scope o1, Scope o2) {
            int result = 0;
			if (o1.getParentScope() != null && o2.getParentScope() != null) {
				int d1 = treeData.getDepthOfData(o1);
				int d2 = treeData.getDepthOfData(o2);
				
				var path1 = treeData.getPath(o1);
				var path2 = treeData.getPath(o2);
				
				for(int d=0; d < d1 && d < d2; d++) {
					var node1 = path1.get(d);
					var node2 = path2.get(d);
					
					result = ScopeTreeData.compareNodes(node1, node2, metric, dir);
					if (result != 0)
						return result;
				}
				result = d1 - d2;
			}
			return result;
		}		
	}

	
	/***
	 * Returns a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 * @param o1
	 * @param o2
	 * @param metric
	 * @param dir
	 * @return
	 */
	protected static int compareNodes(Scope o1, Scope o2, BaseMetric metric, SortDirectionEnum dir) {
		// o1 and o2 are exactly the same object. This should return 0
		// no need to go further
		if (o1 == o2)
			return 0;
		
		int factor = dir == SortDirectionEnum.ASC ? 1 : -1;

		if (metric == null) {
			return compareNodeName(o1, o2, factor);
		}

		final MetricValue mv1 = o1.getMetricValue(metric);
		final MetricValue mv2 = o2.getMetricValue(metric);

		if (mv1 == MetricValue.NONE || mv2 == MetricValue.NONE) {
			if (mv1 == MetricValue.NONE && mv2 == MetricValue.NONE)
				return compareNodeName(o1, o2, factor);

			if (mv1 == MetricValue.NONE)
				return factor * -1;
			
			return factor * 1;
		}

		if (mv1.getValue() > mv2.getValue())
			return factor * 1;
		if (mv1.getValue() < mv2.getValue())
			return factor * -1;
		
		// ok. So far o1 looks the same as o2
		// we don't want returning 0 because it will cause the tree looks weird
		// let's try to compare with the name, and then with the hash code
		return compareNodeName(o1, o2, factor);
	}

	
	/****
	 * compare the name of the nodes and guaranteed to be unique (not zero).
	 * If the nodes have the same node, we compare with the line number.
	 * If it's still the same, we compare with the hash code. This can't be the same.
	 * @param o1
	 * @param o2
	 * @param factor
	 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
	 */
	protected static int compareNodeName(Scope o1, Scope o2, int factor) {
		int result = o1.getName().compareTo(o2.getName());
		if (result == 0) {
			// same name: compare the line number
			result = o1.getFirstLineNumber() - o2.getFirstLineNumber();
			
			// ok. So far o1 looks the same as o2
			// we don't want returning 0 because it will cause the tree looks weird
			// let's try to compare with the hash code
			if (result == 0) {
				result = o1.getCCTIndex() - o2.getCCTIndex();
			}
			// if cct-id of o1 and cct-id of o2 are identical, it must be a bug
			// or we are at the bottom-up tree where we are allowed to merge two
			// nodes into a single node
			if (result == 0) {
				// Issue #267 this is to make sure o1 and o2 are NOT identical.
				result = System.identityHashCode(o1) - System.identityHashCode(o2);
			}
		}
		return factor * result;
	}
}
