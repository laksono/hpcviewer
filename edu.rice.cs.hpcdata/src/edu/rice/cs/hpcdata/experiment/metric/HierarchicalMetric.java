package edu.rice.cs.hpcdata.experiment.metric;

import com.graphbuilder.math.Expression;
import edu.rice.cs.hpcdata.db.version4.DataSummary;
import edu.rice.cs.hpcdata.experiment.TreeNode;
import edu.rice.cs.hpcdata.experiment.scope.IMetricScope;
import edu.rice.cs.hpcdata.experiment.scope.Scope;

/****************************************************************
 * 
 * Metric class with tree-based structure
 * <p>
 * This class is designed to work with meta.db database which allows
 * to create a hierarchy of metrics.<br/>
 * 
 * It has the same structure as the 
 * {@link edu.rice.cs.hpcdata.experiment.scope.Scope} class: <br/>
 * The root metric has no parent (or the parent is null), and the
 * leaves have no children. 
 *
 ****************************************************************/
public class HierarchicalMetric extends AbstractMetricWithFormula
{	
	private static final byte FMT_METADB_COMBINE_SUM = 0;
	private static final byte FMT_METADB_COMBINE_MIN = 1;
	private static final byte FMT_METADB_COMBINE_MAX = 2;
	
	private static final byte COMBINE_UNKNOWN = -1;
	
	private static final String []COMBINE_LABEL = {"Sum", "Min", "Max", "Mean", "StdDev", "CfVar"};

	private final DataSummary profileDB;
	private final TreeNode<HierarchicalMetric> node;
	private final String originalName;

	private PropagationScope propagationScope;
	
	// the name of variant (usually statistic variants like sum, min,....)
	// it's a bit mix up with the combine
	private String variant;
	
	/**
	 * The combination function combine is an enumeration with the following possible values (the name after / is the matching name for inputs:combine in METRICS.yaml):
	 * <ul>
	 *   <li>0/sum: Sum of input values
	 *   <li>1/min: Minimum of input values
	 *   <li>2/max: Maximum of input values
	 * </ul>
	 */
	private byte combineType; 

	private final String formula;
	
	private short propMetricId;
	
	/***
	 * Create a basic metric descriptor without hierarchy information.
	 * The caller has to provide the parent/children information later on.
	 * 
	 * @param profileDB
	 * 			The object to access summary.db file.
	 * @param index
	 * 			The index of this metric
	 * @param name
	 * 			The basic name of the metric. This is not the displayed name.
	 */
	public HierarchicalMetric(DataSummary profileDB, int index, String name, String formula) {
		super(String.valueOf(index), name);
		this.profileDB = profileDB;
		setIndex(index);
		
		node = new TreeNode<>(index);
		originalName = name;

		combineType  = COMBINE_UNKNOWN;
		this.formula = formula;
	}
	
	public String getFormula() {
		return formula;
	}
	
	public void setParent(HierarchicalMetric parent) {
		node.setParent(parent);
	}
	
	public HierarchicalMetric getParent() {
		return node.getParent();
	}
	
	public void addChild(HierarchicalMetric child) {
		node.add(child);
	}
	
	public int getChildCount() {
		return node.getChildCount();
	}
	
	public HierarchicalMetric getChildAt(int index) {
		return node.getChildAt(index);
	}
	
	
	/**
	 * Set the type of combine function. 
	 * <ul>
	 *   <li>0/sum: Sum of input values
	 *   <li>1/min: Minimum of input values
	 *   <li>2/max: Maximum of input values
	 * </ul>
	 */
	public void setCombineType(byte type) {
		this.combineType = type;
	}
	
	
	/***
	 * Return the combine type of this metric.
	 * 
	 * @see getCombineTypeLabel
	 * 
	 * @return byte
	 */
	public byte getCombineType() {
		return combineType;
	}
	
	/****
	 * Set the combine type by name
	 * 
	 * @param variantLabel
	 */
	public void setVariantLabel(String variantLabel) {
		this.variant = variantLabel.trim();
	}
	
	
	public String getVariantLabel() {
		return variant;
	}
	
	/***
	 * Get the name of combine function.
	 * 
	 * @return {@code String}
	 */
	public String getCombineTypeLabel() {
		if (combineType == COMBINE_UNKNOWN)
			return "";
		
		return COMBINE_LABEL[combineType];
	}
	
	
	/***
	 * Perform reduction depending on the combine type function.
	 * <ul>
	 * 	<li>If it's a sum, then the reduce operator is minus.
	 *  <li>If it's a min, then the operator is the minimum of the two values
	 *  <li>etc.
	 * </ul>
	 * 
	 * @param target
	 * 			The destination of the result of reduction
	 * @param source
	 * 			The source of reduce operation
	 * @return {@code MetricValue}
	 * 			Equivalent with the {@code target} variable in the argument
	 */
	public MetricValue reduce(MetricValue target, MetricValue source) {
		if (combineType == COMBINE_UNKNOWN)
			return target;
		
		final float INSIGNIFICANT_NUMBER = 0.000001f;
		if (source == MetricValue.NONE)
			return target;

		if (target == MetricValue.NONE) {
			return source.duplicate();
		}

		switch (combineType) {
		case FMT_METADB_COMBINE_MAX:
			var v1 = target.getValue();
			var v2 = source.getValue();
			v1 = Math.max(v1, v2);
			target.setValue(v1);
			break;
			
		case FMT_METADB_COMBINE_MIN:
			v1 = target.getValue();
			v2 = source.getValue();
			v1 = Math.min(v1, v2);
			target.setValue(v1);
			break;
			
		case FMT_METADB_COMBINE_SUM:
			v1 = target.getValue();
			v2 = source.getValue();
			var d = v1-v2;
			if (Math.abs(d/v1) < INSIGNIFICANT_NUMBER)
				return MetricValue.NONE; 
			
			target.setValue(d);
			break;
			
		default:
			// nothing. error?
		}
		return target;
	}
	
	
	/****
	 * Retrieve the original metric name without any changes or suffixes.
	 * 
	 * @see getDisplayName
	 * 
	 * @return {@code String} 
	 * 			the original name
	 */
	public String getOriginalName() {
		return originalName;
	}
	
	
	@Override
	public String getDisplayName() {
		// the display name of hierarchical metric (meta.db) is tricky
		// because the name in meta.db doesn't include the metric type suffix
		// (something like (I) or (E)).
		// Worse, the metric type is only known after we parse the yaml file.
		//
		// So to remedy this issue, we have the "original name" and "display name"
		// the "display name" is the original name with suffix
		if (!displayName.equals(originalName))
			return displayName;
		
		// if the display name already have metric-type suffix.
		// return the real display name
		if (displayName.endsWith(PropagationScope.SUFFIX_EXCLUSIVE) || 
			displayName.endsWith(PropagationScope.SUFFIX_INCLUSIVE) ||
			displayName.endsWith(PropagationScope.SUFFIX_POINT_EXC))
			return displayName;
		
		StringBuilder sb = new StringBuilder(originalName);

		final String SUFFIX = ": ";
		if (variant == null) {
			sb.append(SUFFIX);
			sb.append(getCombineTypeLabel());
		} else if (!variant.isEmpty()) {
			sb.append(SUFFIX);
			sb.append(variant);
		}

		// otherwise we need to add suffix for the metric type
		// 
		if (propagationScope != null)
			sb.append(propagationScope.getMetricTypeSuffix());

		displayName = sb.toString();
		
		return displayName;
	}
	
	@Override
	protected Expression[] getExpressions() {
		// not supported at the moment
		// all formula-based metrics should use DerivedMetric class
		return new Expression[] {};
	}

	/**
	 * @return the propagationScope
	 */
	public PropagationScope getPropagationScope() {
		return propagationScope;
	}


	/**
	 * @param propagationScope the propagationScope to set
	 */
	public void setPropagationScope(PropagationScope propagationScope) {
		this.propagationScope = propagationScope;
		
		// redundant information
		MetricType type = propagationScope.getMetricType();		
		setMetricType(type);
	}


	/**
	 * @return the propMetricId
	 */
	public short getPropMetricId() {
		return propMetricId;
	}

	/**
	 * @param propMetricId the propMetricId to set
	 */
	public void setPropMetricId(short propMetricId) {
		this.propMetricId = propMetricId;
	}


	@Override
	public MetricValue getValue(IMetricScope s) {
		Scope scope = (Scope)s;			
		return scope.getDirectMetricValue(index);
	}

	
	@Override
	public BaseMetric duplicate() {
		var dupl = new HierarchicalMetric(profileDB, index, displayName, formula);
		copy(dupl);
		
		return dupl;
	}
	
	protected void copy(HierarchicalMetric target) {
		target.annotationType = annotationType;
		target.combineType    = combineType;
		target.description    = description;
		target.displayFormat  = displayFormat;
		target.metricType     = metricType;
		target.order          = order;
		target.partnerIndex   = partnerIndex;
		target.sampleperiod   = sampleperiod;
		target.variant        = variant;
		
		target.propagationScope = propagationScope;
	}

	
	@Override
	public boolean equals(Object other) {
		if (!(other instanceof HierarchicalMetric))
			return false;
		
		HierarchicalMetric otherMetric = (HierarchicalMetric) other;
		return this.index == otherMetric.index &&
			   this.annotationType == otherMetric.annotationType &&
			   this.combineType    == otherMetric.combineType    &&
			   this.displayName.equals(otherMetric.displayName);  
	}
	
	
	/*****
	 * Retrieve the parser for profile.db
	 * 
	 * @return
	 */
	protected DataSummary getDataSummary() {
		return profileDB;
	}
}
