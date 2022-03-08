package edu.rice.cs.hpcdata.db.version4;

import java.io.File;
import java.io.IOException;

import edu.rice.cs.hpcdata.experiment.ExperimentFile;
import edu.rice.cs.hpcdata.experiment.IExperiment;
import edu.rice.cs.hpcdata.util.IUserData;

public class MetaDbFileParser extends ExperimentFile
{

	@Override
	public File parse(File location, IExperiment experiment, boolean need_metrics, IUserData<String, String> userData)
			throws Exception {
		String directory;
		if (location.isFile()) {
			directory = location.getParent();
		} else {
			throw new IOException(location.getName() + ": not readable");
		}
		
		DataMeta data = new DataMeta();
		data.open(directory);
		
		DataSummary profileDB = new DataSummary(null);
		
		return null;
	}

}
