// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctraceviewer.ui.base;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;


/***************************************************
 * 
 * Basic class for a tab item
 *
 ***************************************************/
public abstract class AbstractBaseItem extends CTabItem implements ITraceItem 
{

	protected AbstractBaseItem(CTabFolder parent, int style) {
		super(parent, style);
	}
}
