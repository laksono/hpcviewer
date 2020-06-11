package edu.rice.cs.hpcviewer.ui.parts;

import org.eclipse.e4.ui.services.EMenuService;
import org.eclipse.swt.widgets.Composite;

import edu.rice.cs.hpc.data.experiment.BaseExperiment;
import edu.rice.cs.hpc.data.experiment.Experiment;
import edu.rice.cs.hpc.data.experiment.scope.RootScope;
import edu.rice.cs.hpc.data.experiment.scope.RootScopeType;
import edu.rice.cs.hpcviewer.ui.internal.FlatContentViewer;

public class FlatPart extends BaseViewPart
{
	static final public String ID = "edu.rice.cs.hpcviewer.ui.part.flat";
	static final public String IDdesc = "edu.rice.cs.hpcviewer.ui.partdescriptor.flat";


	public FlatPart() {	}


	@Override
	public String getViewType() {
		return Experiment.TITLE_FLAT_VIEW;
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	protected RootScope createRoot(BaseExperiment experiment) {
		
		RootScope rootCCT  = experiment.getRootScope(RootScopeType.CallingContextTree);
		RootScope rootFlat = experiment.getRootScope(RootScopeType.Flat);
		
		RootScope root = ((Experiment) experiment).createFlatView(rootCCT, rootFlat);

		return root;
	}

	@Override
	protected IContentViewer setContentViewer(Composite parent, EMenuService menuService) {

		IContentViewer contentViewer = new FlatContentViewer(partService, modelService, app, broker, databaseAddOn);
    	contentViewer.createContent(parent, menuService);
		return contentViewer;
	}

	@Override
	protected RootScopeType getRootType() {
		// TODO Auto-generated method stub
		return null;
	}

}
