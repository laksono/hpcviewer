 
package edu.rice.cs.hpcviewer.ui.parts;

import javax.inject.Inject;
import javax.inject.Named;
import javax.annotation.PostConstruct;
import org.eclipse.swt.widgets.Composite;
import javax.annotation.PreDestroy;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.e4.ui.di.Focus;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.e4.ui.services.IServiceConstants;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpcviewer.ui.experiment.DatabaseCollection;
import edu.rice.cs.hpcviewer.ui.graph.AbstractGraphViewer;
import edu.rice.cs.hpcviewer.ui.graph.GraphEditorInput;
import edu.rice.cs.hpcviewer.ui.graph.GraphHistoViewer;
import edu.rice.cs.hpcviewer.ui.graph.GraphPlotRegularViewer;
import edu.rice.cs.hpcviewer.ui.graph.GraphPlotSortViewer;
import edu.rice.cs.hpcviewer.ui.parts.bottomup.BottomUpView;
import edu.rice.cs.hpcviewer.ui.parts.editor.Editor;
import edu.rice.cs.hpcviewer.ui.parts.editor.IUpperPart;
import edu.rice.cs.hpcviewer.ui.parts.editor.PartFactory;
import edu.rice.cs.hpcviewer.ui.parts.flat.FlatView;
import edu.rice.cs.hpcviewer.ui.parts.topdown.TopDownView;
import edu.rice.cs.hpcviewer.ui.util.ElementIdManager;



public class ProfilePart implements IViewPart
{
	public static final String ID = "edu.rice.cs.hpcviewer.ui.partdescriptor.profile";
	
	@Inject	protected EPartService  partService;
	@Inject protected EModelService modelService;
	@Inject protected MApplication  app;
	@Inject protected IEventBroker  eventBroker;
	protected EMenuService menuService;
	
	@Inject protected DatabaseCollection databaseAddOn;

	@Inject protected PartFactory partFactory;

	/** Each view needs to store the experiment database.
	 * In case it needs to populate the table, we know which database 
	 * to be loaded. */
	private BaseExperiment  experiment;
	
	private AbstractViewItem []views;

	private CTabFolder tabFolderTop, tabFolderBottom;
	private Shell shell;
	
	@Inject
	public ProfilePart() {
	}
	
	@PostConstruct
	public void postConstruct(Composite parent, EMenuService menuService, @Named(IServiceConstants.ACTIVE_SHELL) Shell shell) {
		
		this.menuService = menuService;
		this.shell = shell;
		
		parent.setLayout(new FillLayout(SWT.HORIZONTAL));

		SashForm sashForm = new SashForm(parent, SWT.VERTICAL);
		
		tabFolderTop = new CTabFolder(sashForm, SWT.BORDER);
		tabFolderTop.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		tabFolderBottom = new CTabFolder(sashForm, SWT.BORDER);
		tabFolderBottom.setSelectionBackground(Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND_GRADIENT));
		
		sashForm.setWeights(new int[] {1, 1});

		tabFolderBottom.setFocus();
	}
	
	
	/***
	 * Display an editor in the top folder
	 * 
	 * @param input cannot be null
	 */
	public void addEditor(Object input) {
		// search if the input is already displayed 
		// if this is the case, we reuse the existing item
		
		CTabItem []items =  tabFolderTop.getItems();
		for (int i=0; i<items.length; i++) {
			CTabItem item = items[i];
			if (item instanceof IUpperPart) {
				IUpperPart editor = (IUpperPart) item;
				if (editor.hasEqualInput(input)) {
					editor.setInput(null, input);
					
					tabFolderTop.setSelection((CTabItem) editor);
					
					return;
				}
			}
		}
		// the input is not displayed
		// create a new item for this input
		CTabItem viewer = null;
		if (input instanceof GraphEditorInput) {
			GraphEditorInput graphInput = (GraphEditorInput) input;
			if (graphInput.getGraphType() == GraphPlotRegularViewer.LABEL) {
				viewer = new GraphPlotRegularViewer(tabFolderTop, SWT.NONE);
				
			} else if (graphInput.getGraphType() == GraphPlotSortViewer.LABEL) {
				viewer = new GraphPlotSortViewer(tabFolderTop, SWT.NONE);
			
			} else if (graphInput.getGraphType() == GraphHistoViewer.LABEL) {
				viewer = new GraphHistoViewer(tabFolderTop, SWT.NONE);
			}
			
			Composite parent = new Composite(tabFolderTop, SWT.NONE);
			((AbstractGraphViewer)viewer).postConstruct(parent);
			viewer.setControl(parent);
			((AbstractGraphViewer)viewer).setInput(null, graphInput);
			
		} else {
			
			viewer = new Editor(tabFolderTop, SWT.NONE);
			viewer.setText("code");
			((Editor) viewer).setService(eventBroker, partService.getActivePart());
			
			Composite parent = new Composite(tabFolderTop, SWT.NONE);
			viewer.setControl(parent);
			((Editor) viewer).postConstruct(parent);
			((Editor) viewer).setInput(null, input);
		}
		
		// need to select the input to refresh the viewer
		// otherwise it will display empty item
		
		tabFolderTop.setSelection(viewer);
	}
		
	@PreDestroy
	public void preDestroy() {
	}
	
	
	@Focus
	public void onFocus() {
	}

	@Override
	public BaseExperiment getExperiment() {
		return experiment;
	}

	@Override
	public void setInput(MPart part, Object input) {
		if (input == null ) return;
		if (!(input instanceof Experiment)) return;
		
		Experiment experiment = (Experiment) input;
		this.experiment = experiment;
		
		part.setLabel("Profile: " + experiment.getName());
		part.setElementId(ElementIdManager.getElementId(experiment));
		
		Object []roots = experiment.getRootScopeChildren();
		views = new AbstractViewItem[roots.length];		
		
		for(int numViews=0; numViews<roots.length; numViews++) {
			RootScope root = (RootScope) roots[numViews];
			
			if (root.getType() == RootScopeType.CallingContextTree) {
				views[numViews] = new TopDownView(tabFolderBottom, SWT.NONE);
				
			} else if (root.getType() == RootScopeType.CallerTree) {
				views[numViews] = new BottomUpView(tabFolderBottom, SWT.NONE);
				
			} else if (root.getType() == RootScopeType.Flat) {
				
				views[numViews] = new FlatView(tabFolderBottom, SWT.NONE);
			} else {
				System.err.println("Not supported root: " + root.getType());
				break;
			}
			// TODO: make sure this statement is called early.
			// The content builder will need many services. So we have to make they are initialized
			views[numViews].setService(partService, eventBroker, databaseAddOn, this, menuService);

			Composite composite = new Composite(tabFolderBottom, SWT.NONE);
			views[numViews].setControl(composite);
			composite.setLayout(new GridLayout(1, false));

			RunViewCreation createView = new RunViewCreation(views[numViews], composite, experiment);
			Display display = shell.getDisplay();

			if (numViews == 0) {
				BusyIndicator.showWhile(display, createView);
			} else {
				display.asyncExec(createView);
			}
		}
		
		tabFolderBottom.setSelection(views[0]);
		tabFolderBottom.setFocus();
	}
	
	static private class RunViewCreation implements Runnable 
	{
		final private AbstractViewItem view;
		final private Composite parent;
		final private Experiment experiment;
		
		RunViewCreation(AbstractViewItem view, Composite parent, Experiment experiment) {
			this.view = view;
			this.parent = parent;
			this.experiment = experiment;
			
		}
		
		@Override
		public void run() {
			view.createContent(parent);
			view.setInput(experiment);
		}
	}
}