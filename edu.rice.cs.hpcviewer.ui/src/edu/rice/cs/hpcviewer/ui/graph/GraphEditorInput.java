package edu.rice.cs.hpcviewer.ui.graph;

import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.scope.Scope;


public class GraphEditorInput 
{
	static public final int MAX_TITLE_CHARS = 100; // maximum charaters for a title

	private final Scope      scope;
	private final BaseMetric metric;
	
	private final IThreadDataCollection threadData;
	
	private GraphType graphType;

	/***
	 * Create a new editor input for a give scope, metric, plot type and database
	 * @param experiment
	 * @param scope
	 * @param metric
	 * @param type
	 * @param database
	 */
	public GraphEditorInput(IThreadDataCollection threadData, Scope scope, BaseMetric metric) {
		
		this.threadData = threadData;
		
		this.scope = scope;
		this.metric = metric;
	}
	
	
	public Scope getScope() {
		return scope;
	}
	
	public BaseMetric getMetric() {
		return metric;
	}
	
	public IThreadDataCollection getThreadData() {
		return threadData;
	}
	
	public String toString() {
		
		String scopeName = scope.getName();
		if (scopeName.length() > MAX_TITLE_CHARS) {
			scopeName = scope.getName().substring(0, MAX_TITLE_CHARS) + "...";
		}
		String type  = (graphType != null? graphType.toString() : "graph");
		String title = "[" + type + "] " + scopeName +": " + metric.getDisplayName();

		return title;
	}


	public GraphType getGraphType() {
		return graphType;
	}


	public void setGraphType(GraphType graphType) {
		this.graphType = graphType;
	}
}
