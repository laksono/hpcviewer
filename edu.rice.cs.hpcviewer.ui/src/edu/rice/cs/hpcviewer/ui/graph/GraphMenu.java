package edu.rice.cs.hpcviewer.ui.graph;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;

import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.extdata.IThreadDataCollection;
import edu.rice.cs.hpc.data.experiment.metric.BaseMetric;
import edu.rice.cs.hpc.data.experiment.metric.MetricValue;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.Scope;
import edu.rice.cs.hpcviewer.ui.parts.editor.Editor;
import edu.rice.cs.hpcviewer.ui.parts.editor.PartFactory;


/****
 * 
 * Class to handle metric graph menus (plot, sorted and histo)
 *
 */
public class GraphMenu 
{
	static public void createAdditionalContextMenu(
			PartFactory partFactory,
			IMenuManager mgr, Experiment experiment, 
			IThreadDataCollection threadData, Scope scope) {
		
		if (scope != null) {

			if (threadData == null || !threadData.isAvailable())
				// no menus if there is no thread-level data
				return;
			
			final BaseMetric []metrics = experiment.getMetricRaw();
			if (metrics == null)
				return;
			
			mgr.add( new Separator() );
			
			for (BaseMetric metric: metrics) {
				
				// do not display empty metric 
				// this is important to keep consistency with the table
				// which doesn't display empty metrics
				
				RootScope root = scope.getRootScope();
				MetricValue mv = root.getMetricValue(metric);
				if (mv == MetricValue.NONE)
					continue;
				
	        	GraphEditorInput objInput = new GraphEditorInput(threadData, scope, metric);

				// display the menu
				
				MenuManager subMenu = new MenuManager("Graph "+ metric.getDisplayName() );
				createGraphMenus(objInput, partFactory, subMenu);
				mgr.add(subMenu);
			}
		}		
	} 

	/***
	 * Create 3 submenus for plotting graph: plot, sorted and histo
	 * @param menu
	 * @param scope
	 * @param m
	 * @param index
	 */
	static private void createGraphMenus(
			GraphEditorInput input, 
			PartFactory partFactory, 
			IMenuManager menu) {
		
		menu.add( createGraphMenu(input, partFactory, 
				  GraphPlotRegularViewer.LABEL, GraphPlotRegularViewer.ID) );
		
		menu.add( createGraphMenu(input, partFactory, 
				  GraphPlotSortViewer.LABEL, GraphPlotSortViewer.ID) );
		
		menu.add( createGraphMenu(input, partFactory, 
				  GraphHistoViewer.LABEL, GraphHistoViewer.ID) );
	}
	
	/***
	 * Create a menu action for graph
	 * @param scope
	 * @param m
	 * @param index
	 * @param t
	 * @return
	 */
	static private ScopeGraphAction createGraphMenu(
			GraphEditorInput input, 
			PartFactory partFactory, 
			String      partLabel,
			String      descriptorId) {
		
		return new ScopeGraphAction( input, partFactory, partLabel, descriptorId);
	}
	

    /********************************************************************************
     * class to initialize an action for displaying a graph
     ********************************************************************************/
    static private class ScopeGraphAction extends Action 
    {
    	final private String descriptorId;
    	final private PartFactory partFactory;
		final private GraphEditorInput input;

		public ScopeGraphAction(
				GraphEditorInput input, 
				PartFactory partFactory, 
				String label, 
				String descriptorId) {
			
			super(label);
			
			this.input  	  = input;
			this.partFactory  = partFactory;
			this.descriptorId = descriptorId;
		}
    	
		public void run() {
			
        	String elementId = GraphViewer.getID(descriptorId, input.getScope(), input.getMetric());
        	
        	partFactory.display(Editor.STACK_ID, descriptorId, elementId, input);
		}
    }
}
