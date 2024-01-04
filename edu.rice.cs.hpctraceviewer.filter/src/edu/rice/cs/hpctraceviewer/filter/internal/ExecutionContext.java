package edu.rice.cs.hpctraceviewer.filter.internal;

import edu.rice.cs.hpcdata.db.IdTuple;

public class ExecutionContext implements IExecutionContext
{
	private final IdTuple idTuple;
	private final int numSamples;
	
	
	public ExecutionContext(IdTuple executionContext, int numSamples) {
		this.idTuple = executionContext;
		this.numSamples = numSamples;
	}


	/**
	 * @return the executionContext
	 */
	public IdTuple getIdTuple() {
		return idTuple;
	}


	/**
	 * @return the numSamples
	 */
	public int getNumSamples() {
		return numSamples;
	}

}
