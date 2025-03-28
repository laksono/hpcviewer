// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcmetric.internal;

import java.util.List;

import org.eclipse.nebula.widgets.nattable.sort.SortDirectionEnum;

import edu.rice.cs.hpcdata.experiment.metric.BaseMetric;
import edu.rice.cs.hpcdata.experiment.metric.MetricValue;
import edu.rice.cs.hpcdata.experiment.scope.RootScope;
import edu.rice.cs.hpcfilter.FilterDataItem;
import edu.rice.cs.hpcfilter.FilterDataItemSortModel;


/*************************************
 * 
 * Customized class to sort metrics in the metric properties window
 *
 */
public class MetricFilterSortModel extends FilterDataItemSortModel<BaseMetric> 
{
	private final RootScope root;

	public MetricFilterSortModel(RootScope root, List<FilterDataItem<BaseMetric>> list) {
		super(list);
		this.root = root;
	}
	
	
	@Override
	public int compare(FilterDataItem<BaseMetric> o1, FilterDataItem<BaseMetric> o2) {
		int factor = 1;
		if (currentSortDirect == SortDirectionEnum.DESC) {
			factor = -1;
		}
		switch(currentSortColumn) {
		case 2:
			return factor * o1.compareTo(o2);
		case 3:
			BaseMetric m1 = o1.data;
			BaseMetric m2 = o2.data;
			
			MetricValue mv1 = root.getMetricValue(m1);
			MetricValue mv2 = root.getMetricValue(m2);
			
			int result = mv1.getValue() > mv2.getValue() ? 1 : (mv1.getValue() < mv2.getValue() ? -1 : 0);
			return factor * result;
		}
		return super.compare(o1, o2);
	}

}
