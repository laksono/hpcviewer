package edu.rice.cs.hpcviewer.ui.parts.editor;

import edu.rice.cs.hpcviewer.ui.parts.IBasePart;

public interface IUpperPart extends IBasePart 
{
	public String getTitle ();
	public String getPartDescriptorId();
	
	public boolean hasEqualInput(Object input);
	
	public void setMarker(int lineNumber);
}
