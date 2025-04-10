// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctree;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.layer.ILayerListener;
import org.eclipse.nebula.widgets.nattable.layer.event.ILayerEvent;
import org.eclipse.nebula.widgets.nattable.sort.ISortModel;
import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;
import org.eclipse.nebula.widgets.nattable.tree.AbstractTreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeRowModel;

import org.hpctoolkit.db.local.experiment.scope.Scope;


/***********************************************************
 * 
 * Specific tree model for tree Scope.
 * The class basically inherits attributes from {@link TreeRowModel}
 * but it stores the list of items that have been expanded.
 * The reason is 
 *
 ***********************************************************/
public class ScopeTreeRowModel extends AbstractTreeRowModel<Scope> implements ISortModel, ILayerListener
{
	private static final String ERR_MSG_NOTSUPPORTED = "NOT SUPPORTED";

	
	public ScopeTreeRowModel(IScopeTreeData treeData) {
		super(treeData);
	}
	
	@Override
    public boolean isCollapsed(int index) {
		IScopeTreeData tdata = (ScopeTreeData) getTreeData();
		Scope scope = tdata.getDataAtIndex(index);
		if (scope.hasChildren()) {
			int indexChild = tdata.indexOf(scope.getSubscope(0));
			return (indexChild < 0);
		}
		return false;
	}
	
	@Override
    public List<Integer> collapse(int index) {
		return new ArrayList<>(0);
	}

	@Override
    public List<Integer> collapseAll() {
		return collapse(0);
	}
	
	
	@Override
    public List<Integer> expand(int index) {		
		return new ArrayList<>(0);
	}
	
	
    @Override
    public int depth(int index) {
    	if (index == 0)
    		return 0;
    	
        return getTreeData().getDepthOfData(index);
    }


	
	@Override
    public List<Integer> expandAll() {
		throw new IllegalCallerException(ERR_MSG_NOTSUPPORTED);
	}

	@Override
	public List<Integer> getSortedColumnIndexes() {
		final List<Integer> list = new ArrayList<>(1);
		IScopeTreeData treeData = (IScopeTreeData) getTreeData();
		list.add(treeData.getSortedColumn());
		return list;
	}

	@Override
	public boolean isColumnIndexSorted(int columnIndex) {
		IScopeTreeData treeData = (IScopeTreeData) getTreeData();
		return columnIndex == treeData.getSortedColumn();
	}

	@Override
	public SortDirectionEnum getSortDirection(int columnIndex) {
		IScopeTreeData treeData = (IScopeTreeData) getTreeData();
		return treeData.getSortDirection();
	}

	@Override
	public int getSortOrder(int columnIndex) {
		return 0;
	}

	@SuppressWarnings({ "rawtypes" })
	@Override
	public List<Comparator> getComparatorsForColumnIndex(int columnIndex) {
		return Collections.emptyList();
	}

	@Override
	public Comparator<?> getColumnComparator(int columnIndex) {
		return null;
	}

	@Override
	public void sort(int columnIndex, SortDirectionEnum sortDirection, boolean accumulate) {
		IScopeTreeData treedata = (IScopeTreeData) getTreeData();
		treedata.sort(columnIndex, sortDirection, accumulate);
 	}

	@Override
	public void clear() {
		IScopeTreeData treedata = (IScopeTreeData) getTreeData();
		treedata.clear();
	}

	
	@Override
	public void handleLayerEvent(ILayerEvent event) {
		// unused
	}

	public boolean isChildrenVisible(Scope scope) {
		if (!scope.hasChildren())
			return false;

		IScopeTreeData tdata = (IScopeTreeData) getTreeData();
		int indexChild = tdata.indexOf(scope.getSubscope(0));
		return (indexChild >= 0);
	}
	
	public void setRoot(Scope root) {
		IScopeTreeData treedata = (IScopeTreeData) getTreeData();
		treedata.setRoot(root);
	}

	
	public Scope getRoot() {
		IScopeTreeData treedata = (IScopeTreeData) getTreeData();
		return treedata.getRoot();
	}

	@Override
	public List<Integer> expandToLevel(int parentIndex, int level) {
		throw new IllegalCallerException(ERR_MSG_NOTSUPPORTED);
	}

	@Override
	public List<Integer> expandToLevel(int level) {
		throw new IllegalCallerException(ERR_MSG_NOTSUPPORTED);
	}
}
