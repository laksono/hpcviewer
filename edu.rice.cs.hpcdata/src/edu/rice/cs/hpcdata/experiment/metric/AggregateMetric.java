package edu.rice.cs.hpcdata.experiment.metric;

import com.graphbuilder.math.Expression;
import com.graphbuilder.math.ExpressionParseException;
import com.graphbuilder.math.ExpressionTree;

import edu.rice.cs.hpcdata.experiment.scope.IMetricScope;
import edu.rice.cs.hpcdata.experiment.scope.Scope;

import com.graphbuilder.math.FuncMap;


/***
 * Class for handling derived-incremental type metric
 * This metric is generated by hpcprof-mpi which contains two formula to compute the value:
 * 	combine formula: used for computing temporary value during caller and flat view creation
 *  finalize formula: used to finalizing value independent of the type of the view
 *  
 *
 */
public class AggregateMetric extends AbstractMetricWithFormula 
{
 
	static final public char FORMULA_COMBINE = 'c';
	static final public char FORMULA_FINALIZE = 'f';
	
	// formula expression
	private Expression formulaCombine, formulaFinalize;
	
	// map function
	private FuncMap fctMap;
	// map variable 
	private MetricVarMap finalizeVarMap;
	private CombineAggregateMetricVarMap combineVarMap;
	
	
	/*****
	 * Compact constructor of the class given its ID and name.
	 * The caller is responsible to set the formula and other attributes.
	 *  
	 * @param sID
	 * 			 the ID of the metric
	 * @param sDisplayName
	 * 			the display name of the metric
	 */
	public AggregateMetric(String sID, String sDisplayName) {
		super(sID, sDisplayName);
	}
	
	/**
	 * Create a new aggregate metric
	 * @see BaseMetric
	 */
	public AggregateMetric(String sID, String sDisplayName, String sDescription,
			VisibilityType displayed, String format,
			AnnotationType annotationType, int index, int partner, MetricType type) {

		super( sID, sDisplayName, sDescription, displayed, format, annotationType, index, partner, type);
		
		fctMap = new FuncMap();
		fctMap.loadDefaultFunctions();
		
		// set up the variables
		finalizeVarMap = new MetricVarMap();
		finalizeVarMap.setMetric(this);
		
		combineVarMap = new CombineAggregateMetricVarMap();
		combineVarMap.setMetric(this);
	}


	/****
	 * set the math expression
	 * @param type
	 * @param sFormula
	 *******/
	public void setFormula(char type, String sFormula) {		
		try {
			if (type == FORMULA_COMBINE) {
				formulaCombine = ExpressionTree.parse(sFormula);				
			} else if (type == FORMULA_FINALIZE) {
				formulaFinalize = ExpressionTree.parse(sFormula);
			}
		} catch (ExpressionParseException e) {
			e.printStackTrace();
		}
	}
	
	
	/******
	 * combining the metric from another view (typically cct) to this view
	 * if the target metric is not available (or empty) then we initialize it with
	 * 	the value of the source
	 * @param s_source
	 * @param s_target
	 ******/
	public void combine(Scope s_source, Scope s_target) {
		MetricValue value = s_target.getMetricValue(this); 
		if (MetricValue.isAvailable(value)) {
			//--------------------------------------------------------------------------
			// the target has the metric. we need to "combine" it with the source
			//--------------------------------------------------------------------------
			Expression expression = this.formulaCombine;
			if (expression != null) {
				this.combineVarMap.setScopes(s_source, s_target);
				this.setScopeValue(expression, this.combineVarMap, s_target);
			}
		} else {
			//--------------------------------------------------------------------------
			// the target doesn't have the metric. we need to copy from the source
			//--------------------------------------------------------------------------
			MetricValue v_source = s_source.getMetricValue(this);
			s_target.setMetricValue(index, v_source);
		}
	}
	
	
	
	/******
	 * 
	 * @param expression
	 * @param var_map
	 * @param scope
	 ******/
	private void setScopeValue(Expression expression, MetricVarMap var_map, Scope scope) {
		MetricValue mv;
		try {
			double dValue = expression.eval(var_map, this.fctMap);
			// ugly checking if the value is zero or not. There is no zero comparison in
			// Java double, so we assume we can compare it with 0.0d
			if (Double.compare(dValue, 0.0d) == 0)
				mv = MetricValue.NONE;
			else
				mv = new MetricValue(dValue);
		} catch(java.lang.Exception e) {
			mv = MetricValue.NONE;
			e.printStackTrace();
		}
		scope.setMetricValue(this.index, mv);
	}


	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.metric.BaseMetric#getValue(edu.rice.cs.hpc.data.experiment.scope.Scope)
	 */
	public MetricValue getValue(IMetricScope scope) {
		MetricValue value = MetricValue.NONE;
		if (formulaFinalize != null) {
			this.finalizeVarMap.setScope(scope);
			try {
				double dValue = formulaFinalize.eval(finalizeVarMap, fctMap);
				// ugly checking if the value is zero or not. There is no zero comparison in
				// Java double, so we assume we can compare it with 0.0d
				if (Double.compare(dValue, 0.0d) != 0) {
					value = new MetricValue(dValue);
				}
			} catch(java.lang.Exception e) {
				e.printStackTrace();
			}
		} else {
			// metric has no finalize formula
			// get whatever the combine formula has ?
			value = scope.getDirectMetricValue(index);
		}
		return value;
	}


	@Override
	/*
	 * (non-Javadoc)
	 * @see edu.rice.cs.hpc.data.experiment.metric.BaseMetric#duplicate()
	 */
	public BaseMetric duplicate() {
		AggregateMetric m = new AggregateMetric(shortName, displayName, description, visibility, 
				null, annotationType, index, partnerIndex, metricType);
		m.setOrder(order);
		if (formulaCombine != null)
			m.formulaCombine  = formulaCombine.duplicate();
		if (formulaFinalize != null)
			m.formulaFinalize = formulaFinalize.duplicate();
		
		return m;
	}


	@Override
	protected Expression[] getExpressions() {
		return new Expression[] {formulaCombine, formulaFinalize};
	}
}
