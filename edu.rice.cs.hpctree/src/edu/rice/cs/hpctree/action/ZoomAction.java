/**
 * 
 */
package edu.rice.cs.hpctree.action;

import java.util.Stack;

import edu.rice.cs.hpcdata.experiment.scope.CallSiteScopeCallerView;
import edu.rice.cs.hpcdata.experiment.scope.Scope;
import edu.rice.cs.hpctree.IScopeTreeAction;


/**
 * Class to manage zoom-in and zoom out of a scope
 */
public class ZoomAction 
{
	private static final String CONTEXT = "Zoom";
	
	// --------------------------------------------------------------------
	//	ATTRIBUTES
	// --------------------------------------------------------------------
    private final Stack<Scope> 		     stackRootTree;
    private final IScopeTreeAction       treeAction;
    private final IUndoableActionManager actionManager;
    
	// --------------------------------------------------------------------
	//	CONSTRUCTORS
	// --------------------------------------------------------------------
	/**
	 * Constructor to prepare zooms
	 * @param treeViewer
	 * @param objGUI
	 */
	public ZoomAction (IUndoableActionManager actionManager, IScopeTreeAction treeAction) {
		this.treeAction    = treeAction;
		this.actionManager = actionManager;
		this.stackRootTree = new Stack<Scope>();
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

		Scope old = treeAction.getRoot();
		
		stackRootTree.push(old); // save the node for future zoom-out
		actionManager.push(CONTEXT);
		
		treeAction.setRoot(current);
		treeAction.traverseOrExpand(0);
	}
	
	/**
	 * zoom out
	 */
	public void zoomOut () {
		if (!actionManager.canUndo(CONTEXT))
			return;
		
		Scope parent; 
		if(stackRootTree.size()>0) {
			// the tree has been zoomed
			parent = stackRootTree.pop();
			actionManager.undo();
			
			treeAction.setRoot(parent);
			treeAction.traverseOrExpand(0);

		} else {
			// case where the tree hasn't been zoomed
			// FIXME: there must be a bug if the code comes to here !
			parent = treeAction.getRoot();
			throw( new java.lang.RuntimeException("ScopeViewActions - illegal zoomout: "+parent));
		}
	}
	
	/**
	 * Verify if zoom out is possible
	 * @return
	 */
	public boolean canZoomOut () {
		if (!actionManager.canUndo(CONTEXT))
			return false;
		
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
		
		Scope input = treeAction.getRoot();
		if (input == node)
			return false;
		
		if (node instanceof CallSiteScopeCallerView) {
			// in caller view, we don't know exactly how many children a scope has
			// the most reliable way is to retrieve the "mark" if the scope has a child or not
			return ((CallSiteScopeCallerView)node).hasScopeChildren();
		}
		return ( node.getChildCount()>0 );
	}
}