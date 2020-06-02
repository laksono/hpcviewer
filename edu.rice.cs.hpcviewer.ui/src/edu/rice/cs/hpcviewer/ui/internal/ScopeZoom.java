/**
 * 
 */
package edu.rice.cs.hpcviewer.ui.internal;

import edu.rice.cs.hpc.data.experiment.scope.CallSiteScopeCallerView;
import edu.rice.cs.hpc.data.experiment.scope.Scope;

/**
 * Class to manage zoom-in and zoom out of a scope
 */
public class ScopeZoom {
	// --------------------------------------------------------------------
	//	ATTRIBUTES
	// --------------------------------------------------------------------
	private ScopeTreeViewer viewer;
	
    private java.util.Stack<Scope> stackRootTree;

	// --------------------------------------------------------------------
	//	CONSTRUCTORS
	// --------------------------------------------------------------------
	/**
	 * Constructor to prepare zooms
	 * @param treeViewer
	 * @param objGUI
	 */
	public ScopeZoom ( ScopeTreeViewer treeViewer) {
		this.viewer = treeViewer;
		stackRootTree = new java.util.Stack<Scope>();
	}
	
	// --------------------------------------------------------------------
	//	METHODS
	// --------------------------------------------------------------------
	/**
	 * Zoom in from "old" scope to "new" scope, store the tree description (expanded items) 
	 * if necessary
	 * @param current
	 * @param old
	 */
	public void zoomIn (Scope current) {

		Scope old = (Scope) viewer.getInput();
		
		stackRootTree.push(old); // save the node for future zoom-out

		viewer.setInput(current);
		// we need to insert the selected node on the top of the table
		// FIXME: this approach is not elegant, but we don't have any choice
		// 			at the moment
		viewer.insertParentNode(current);
	}
	
	/**
	 * zoom out
	 */
	public void zoomOut () {
		Scope parent; 
		if(stackRootTree.size()>0) {
			// the tree has been zoomed
			parent = stackRootTree.pop();
		} else {
			// case where the tree hasn't been zoomed
			// FIXME: there must be a bug if the code comes to here !
			parent = (Scope)viewer.getInput();
			throw( new java.lang.RuntimeException("ScopeViewActions - illegal zoomout"+parent));
		}

		viewer.setInput( parent );
		// Bug fix: we need to insert the parent on the top of the table
		viewer.insertParentNode(parent);
	}
	
	/**
	 * Verify if zoom out is possible
	 * @return
	 */
	public boolean canZoomOut () {
		boolean bRet = (stackRootTree != null);
		if (bRet) {
			bRet = ( stackRootTree.size()>0 );
		}
		return bRet;
	}
	
	/**
	 * 
	 * @param node
	 * @return
	 */
	public boolean canZoomIn ( Scope node ) {
		if (node == null)
			return false;
		if (node instanceof CallSiteScopeCallerView) {
			// in caller view, we don't know exactly how many children a scope has
			// the most reliable way is to retrieve the "mark" if the scope has a child or not
			return ((CallSiteScopeCallerView)node).hasScopeChildren();
		}
		return ( node.getChildCount()>0 );
	}
	
	public void setViewer ( ScopeTreeViewer treeViewer ) {
		viewer = treeViewer;
	}
}
