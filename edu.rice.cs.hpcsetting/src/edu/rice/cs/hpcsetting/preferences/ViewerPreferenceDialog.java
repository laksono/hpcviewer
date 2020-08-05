package edu.rice.cs.hpcsetting.preferences;

import org.eclipse.jface.preference.IPreferencePage;
import org.eclipse.jface.preference.PreferenceDialog;
import org.eclipse.jface.preference.PreferenceManager;
import org.eclipse.jface.preference.PreferenceNode;
import org.eclipse.swt.widgets.Shell;


public class ViewerPreferenceDialog extends PreferenceDialog 
{	
	public ViewerPreferenceDialog(Shell parentShell) {
		super(parentShell, new PreferenceManager());
		
		parentShell.setText("Preferences");
	}

	
	public void addPage(final String label, IPreferencePage page) {
		
		PreferenceNode pNode = new PreferenceNode(label);
		pNode.setPage(page);
		getPreferenceManager().addToRoot(pNode);
	}
	
	public void addPage(final String parent, final String label, IPreferencePage page) {
		
		PreferenceNode pNode = new PreferenceNode(label);
		pNode.setPage(page);
		getPreferenceManager().addTo(parent, pNode);
	}
	
	@Override
	public int open() {
		super.create();
		getTreeViewer().expandAll();
		
		return super.open();
	}

	
	public static void main(String[] args) {

	}
}
