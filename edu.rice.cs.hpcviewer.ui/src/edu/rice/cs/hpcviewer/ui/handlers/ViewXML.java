// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpcbase.ui.IBasePart;
import edu.rice.cs.hpcbase.ui.IProfilePart;
import org.hpctoolkit.db.local.experiment.BaseExperiment;
import edu.rice.cs.hpclocal.IDatabaseLocal;
import edu.rice.cs.hpcviewer.ui.ProfilePart;
import edu.rice.cs.hpcviewer.ui.parts.editor.EditorInputFile;

import java.io.File;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.CanExecute;

public class ViewXML 
{
	@Inject EModelService 	   modelService;

	
	@Execute
	public void execute(@Optional @Active MPart part, Shell shell, MWindow window) {
		if (part != null) {
			Object obj = part.getObject();
			
			var file = getExperimentFile(obj);

			// sanity check: the file must exist
			if (file == null || !file.canRead())
				return;

			if (obj instanceof IProfilePart) {
				ProfilePart profilePart = (ProfilePart) obj;
				profilePart.addEditor(new EditorInputFile(shell, file));
				return;
			}
			// The current active element is trace view. 
			// find the corresponding profile part to display the XML file
			
			List<MPart> elements = modelService.findElements(part.getParent(), ProfilePart.ID, MPart.class); 
			for (MPart element: elements) {				

				ProfilePart profilePart = (ProfilePart) element.getObject();
				var database = profilePart.getInput();
				var experiment = database.getExperimentObject();
				
				if (profilePart.getExperiment() == experiment) {
					profilePart.addEditor(new EditorInputFile(shell, file));
					
					// sanity check: make sure the profile part is visible
					element.setVisible(true);
					
					// activate the part
					part.getParent().setSelectedElement(element);
					
					return;
				}
			}
		}
	}
	
	
	@CanExecute
	public boolean canExecute(@Optional @Active MPart part) {
		if (part == null)
			return false;
		
		var expFile = getExperimentFile(part.getObject());
		return expFile != null && expFile.canRead();
	}
	
	
	private File getExperimentFile(Object obj) {
		if (obj instanceof IBasePart) {
			var database = ((IBasePart)obj).getInput();
			
			// remote database is not supported
			if (database instanceof IDatabaseLocal) {
				
				var experiment = database.getExperimentObject();
				if (!(experiment instanceof BaseExperiment))
					return null;
				var baseExp = (BaseExperiment) experiment;
				if (baseExp.getMajorVersion() > 2)
					// meta.db is not supported
					return null;
				
				// we need to make sure the XML file really exist
				// for a merged database, we have a fake xml file. Hence, we shouldn't 
				// enable the menu if the current part is merged database.

				return baseExp.getExperimentFile();
			}
		}
		return null;
	}
}
