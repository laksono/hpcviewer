// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcremote.trace;

import java.io.IOException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.hpctoolkit.db.client.BrokerClient;

import org.hpctoolkit.db.local.experiment.IExperiment;
import org.hpctoolkit.db.local.experiment.InvalExperimentException;
import org.hpctoolkit.db.local.experiment.metric.IMetricManager;
import edu.rice.cs.hpcremote.IDatabaseRemote;
import edu.rice.cs.hpctraceviewer.data.AbstractDBOpener;
import edu.rice.cs.hpctraceviewer.data.SpaceTimeDataController;


public class RemoteTraceOpener extends AbstractDBOpener
{
	private final BrokerClient client;
	private final IExperiment experiment;
	
	
	public RemoteTraceOpener(IDatabaseRemote database) {
		this(database.getClient(), database.getExperimentObject());
	}
	
	public RemoteTraceOpener(BrokerClient client, IMetricManager experiment) {
		this.client = client;
		this.experiment = (IExperiment) experiment;
	}

	@Override
	public SpaceTimeDataController openDBAndCreateSTDC(IProgressMonitor statusMgr)
			throws IOException, InvalExperimentException {
		
		return new RemoteSpaceTimeDataController(client, experiment);
	}

	@Override
	public void end() {
		// close experience object?
	}

}
