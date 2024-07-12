// SPDX-FileCopyrightText: 2024 Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: BSD-3-Clause

package edu.rice.cs.hpcviewer.ui.handlers;

import javax.inject.Inject;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.basic.MWindow;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.swt.widgets.Shell;

import edu.rice.cs.hpcbase.IDatabaseIdentification;
import edu.rice.cs.hpcviewer.ui.addon.DatabaseCollection;

public class DatabaseSwitch extends RecentDatabase 
{
	private static final String ID_MENU_URI = "bundleclass://edu.rice.cs.hpcviewer.ui/" + 
											   DatabaseSwitch.class.getName();

	@Inject DatabaseCollection dbCollection;
	
	@Override
	protected void execute(MApplication application, 
						   MWindow      window,
						   EModelService modelService, 
						   EPartService partService, 
						   Shell shell,
						   IDatabaseIdentification database) {

		dbCollection.switchDatabase(shell, window, partService, modelService, database);
	}

	@Override
	protected String getURI() {
		return ID_MENU_URI;
	}

}
