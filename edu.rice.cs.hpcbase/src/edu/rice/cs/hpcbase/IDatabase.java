package edu.rice.cs.hpcbase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpcdata.experiment.Experiment;
import edu.rice.cs.hpcdata.experiment.IExperiment;
import edu.rice.cs.hpcdata.experiment.InvalExperimentException;
import edu.rice.cs.hpcdata.experiment.scope.Scope;
import edu.rice.cs.hpcdata.experiment.source.EmptySourceFile;
import edu.rice.cs.hpcdata.experiment.source.FileSystemSourceFile;
import edu.rice.cs.hpcdata.experiment.source.SourceFile;
import edu.rice.cs.hpcdata.util.Util;

public interface IDatabase 
{
	enum DatabaseStatus {NOT_INITIALIZED, OK, INVALID, INEXISTENCE, UNKNOWN_ERROR, CANCEL}
	
	/****
	 * Get the unique ID of this database. 
	 * It can be the full path of the file or the remote access + full path.
	 * 
	 * @return {@code String} 
	 * 			The unique ID
	 */
	IDatabaseIdentification getId();
	
	
	/****
	 * Check if the database is valid and can be used by the viewer
	 * 
	 * @param shell
	 * 			The parent shell to display a message to the user
	 * 
	 * @return {@code boolean} 
	 * 			true if the database is valid
	 */
	DatabaseStatus open(Shell shell);
	
	
	/****
	 * Reset or reopen the current database with the new id.
	 * 
	 * @param shell 
	 * 			The parent shell, used to display windows or dialog boxes
	 * @param id
	 * 			The new Id
	 * @return {@code DatabaseStatus}
	 * 			The status of the new reset database
	 */
	DatabaseStatus reset(Shell shell, IDatabaseIdentification id);
	
	/****
	 * Get the latest status of this database
	 * 
	 * @return {@code DatabaseStatus}
	 */
	DatabaseStatus getStatus();
	
	/****
	 * Inform the class that the database is not needed anymore and we can
	 * start clean up the allocated resources if any.
	 */
	void close();
	
	/*****
	 * Retrieve the {@code IExperiment} object when the initialization succeeds.
	 * Otherwise it returns null.
	 * 
	 * @return {@code IExperiment}
	 * 			null if the initialization fails.
	 */
	IExperiment getExperimentObject();
	
	
	/****
	 * Check if the database has traces or not
	 * 
	 * @return {@code boolean}
	 * 			{@code true} if it includes traces, {@code false} otherwise. 
	 */
	boolean hasTraceData();

	
	/****
	 * Retrieve the trace manager of this database (if exist)
	 * 
	 * @return {@code ITraceManager} 
	 * 			null if there is no trace
	 * @throws InvalExperimentException 
	 * @throws IOException 
	 */
	ITraceManager getORCreateTraceManager() throws IOException, InvalExperimentException;
	
	
	/***
	 * Retrieve the content of a source file given its file id
	 * 
	 * @param fileId 
	 * 			Object of {@code SourceFile}
	 * 
	 * @return {@code String} 
	 * 			the content of the file
	 * 
	 * @throws IOException
	 */
	String getSourceFileContent(SourceFile fileId) throws IOException;
	
	
	/***
	 * Check if the source file of a given scope exists or not
	 * 
	 * @param scope
	 * @return
	 */
	boolean isSourceFileAvailable(Scope scope);
	
	
	/***
	 * Create an empty database wrapper for a given Experiment object
	 * @param experiment
	 * @return
	 */
	static IDatabase getEmpty(final Experiment experiment) {
		return new IDatabase() {

			@Override
			public IDatabaseIdentification getId() {
				return experiment::getDirectory;
			}

			@Override
			public DatabaseStatus open(Shell shell) {
				return DatabaseStatus.OK;
			}

			@Override
			public DatabaseStatus getStatus() {
				return DatabaseStatus.OK;
			}

			@Override
			public void close() {
				// nothing			
			}

			@Override
			public IExperiment getExperimentObject() {
				return experiment;
			}

			@Override
			public boolean hasTraceData() {
				return false;
			}

			@Override
			public ITraceManager getORCreateTraceManager() {
				return null;
			}

			@Override
			public String getSourceFileContent(SourceFile fileId) throws IOException {
				if (fileId instanceof EmptySourceFile || !fileId.isAvailable())
					return null;
				
				FileSystemSourceFile source = (FileSystemSourceFile) fileId;
				var filename = source.getCompleteFilename();
				Path path = Path.of(filename);

				return Files.readString(path, StandardCharsets.ISO_8859_1);
			}

			@Override
			public boolean isSourceFileAvailable(Scope scope) {
				return Util.isFileReadable(scope);
			}

			@Override
			public DatabaseStatus reset(Shell shell, IDatabaseIdentification id) {
				return DatabaseStatus.OK;
			}		

		};
	}
}
