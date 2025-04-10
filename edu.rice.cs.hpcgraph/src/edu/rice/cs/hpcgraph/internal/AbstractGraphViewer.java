// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcgraph.internal;

import javax.inject.Inject;

import java.util.ArrayList;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swtchart.Chart;
import org.eclipse.swtchart.IAxis;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IAxisTick;

import edu.rice.cs.hpcbase.BaseConstants;
import edu.rice.cs.hpcbase.IBaseInput;
import edu.rice.cs.hpcbase.ui.AbstractUpperPart;
import edu.rice.cs.hpcbase.ui.ILowerPart;
import edu.rice.cs.hpcbase.ui.IUpperPart;
import org.hpctoolkit.db.local.experiment.metric.BaseMetric;
import org.hpctoolkit.db.local.experiment.scope.Scope;
import edu.rice.cs.hpcgraph.GraphEditorInput;

import javax.annotation.PreDestroy;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;

public abstract class AbstractGraphViewer extends AbstractUpperPart implements IUpperPart
{
	public static final int PLOT_OK          = 0;
	public static final int PLOT_ERR_IO 	 = -1;
	public static final int PLOT_ERR_UNKNOWN = -2;
	
	public static final int MAX_TITLE_CHARS = 100; // maximum characters for a title
	
    private Chart chart;
    private GraphEditorInput input;
    private Composite container;

	@Inject
	protected AbstractGraphViewer(CTabFolder tabFolder, int style) {
		super(tabFolder, style);
		setShowClose(true);
		
		container = new Composite(tabFolder, SWT.NONE);
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(container);
		
		setControl(container);
	}
	
	
	@PreDestroy
	public void preDestroy() {
	}
	
	
	@Focus
	public void onFocus() {
		setFocus();
	}


	@Override
	public void setFocus() {
		chart.setFocus();
	}

	
	@Override
	public boolean hasEqualInput(IBaseInput input) {
		if (input == null) return false;
		if (!(input instanceof GraphEditorInput)) return false;
		
		GraphEditorInput inputNew = (GraphEditorInput) input;
		
		return inputNew.getScope()     == this.input.getScope()     && 
			   inputNew.getGraphType() == this.input.getGraphType() &&
			   inputNew.getMetric().compareTo(this.input.getMetric()) == 0;
	}

	@Override
	public void setMarker(int lineNumber) {
	}

	@Override
	public void setInput(IBaseInput obj) {

		if (obj == null) return;

		// we shouldn't create another plot if we already have plotted
		if (input != null) return;
		
		// Important: First thing: we need to set the value of input here 
		// subclasses may need the input value for setting the title
		
		input = (GraphEditorInput) obj;
				
		//----------------------------------------------
		// chart creation
		//----------------------------------------------
		chart = new GraphChart(input.getProfilePart(), container, SWT.NONE);
		((GraphChart) chart).setInput(input, getGraphTranslator());
		
		GridDataFactory.fillDefaults().grab(true, true).applyTo(chart);
		
		//----------------------------------------------
		// formatting axis
		//----------------------------------------------
		IAxisSet axisSet = chart.getAxisSet();
		IAxisTick yTick = axisSet.getYAxis(0).getTick();
		yTick.setFormat(GraphChart.METRIC_FORMAT);
		
		//----------------------------------------------
		// tidy-up the chart
		//----------------------------------------------

		chart.getLegend().setVisible(false);
		
		final String title = getTitle();
		chart.getTitle().setText(title);
		
		setToolTipText(title);
		setText(title);
		
		Color bg = chart.getBackground();

		Display display = Display.getDefault();

		// Pick the color of the text indicating sample depth. 
		// If the background is sufficiently light, pick dark blue, otherwise white
		Color fg = display.getSystemColor(SWT.COLOR_BLUE);
		
		if (bg.getRed()+bg.getBlue()+bg.getGreen() <= BaseConstants.DARKEST_COLOR_FOR_BLACK_TEXT)
			fg = display.getSystemColor(SWT.COLOR_WHITE);

		chart.getTitle().setForeground(fg);
		IAxis[] axis = chart.getAxisSet().getAxes();
		for(IAxis x: axis) {
			x.getTitle().setForeground(fg);
			x.getTick().setForeground(fg);
		}
		
		//----------------------------------------------
		// main part: ask the subclass to plot the graph
		//----------------------------------------------

		plotData(input);
		
		// -----------------------------------------------------------------
		// Due to SWT Chart bug, we need to adjust the range once the create-part-control
		// 	finishes its layout.
		// -----------------------------------------------------------------

		// have to adjust the range separately on E4.
		
		display.asyncExec(() -> chart.getAxisSet().adjustRange());
	}
	
	protected GraphEditorInput getInput() {
		return input;
	}
	
	
	@Override
	public String getTitle() {
		
		Scope scope = input.getScope();
		BaseMetric metric = input.getMetric();
		
		String scopeName = scope.getName();
		if (scopeName.length() > MAX_TITLE_CHARS) {
			scopeName = scope.getName().substring(0, MAX_TITLE_CHARS) + "...";
		}
		String type  = getGraphTypeLabel();
		return "[" + type + "] " + scopeName +": " + metric.getDisplayName();
	}
	

	@Override
	public void refresh(ILowerPart lowerPart) {
		// no need to refresh the content
	}
	

	public static String getID(String descID, Scope scope, BaseMetric metric) {
		final char ELEMENT_SEPARATOR = ':';
		
		String dbId  = scope.getExperiment().getDirectory();
		int scopeId  = scope.getCCTIndex();
		int metricId = metric.getIndex();
		int graphId  = descID.hashCode();
		
		return dbId 					+ ELEMENT_SEPARATOR + 
			   String.valueOf(scopeId)  + ELEMENT_SEPARATOR +
			   String.valueOf(metricId) + ELEMENT_SEPARATOR +
			   String.valueOf(graphId);
	}
	
	protected Chart getChart() {
		return chart;
	}
	
	/**
	 * method to plot a graph of a specific scope and metric of an experiment
	 * 
	 * @param scope: the scope to plot
	 * @param metric: the raw metric to plot
	 * 
	 * @return PLOT_OK if everything works fine. Negative integer otherwise 
	 */
	protected abstract int plotData(GraphEditorInput input);
	
	/****
	 * Translate a set of thread-index selections into the original set of
	 * thread-index selection.<br/>
	 * It is possible that the child class change the index of x-axis. This
	 * method will then translate from the current selected index to the original
	 * index so that it can be displayed properly by {@link ThreadView}. 
	 *  
	 * @param selections : a set of selected index (usually only one item)
	 * @return the translated set of indexes
	 */
	protected abstract ArrayList<Integer> translateUserSelection(ArrayList<Integer> selections); 

	protected abstract IGraphTranslator getGraphTranslator();

	protected abstract String getGraphTypeLabel();
}
