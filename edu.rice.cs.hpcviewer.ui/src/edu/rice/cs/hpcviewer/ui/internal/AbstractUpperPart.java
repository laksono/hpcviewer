package edu.rice.cs.hpcviewer.ui.internal;

import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;

import edu.rice.cs.hpcviewer.ui.base.IUpperPart;

public abstract class AbstractUpperPart extends CTabItem implements IUpperPart {

	public AbstractUpperPart(CTabFolder parent, int style) {
		super(parent, style);
	}

}
