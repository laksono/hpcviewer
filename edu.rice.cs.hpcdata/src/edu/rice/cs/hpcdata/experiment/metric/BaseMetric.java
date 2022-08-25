/**
 * 
 */
package edu.rice.cs.hpcdata.experiment.metric;

import edu.rice.cs.hpcdata.experiment.metric.format.IMetricValueFormat;
import edu.rice.cs.hpcdata.experiment.metric.format.MetricValueFormatFactory;
import edu.rice.cs.hpcdata.experiment.metric.format.MetricValuePredefinedFormat;
import edu.rice.cs.hpcdata.experiment.metric.format.SimpleMetricValueFormat;
import edu.rice.cs.hpcdata.experiment.scope.IMetricScope;
import edu.rice.cs.hpcdata.experiment.scope.Scope;

/********************************************************************
 * Basic class for metric description
 *
 ********************************************************************/
public abstract class BaseMetric implements Comparable<BaseMetric>{

	//-------------------------------------------------------------------------------
	// CONSTANTS
	//-------------------------------------------------------------------------------

	public static final int  PARTNER_UNKNOWN = -1;
	private static final int NO_ORDER = -1;

	//-------------------------------------------------------------------------------
	// DATA STRUCTURE
	//-------------------------------------------------------------------------------

	/** Valid types of Annotations to be used with metric values */
	public enum AnnotationType { NONE, PERCENT, PROCESS }
	
	public enum VisibilityType { 
		HIDE, 				// hide the metric column 
		SHOW, 				// show the metric column
		SHOW_INCLUSIVE,		// show only the inclusive metric 
		SHOW_EXCLUSIVE, 	// show only the exclusive metric
		INVISIBLE			// the metric shouldn't be visible by users. see issue #67
	}
	
	
	//-------------------------------------------------------------------------------
	// DATA
	//-------------------------------------------------------------------------------
 
	/** The short name of this metric, used within an experiment's XML file. 
	 *  We shouldn't change the short name as it's assigned by hpcprof to compute
	 *  derived incremental metric */
	protected String shortName;

	/** metric description (optional, just for display to users). */
	protected String description;

	/** The user-visible name of this metric. */
	protected String displayName;

	/** Whether this metric should be displayed. */
	protected VisibilityType visibility;

	/** The type of annotation that should be displayed with this metric (percent or process number). */
	protected AnnotationType annotationType = AnnotationType.NONE;

	protected int order;
	
	/** The index of this metric in its experiment's metric list. */
	protected int index;
	// partner of the metric. If the metric is exclusive, then its partner is the inclusive one
	protected int partnerIndex;

	/** The display format to be used for this metric. */
	protected IMetricValueFormat displayFormat;

	protected MetricType metricType;

	protected double  sampleperiod;

	private char unit;
	
	//-------------------------------------------------------------------------------
	// CONSTRUCTOR
	//-------------------------------------------------------------------------------


	/*************************************************************************
	 * 
	 * @param sID : Unique ID of the metric
	 * @param sDisplayName : the name of the title
	 * @param sDescription : metric description (it's okay to be null)
	 * @param displayed : will metric be displayed ?
	 * @param format : format of the display
	 * @param annotationType : show the percent or process number ?
	 * @param index : index in the table
	 * @param partnerIndex : index of the partner metric. 
	 * 		IT HAS TO BE NEGATIVE IF IT DOESNT HAVE A PARTNER !!
	 * @param type : type of the metric
	 *************************************************************************/
	protected BaseMetric(String sID, String sDisplayName, String sDescription,
			VisibilityType displayed, String format, 
			AnnotationType annotationType, int index, int partnerIndex, MetricType type) 
	{
		this.displayName = sDisplayName;
		this.visibility  = displayed;
		this.annotationType = annotationType;
		
		this.order = NO_ORDER;
		this.index = index;
		this.partnerIndex = partnerIndex;
		
		// format
		if (format == null) {
			displayFormat = getFormatBasedOnAnnotation(annotationType);
		} else {
			displayFormat = new MetricValuePredefinedFormat(format);
		}

		this.unit = '0';
		this.shortName = sID;
		this.description = sDescription;
		this.metricType = type;
	}

	
	protected BaseMetric(String sID, String sDisplayName) {
		this(sID, sDisplayName, sID, VisibilityType.SHOW, null, null, 0, 0, MetricType.UNKNOWN);
	}
	
	//-------------------------------------------------------------------------------
	// METHODS
	//-------------------------------------------------------------------------------
	

	/*************************************************************************
	 * return the comparison between this metric with other.
	 * By default we compare with the metric index, but the child class
	 * can override with other approach.
	 *************************************************************************/
	@Override
	public int compareTo(BaseMetric o) {
		return this.getIndex() - o.getIndex();
	}
	
	
	/*************************************************************************
	 *	Sets the metric's index.
	 ************************************************************************/

	public void setIndex(int index)
	{
		this.index = index;
	}

	/*************************************************************************
	 *	Returns the metric's index.
	 ************************************************************************/

	public int getIndex()
	{
		return this.index;
	}

	
	/*****
	 * get the partner metric index
	 * 
	 * @return the index partner, negative if it has no partner
	 */
	public int getPartner() {
		return partnerIndex;
	}
	

	public void setPartner(int ei)
	{
		this.partnerIndex = ei;
	}

	public void setOrder(int order)
	{
		this.order = order;
	}
	
	public int getOrder()
	{
		return order;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	public String getDescription()
	{
		return description;
	}
	
	/****
	 * check if two metric has the same index (index has to be unique
	 * regardless the name.<br/> 
	 * It's recommended to compare metrics with the index instead of the name.
	 * <br/>
	 * Warning: The same metric index can have different name since users
	 * can rename it.
	 * 
	 * @param other other metric
	 * @return true if metric has the same index. False otherwise.
	 */
	public boolean equalIndex(BaseMetric other) 
	{
		return index == other.index;
	}
	
	//=================================================================================
	//		ACCESS TO METRIC
	//=================================================================================

	/*************************************************************************
	 *	Returns the metric's short (internal) name.
	 ************************************************************************/
	public String getShortName()
	{
		return this.shortName;
	}

	/*************************************************************************
	 * 
	 * @param newName
	 *************************************************************************/
	public void setShortName(String newName)
	{
		this.shortName = newName;
	}


	/*************************************************************************
	 *	Returns the metric's user-visible name.
	 ************************************************************************/
	public String getDisplayName()
	{
		return this.displayName;
	}	

	/*************************************************************************
	 * set the new display name
	 * @param name
	 *************************************************************************/
	public void setDisplayName(String name)
	{
		displayName = name;
	}	

	/*************************************************************************
	 *	Returns whether the metric should be displayed.
	 ************************************************************************/	
	public boolean getDisplayed()
	{
		return visibility == VisibilityType.SHOW || 
				(visibility == VisibilityType.SHOW_EXCLUSIVE && metricType == MetricType.EXCLUSIVE) ||
				(visibility == VisibilityType.SHOW_INCLUSIVE && metricType == MetricType.INCLUSIVE);
	}

	
	public VisibilityType getVisibility()
	{
		return visibility;
	}
	
	public boolean isInvisible()
	{
		return visibility == VisibilityType.INVISIBLE;
	}
	
	/*************************************************************************
	 * Set display flag (true=to be displayed)
	 * @param d, the flag
	 *************************************************************************/
	public void setDisplayed(VisibilityType d)
	{
		this.visibility = d;
	}

	/*************************************************************************
	 *	Returns the type of annotation a metric's display should include (percent, process number, others ??).
	 ************************************************************************/
	public AnnotationType getAnnotationType()
	{
		return this.annotationType;
	}

	public void setAnnotationType( AnnotationType annType ) 
	{
		annotationType = annType;
		displayFormat = getFormatBasedOnAnnotation(annotationType);
	}
	
	/**
	 * Return the text to display based on the value of the scope
	 * @param scope
	 * @return
	 */
	public String getMetricTextValue(Scope scope) {
		MetricValue mv = scope.getMetricValue(this);
		boolean showPercent = getAnnotationType() == AnnotationType.PERCENT;
		MetricValue rootValue = showPercent ? scope.getRootScope().getMetricValue(this) : MetricValue.NONE;
		
		return this.getMetricTextValue(mv, rootValue);
	}

	/*************************************************************************
	 * Return the text to display based on the metric value
	 * @param mv: the value of a metric
	 *************************************************************************/
	protected String getMetricTextValue(MetricValue mv, MetricValue rootValue) {
		
		if (mv == null)
			return null;
		
		String sText;
		
		// enforce bounds for presentation
		if (mv.value > 9.99e99) mv.value = Float.POSITIVE_INFINITY;
		else if (mv.value < -9.99e99) mv.value = Float.NEGATIVE_INFINITY;
		else if (Math.abs(mv.value) < 1.00e-99)  mv.value = (float) 0.0;
		
		// if not a special case, convert the number to a string
		if (Float.compare(mv.value, 0.0f) == 0 || mv == MetricValue.NONE || !MetricValue.isAvailable(mv) ) sText = "";
		else if (Float.compare(mv.value, Float.POSITIVE_INFINITY)==0) sText = "Infinity";
		else if (Float.compare(mv.value, Float.NEGATIVE_INFINITY)==0) sText = "-Infinity";
		else if (Float.isNaN(mv.value)) sText = "NaN";
		else {
			sText = getDisplayFormat().format(mv, rootValue);
		}
		
		return sText;
	}

	/*************************************************************************
	 * Sets the metric's display format.
	 * @param format
	 *************************************************************************/
	public void setDisplayFormat( IMetricValueFormat format )
	{
		this.displayFormat = format;
	}

	/*************************************************************************
	 *	Returns the metric's display format.
	 ************************************************************************/
	public IMetricValueFormat getDisplayFormat()
	{
		return this.displayFormat;
	}
	
	/*************************************************************************
	 *	MISC
	 ************************************************************************/
	public MetricType getMetricType()
	{
		return this.metricType;
	}

	/*************************************************************************
	 * 
	 * @param objType
	 *************************************************************************/
	public void setMetricType( MetricType objType ) 
	{
		this.metricType = objType;
	}
	/*************************************************************************
	 * Laks: need an interface to update the sample period due to change in DTD
	 * @param s
	 *************************************************************************/
	public void setSamplePeriod(String s) {
		this.sampleperiod = this.convertSamplePeriode(s);
	}

	/*************************************************************************
	 * 
	 * @param sUnit
	 *************************************************************************/
	public void setUnit (String sUnit) {
		this.unit = sUnit.charAt(0);
		if (this.isUnitEvent())
			this.sampleperiod = 1.0;
	}

	/*************************************************************************
	 * Return the sample period
	 * @return
	 *************************************************************************/
	public double getSamplePeriod()
	{
		return this.sampleperiod;
	}

	public String toString() {
		return index + ". " + shortName + ": " + displayName;
	}
	
	/*************************************************************************
	 * Convert hpcrun visibility type to Java enum
	 * 
	 * @param type hpcrun visibility type. Has to consult with hpcrun file
	 * 
	 * @return VisibilityType. Default is the metric to be shown.
	 *************************************************************************/
	public static VisibilityType convertToVisibilityType(int type) 
	{
		switch(type) {
		case 0:
			return VisibilityType.HIDE;
		case 1:
			return VisibilityType.SHOW;
		case 2:
			return VisibilityType.SHOW_INCLUSIVE;
		case 3:
			return VisibilityType.SHOW_EXCLUSIVE;
		case 4:
			// see issue #67
			return VisibilityType.INVISIBLE;
		default:
			return VisibilityType.SHOW;
		}
	}
	
	
	//=================================================================================
	//		ABSTRACT METHODS
	//=================================================================================
	/*************************************************************************
	 * Method to return the value of a given scope. To be implemented by derived class.
	 * This method shouldn't be called directly other than from IMetricScope since
	 * it will grab or compute the value directly from the disk.<br>
	 * On the other hand, {@link IMetricScope.getMetricValue} will cache the value,
	 * so it's more optimized.
	 *   
	 * @param s 
	 * 			scope of the metric value
	 * @return {@code MetricValue}
	 * 			a metric value, {@code MetricValue.NONE} if the scope has no cost for this metric.
	 *************************************************************************/
	public abstract MetricValue getValue(IMetricScope s);
	

	/***
	 * Method to duplicate itself (cloning)
	 * @return
	 */
	public abstract BaseMetric duplicate();


	//=================================================================================
	//		UTILITY METHODS
	//=================================================================================

	/**
	 * Verify if the unit is an event or not
	 * @return
	 */
	private boolean isUnitEvent() {
		return this.unit == 'e';
	}
	
	private IMetricValueFormat getFormatBasedOnAnnotation(AnnotationType annotationType) {		
		IMetricValueFormat format;
		if (annotationType == AnnotationType.PERCENT) {
			format = SimpleMetricValueFormat.getInstance(); 
		} else if (annotationType == AnnotationType.PROCESS) {
			format = MetricValueFormatFactory.createFormatProcess(); 
		} else {
			format = SimpleMetricValueFormat.getInstance();  
		}
		return format;
	}

	/**
	 * convert the input sample period into a double, depending of the unit
	 * @param sPeriod
	 * @return
	 */
	protected double convertSamplePeriode( String sPeriod ) {
		if (this.isUnitEvent())
			return 1.0;

		double period = 1.0;
		if (sPeriod != null && sPeriod.length()>0) {
			try {
				period = Double.parseDouble(sPeriod);
			} catch (java.lang.NumberFormatException e) {
				// not treated
			}
		}
		return period;
	}

}
