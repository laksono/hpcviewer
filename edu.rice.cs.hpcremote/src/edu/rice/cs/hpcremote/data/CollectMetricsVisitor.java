// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcremote.data;

import java.io.IOException;
import java.util.List;

import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.eclipse.collections.impl.map.mutable.primitive.IntObjectHashMap;
import org.hpctoolkit.db.protocol.calling_context.CallingContextId;
import org.hpctoolkit.db.protocol.profile.ProfileId;
import org.hpctoolkit.db.client.BrokerClient;
import org.hpctoolkit.db.client.UnknownCallingContextException;
import org.hpctoolkit.db.client.UnknownProfileIdException;

import org.hpctoolkit.db.local.db.IdTuple;
import org.hpctoolkit.db.local.experiment.scope.Scope;
import org.hpctoolkit.db.local.experiment.scope.visitors.ScopeVisitorAdapter;
import org.hpctoolkit.db.local.util.IProgressReport;
import io.vavr.collection.HashSet;

public abstract class CollectMetricsVisitor extends ScopeVisitorAdapter
{
	private final MutableIntObjectMap<Scope> mapToScope;	
	private final List<CallingContextId> listCCTId;
	private final IProgressReport progress;

	
	protected CollectMetricsVisitor(IProgressReport progress) {
		this.progress = progress;
		
		mapToScope = new IntObjectHashMap<>();
		listCCTId  = FastList.newList();
		
		progress.begin("Collect metrics", 3);
		progress.advance();
	}
	
	
	public void postProcess(BrokerClient client) throws UnknownProfileIdException, UnknownCallingContextException, IOException, InterruptedException {
		if (listCCTId.isEmpty())
			return;
		
		var setOfCallingContextId = HashSet.ofAll(listCCTId);
		progress.advance();
		
		// collect the metrics from remote server
		// Warning: this may take some time.
		var mapToMetrics = client.getMetrics(ProfileId.make(IdTuple.PROFILE_SUMMARY.getProfileIndex()), setOfCallingContextId);
		
		mapToMetrics.forEach((cctId, metricMeasurements) -> {
			var scope = mapToScope.remove(cctId.toInt());
			if (scope != null) {
				var setOfMetricId = metricMeasurements.getMetrics();
				setOfMetricId.toStream().forEach(mId -> {
					var mv = metricMeasurements.getMeasurement(mId);
					if (mv.isPresent()) {
						scope.setMetricValue(mId.toShort(), mv.get());
					}
				});
			}
		});
		
		dispose();
		
		progress.end();
	}
	
	
	protected void dispose() {
		mapToScope.clear();
		listCCTId.clear();
	}

	
	protected void add(Scope scope) {
		if (scope.getMetricValues().getValues() == null) {
			// this scope has no metric value or not initialized yet.
			// add it to the list of scope to be sent to the server.
			
			var index = scope.getCCTIndex();
			
			listCCTId.add(CallingContextId.make(index));
			mapToScope.put(index, scope);
		}
	}	
}
