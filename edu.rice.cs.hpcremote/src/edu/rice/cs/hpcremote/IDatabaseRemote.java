package edu.rice.cs.hpcremote;

import org.hpctoolkit.hpcclient.v1_0.HpcClient;

import edu.rice.cs.hpcbase.IDatabase;

public interface IDatabaseRemote extends IDatabase
{
	HpcClient getClient();
}