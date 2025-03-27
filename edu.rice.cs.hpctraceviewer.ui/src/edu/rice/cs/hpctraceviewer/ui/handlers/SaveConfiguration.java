// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctraceviewer.ui.handlers;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import edu.rice.cs.hpctraceviewer.ui.TracePart;
import edu.rice.cs.hpctraceviewer.ui.base.ITraceViewAction;

import org.eclipse.e4.core.di.annotations.CanExecute;

public class SaveConfiguration 
{
	@Execute
	public void execute(MPart part) {

		if (part == null) {
			return;
		}
		TracePart traceView   = (TracePart) part.getObject();
		ITraceViewAction actions = traceView.getActions();
		actions.saveConfiguration();
	}
	
	
	@CanExecute
	public boolean canExecute() {
		
		return true;
	}
		
}
