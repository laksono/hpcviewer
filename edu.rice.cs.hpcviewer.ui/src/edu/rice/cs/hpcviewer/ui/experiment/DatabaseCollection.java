package edu.rice.cs.hpcviewer.ui.experiment;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.e4.core.di.annotations.Creatable;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.core.services.statusreporter.StatusReporter;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MPartStack;
import org.eclipse.e4.ui.model.application.ui.basic.MStackElement;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.UIEvents;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpcviewer.ui.internal.ViewerDataEvent;
import edu.rice.cs.hpcviewer.ui.parts.IViewPart;
import edu.rice.cs.hpcviewer.ui.util.Constants;
import edu.rice.cs.hpcviewer.ui.util.ElementIdManager;

/***
 * <b>
 * Class Database manager
 * </b>
 * <p>It manages multiple experiment databases, including:
 * <ul>
 *  <li>Adding a new database. It sends message DatabaseManager.EVENT_HPC_NEW_DATABASE to
 *      the application components.</li>
 *  <li>Removing an existing database</li>
 * </ul>
 *</p>
 */
@Creatable
@Singleton
public class DatabaseCollection 
{
	static private final String STACK_ID_BASE 	  = "edu.rice.cs.hpcviewer.ui.partstack.lower.";
	
	static private final int MAX_STACKS_AVAIL = 3;
	
	final private HashMap<MWindow, List<BaseExperiment>>   mapWindowToExperiments;
	final private HashMap<BaseExperiment, ViewerDataEvent> mapColumnStatus;
	final private HashMap<RootScopeType, String> 		   mapRoottypeToPartId;
	
	private EPartService      partService;
	private IEventBroker      eventBroker;
    private ExperimentManager experimentManager;
    private MApplication      application;
	
	private StatusReporter    statusReporter;
	
	public DatabaseCollection() {

		mapColumnStatus = new HashMap<BaseExperiment, ViewerDataEvent>();

		mapRoottypeToPartId = new HashMap<RootScopeType, String>();

		mapRoottypeToPartId.put(RootScopeType.CallingContextTree, Constants.ID_VIEW_TOPDOWN);
		mapRoottypeToPartId.put(RootScopeType.CallerTree, 	 	  Constants.ID_VIEW_BOTTOMUP);
		mapRoottypeToPartId.put(RootScopeType.Flat, 		   	  Constants.ID_VIEW_FLAT);
		mapRoottypeToPartId.put(RootScopeType.DatacentricTree, 	  Constants.ID_VIEW_DATA);
		
		experimentManager = new ExperimentManager();
		
		mapWindowToExperiments = new HashMap<MWindow, List<BaseExperiment>>(1);
	}
	
	@Inject
	@Optional
	private void subscribeApplicationCompleted(
			@UIEventTopic(UIEvents.UILifeCycle.APP_STARTUP_COMPLETE) 
			final MApplication   application,
			final EPartService   partService,
			final IEventBroker   broker,
			final EModelService  modelService,
			final StatusReporter statusReporter) {
		
		this.partService    = partService;
		this.eventBroker    = broker;
		this.statusReporter = statusReporter;
		this.application    = application;
		
		// handling the command line arguments:
		// if one of the arguments specify a file or a directory,
		// try to find the experiment.xml and load it.
		
		String args[] = Platform.getApplicationArgs();
		
		Display display = Display.getCurrent();
		Shell myShell   = display.getActiveShell();
		
		if (myShell == null) {
			myShell = new Shell(SWT.TOOL | SWT.NO_TRIM);
		}
		BaseExperiment experiment    = null;
		
		String path = null;
		
		for (String arg: args) {
			if (arg.charAt(0) != '-')
				path = arg;
		}
		// if the user doesn't specify the database path, we need to display
		// a window to get the filename
		
		if (path == null || path.length() < 1) {
			path = experimentManager.openFileExperiment(myShell);
			if (path == null)
				return; 
		}
		experiment = openDatabase(myShell, path);
		if (experiment == null)
			return;
		
		createViewsAndAddDatabase(experiment, application, partService, modelService, null);
	}
	

	/****
	 * One-stop API to open and add a database. 
	 * This method shows a dialog box to pick a directory, check if the database already exists or not,
	 * create views and add to the list of the database collection.
	 * 
	 * @param shell the current shell
	 * @param application MApplication
	 * @param service EPartService
	 * @param modelService EModelService
	 * @param parentId the id of the part stack to contain the views. If the value is null, then
	 *   it searches any available slot if possible.
	 */
	public void addDatabase(Shell shell, 
			MApplication 	application, 
			EPartService    service,
			EModelService 	modelService,
			String          parentId) {
		
		String filename = experimentManager.openFileExperiment(shell);
		if (filename == null)
			return;

		if (isExist(shell, filename))
			return;
		
		BaseExperiment experiment = experimentManager.loadExperiment(shell, filename);
		
		createViewsAndAddDatabase(experiment, application, service, modelService, parentId);
	}
	
	
	/****
	 * One-stop API to open a database. 
	 * This method shows a dialog box to pick a directory, check if the database already exists or not,
	 * create views and remove the existing databases before adding it to the list of the database collection.
	 * The removal is important to make sure there is only one database exist.
	 * 
	 * @param shell the current shell
	 * @param application MApplication
	 * @param service EPartService
	 * @param modelService EModelService
	 * @param parentId the id of the part stack to contain the views. If the value is null, then
	 *   it searches any available slot if possible.
	 */
	public void openDatabase(Shell shell, 
			MApplication 	application, 
			EPartService    service,
			EModelService 	modelService,
			String          parentId) {
		
		String filename = experimentManager.openFileExperiment(shell);
		if (filename == null)
			return;

		if (isExist(shell, filename))
			return;
		
		BaseExperiment experiment = experimentManager.loadExperiment(shell, filename);
		
		removeAll();
		
		createViewsAndAddDatabase(experiment, application, service, modelService, parentId);
	}

	
	/****
	 * Add a new database into the collection.
	 * This database can be remove later on by calling {@code removeLast}
	 * or {@code removeAll}.
	 * 
	 * @param experiment
	 * @param application
	 * @param service
	 * @param broker
	 * @param modelService
	 */
	public void createViewsAndAddDatabase(BaseExperiment experiment, 
			MApplication 	application, 
			EPartService    service,
			EModelService 	modelService,
			String          parentId) {
		
		if (experiment == null) {
			System.err.println("null experiment");
			return;
		}
		
		if (service == null) {
			System.out.println("Error: service is not available");
			return;
		}
		IEclipseContext activeWindowContext = application.getContext().getActiveChild();
		if (activeWindowContext == null) {
			Display display = Display.getCurrent();
			MessageDialog.openError(display.getActiveShell(), "Error", 
					"Cannot find an active window with this platform.\n" +
					"Please open a database from the File-Open menu.");
			return;
		}

		//----------------------------------------------------------------
		// find an empty slot in the part stack
		// If no slot is available, we will try to create a new one.
		// However,creating a new part stack is tricky, and it's up to the
		// system where to locate the part stack.
		//----------------------------------------------------------------
		
		MPartStack stack = null;
		List<MStackElement> list = null;
		
		if (parentId == null) {
			for(int i=1; i<=MAX_STACKS_AVAIL; i++) {
				final String stackId = STACK_ID_BASE + String.valueOf(i) ;
				stack  = (MPartStack)modelService.find(stackId , application);
				
				if (stack != null)
					list = stack.getChildren();

				if (list != null && list.size()==0)
					// we found empty an stack
					break; 
			}			
		} else {
			stack  = (MPartStack)modelService.find(parentId , application);
			if (stack != null)
				list = stack.getChildren();
		}
		
		//----------------------------------------------------------------
		// create a new part stack if necessary
		// We don't want this, since it makes the layout weird.
		//----------------------------------------------------------------
		if (stack == null) {
			System.out.println("create a new part stack");
			
			stack = modelService.createModelElement(MPartStack.class);
			stack.setElementId(STACK_ID_BASE  + "1");
			stack.setToBeRendered(true);
		}
		
		//----------------------------------------------------------------
		// part stack is ready, now we create all view parts and add it to the part stack
		// TODO: We assume adding to the part stack is always successful
		//----------------------------------------------------------------
		stack.setVisible(true);
		stack.setOnTop(true);
		
		Object []children = experiment.getRootScopeChildren();
		
		for (int i=0; i<children.length; i++) {

			RootScope root = (RootScope) children[i];
			
			String partId = mapRoottypeToPartId.get(root.getType());
			if (partId == null)
				continue; 	// TODO: should display error message
			
			final MPart part = service.createPart(partId);
			
			list.add(part);

			part.setLabel(root.getRootName());

			//----------------------------------------------------------------
			// We only make the top-down (the first part) to be visible
			// the other parts will be created, but not activated.
			// Let users to activate the other parts by themselves.
			//----------------------------------------------------------------
			if (i==0) {
				
				service.showPart(part, PartState.VISIBLE);
			} else {
				
				service.showPart(part, PartState.CREATE);
			}			
			IViewPart view = null;
			int maxAttempt = 10;
			
			while(maxAttempt>0) {
				view = (IViewPart) part.getObject();
				if (view != null)
					break;
				
				try {
					Thread.sleep(300);					
				} catch (Exception e) {
					
				}
				maxAttempt--;
			}
			// has to set the element Id before populating the view
			String elementID = ElementIdManager.getElementId(root);
			part.setElementId(elementID);

			view.setInput(part, experiment);
		}
		
		statusReport(IStatus.INFO, "Open " + experiment.getDefaultDirectory().getAbsolutePath(), null);
		
		List<BaseExperiment> listExperiments = getActiveListExperiments();
		if (listExperiments != null) {
			listExperiments.add(experiment);
		}
	}
	
	
	/***
	 * Retrieve the iterator of the database collection
	 * 
	 * @return Iterator for the list
	 */
	public Iterator<BaseExperiment> getIterator() {
		List<BaseExperiment> list = getActiveListExperiments();
		return list.iterator();
	}
	
	
	/***
	 * Retrieve the current registered databases
	 * @return
	 */
	public int getNumDatabase() {
		List<BaseExperiment> list = getActiveListExperiments();
		if (list == null)
			return 0;
		return list.size();
	}
	
	
	/***
	 * Check if the database is empty or not
	 * @return true if the database is empty
	 */
	public boolean isEmpty() {
		List<BaseExperiment> list = getActiveListExperiments();
		return list.isEmpty();
	}
	
	
	/***
	 * Remove the last registered database
	 * @return
	 */
	public BaseExperiment getLast() {
		List<BaseExperiment> list = getActiveListExperiments();
		return list.get(list.size()-1);
	}
	
	
	/***
	 * Check if an experiment already exist in the collection
	 * @param experiment
	 * @return true if the experiment already exists. False otherwise.
	 */
	public boolean IsExist(BaseExperiment experiment) {
		List<BaseExperiment> list = getActiveListExperiments();
		return list.contains(experiment);
	}
	
	
	/***
	 * Check if a database path already exist in the collection
	 * @param shell
	 * @param pathXML the absolute path to XML file
	 * @return true of the XML file already exist. False otherwise
	 */
	public boolean isExist(Shell shell, String pathXML) {
		List<BaseExperiment> list = getActiveListExperiments();
		
		if (list.isEmpty())
			return false;
		
		for (BaseExperiment exp: list) {
			String file = exp.getXMLExperimentFile().getAbsolutePath();
			if (file.equals(pathXML)) {
				// we cannot have two exactly the same database in one window
				MessageDialog.openError(shell, "Error", file +": the database is already opened." );

				return true;
			}
		}
		return false;
	}
	
	
	/****
	 * Remove all databases
	 * @return
	 */
	public int removeAll() {
		List<BaseExperiment> list = getActiveListExperiments();
		
		int size = list.size();
		
		// TODO: ugly solution to avoid concurrency (java will not allow to remove a list while iterating).
		// we need to copy the list to an array, and then remove the list
		
		BaseExperiment arrayExp[] = new BaseExperiment[size];
		list.toArray(arrayExp);
		
		for(BaseExperiment exp: arrayExp) {
			// inside this method, we remove the database AND hide the view parts
			removeDatabase(exp);
		}
		
		list.clear();

		mapColumnStatus.clear();
		
		return size;
	}
	
	
	public void addColumnStatus(BaseExperiment experiment, ViewerDataEvent data) {
		mapColumnStatus.put(experiment, data);
	}
	
	
	public ViewerDataEvent getColumnStatus(BaseExperiment experiment) {
		return mapColumnStatus.get(experiment);
	}
	
	
	public void removeDatabase(final BaseExperiment experiment) {
		
		if (experiment == null)
			return;
		
		// remove any database associated with this experiment
		// some parts may need to check the database if the experiment really exits or not.
		// If not, they will consider the experiment will be removed.
		List<BaseExperiment> list = getActiveListExperiments();
		list.remove(experiment);

		mapColumnStatus.remove(experiment);
		
		statusReport(IStatus.INFO, "Remove " + experiment.getDefaultDirectory().getAbsolutePath(), null);
		
		final Collection<MPart> listParts = partService.getParts();
		if (listParts == null)
			return;

		// first, notify all the parts that have experiment that they will be destroyed.
		
		ViewerDataEvent data = new ViewerDataEvent((Experiment) experiment, null);
		eventBroker.post(ViewerDataEvent.TOPIC_HPC_REMOVE_DATABASE, data);

		String elementID = ElementIdManager.getElementId(experiment);
		
		// destroy all the views and editors that belong to experiment
		// since Eclipse doesn't have "destroy" method, we hide them.
		
		for(MPart part: listParts) {
			
			if (part.getElementId().startsWith(elementID)) {
				partService.hidePart(part, true);
			}
		}
	}	
	
	
	/***
	 * Log status to the Eclipse log service
	 * @param status type of status {@link IStatus}
	 * @param message 
	 * @param e any exception
	 */
	public void statusReport(int status, String message, Exception e) {

		IStatus objStatus = statusReporter.newStatus(status, message, e);
		statusReporter.report(objStatus, StatusReporter.LOG);
	}
	
	
	/***
	 * Retrieve the list of experiments of the current window.
	 * If Eclipse reports there is no active window, the list is null.
	 * 
	 * @return the list of experiments (if there's an active window). null otherwise.
	 * 
	 */
	private List<BaseExperiment> getActiveListExperiments() {
		MWindow window = application.getSelectedElement();
		if (window == null) {
			statusReport(IStatus.ERROR, "no active window", null);
			return null;
		}
		List<BaseExperiment> list = mapWindowToExperiments.get(window);
		
		if (list == null) {
			list = new ArrayList<BaseExperiment>();
			mapWindowToExperiments.put(window, list);
		}
		return list;
	}
	
	/****
	 * Find a database for a given path
	 * 
	 * @param shell the active shell
	 * @param expManager the experiment manager
	 * @param sPath path to the database
	 * @return
	 */
	private BaseExperiment openDatabase(Shell shell, String sPath) {
    	IFileStore fileStore;

		try {
			fileStore = EFS.getLocalFileSystem().getStore(new URI(sPath));
			
		} catch (URISyntaxException e) {
			// somehow, URI may throw an exception for certain schemes. 
			// in this case, let's do it traditional way
			fileStore = EFS.getLocalFileSystem().getStore(new Path(sPath));

			statusReport(IStatus.ERROR, "Locating " + sPath, e);
		}
    	IFileInfo objFileInfo = fileStore.fetchInfo();

    	if (!objFileInfo.exists())
    		return null;

    	BaseExperiment experiment = null;
    	
    	if (objFileInfo.isDirectory()) {
    		experiment = experimentManager.openDatabaseFromDirectory(shell, sPath);
    	} else {
			EFS.getLocalFileSystem().fromLocalFile(new File(sPath));
			experiment = experimentManager.loadExperiment(shell, sPath);
    	}
    	return experiment;
	}
}
