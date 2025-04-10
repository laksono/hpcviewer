// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcmetric.internal;

import org.hpctoolkit.db.local.experiment.metric.BaseMetric;
import edu.rice.cs.hpcfilter.FilterDataItem;

public class MetricFilterDataItem extends FilterDataItem<BaseMetric>
{
	public MetricFilterDataItem(BaseMetric data, boolean checked, boolean enabled) {
		super(data, checked, enabled);
	}
	
	
	public void setLabel(String name) {
		BaseMetric metric = (BaseMetric) data;
		metric.setDisplayName(name);
	}
	
	
	public String getLabel() {
		BaseMetric metric = (BaseMetric) data;
		return metric.getDisplayName();
	}
	
	
	@Override
	public String toString() {
		BaseMetric metric = (BaseMetric) data;
		return metric.getDisplayName();
	}

	@Override
	public int compareTo(FilterDataItem<BaseMetric> o) {
		return data.getIndex() - o.data.getIndex();
	}
}
