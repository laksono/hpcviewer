// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcgraph.plot;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swtchart.IAxisSet;
import org.eclipse.swtchart.IAxisTick;
import org.eclipse.swtchart.ILineSeries;

import org.hpctoolkit.db.local.experiment.extdata.IThreadDataCollection;
import org.hpctoolkit.db.local.experiment.metric.BaseMetric;
import org.hpctoolkit.db.local.experiment.metric.MetricRaw;
import org.hpctoolkit.db.local.experiment.scope.Scope;
import edu.rice.cs.hpcgraph.GraphEditorInput;
import edu.rice.cs.hpcgraph.internal.IGraphTranslator;


public class GraphPlotSortViewer extends AbstractGraphPlotViewer 
{
	public GraphPlotSortViewer(CTabFolder tabFolder, int style) {
		super(tabFolder, style);
	}

	public static final String LABEL = "Sorted plot graph";

    private PairThreadIndex []pairThreadIndex;

	@Override
	protected String getXAxisTitle() {
		return "Execution context sorted by metric value";
	}

	@Override
	protected double[] getValuesX(Scope scope, BaseMetric metric) throws NumberFormatException, IOException {
		IThreadDataCollection threadData = getInput().getThreadData();
		double[] x_values = threadData.getRankLabels();
		double[] sequence_x = new double[x_values.length];
		for (int i=0; i<x_values.length; i++) {
			sequence_x[i] = i;
		}
		return sequence_x;
	}

	@Override
	protected double[] getValuesY(Scope scope, BaseMetric metric) throws Exception {

		int id = metric.getIndex();
		if (metric instanceof MetricRaw) {
			id = ((MetricRaw) metric).getRawID();
		}
		
		IThreadDataCollection threadData = getInput().getThreadData();
		double[] y_values = null;
		y_values = threadData.getMetrics(scope.getCCTIndex(), id);
		pairThreadIndex = new PairThreadIndex[y_values.length];

		for(int i=0; i<y_values.length; i++)
		{
			pairThreadIndex[i] = new PairThreadIndex();
			pairThreadIndex[i].index = i;
			pairThreadIndex[i].value = y_values[i];
		}
		java.util.Arrays.sort(y_values);
		java.util.Arrays.sort(pairThreadIndex);
		
		return y_values;
	}


	
	/*************
	 * 
	 * Pair of thread and the sequential index for the sorting
	 *
	 *************/
	private static class PairThreadIndex implements Comparable<PairThreadIndex>
	{
		int index;
		double value;

		@Override
		public int compareTo(PairThreadIndex o) {
			return Double.compare(value, o.value);
		}
		
		@Override
		public String toString() {
			return "(" + index + "," + value + ")";
		}
	}


	@Override
	protected ArrayList<Integer> translateUserSelection(ArrayList<Integer> selections) {
		
		if (pairThreadIndex != null) {
			ArrayList<Integer> list = new ArrayList<Integer>( selections.size());
			for(Integer i : selections) {
				list.add(pairThreadIndex[i].index);
			}
			return list;
		}
		return new ArrayList<>();
	}

	@Override
	protected String getGraphTypeLabel() {
		return LABEL;
	}

	@Override
	protected int setupXAxis(GraphEditorInput input, ILineSeries<?> scatterSeries) {
		IAxisSet axisSet = getChart().getAxisSet();

		final IAxisTick xTick = axisSet.getXAxis(0).getTick();
		xTick.setFormat(new DecimalFormat("#############"));

		Scope scope = input.getScope();
		BaseMetric metric = input.getMetric();

		try {
			double[] x_values = getValuesX(scope, metric);
			scatterSeries.setXSeries(x_values);
			
		} catch (NumberFormatException | IOException e) {

			showErrorMessage(e);
			return PLOT_ERR_IO;
		}

		return PLOT_OK;
	}

	@Override
	protected IGraphTranslator getGraphTranslator() {
		return index -> pairThreadIndex[index].index;
	}
}
