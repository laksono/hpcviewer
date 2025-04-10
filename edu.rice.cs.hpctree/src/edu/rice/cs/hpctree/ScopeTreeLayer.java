// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctree;

import java.util.Collection;
import java.util.List;

import org.eclipse.nebula.widgets.nattable.command.ILayerCommand;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.data.command.RowDeleteCommand;
import org.eclipse.nebula.widgets.nattable.data.command.RowDeleteCommandHandler;
import org.eclipse.nebula.widgets.nattable.data.command.RowInsertCommand;
import org.eclipse.nebula.widgets.nattable.data.command.RowInsertCommandHandler;
import org.eclipse.nebula.widgets.nattable.layer.AbstractLayerTransform;
import org.eclipse.nebula.widgets.nattable.layer.IUniqueIndexLayer;
import org.eclipse.nebula.widgets.nattable.layer.LabelStack;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.BackgroundPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.CellPainterWrapper;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.decorator.CellPainterDecorator;
import org.eclipse.nebula.widgets.nattable.selection.SelectionLayer;
import org.eclipse.nebula.widgets.nattable.tree.ITreeRowModel;
import org.eclipse.nebula.widgets.nattable.tree.TreeLayer;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeExpandCollapseCommand;
import org.eclipse.nebula.widgets.nattable.tree.command.TreeExpandToLevelCommand;
import org.eclipse.nebula.widgets.nattable.tree.config.DefaultTreeLayerConfiguration;
import org.eclipse.nebula.widgets.nattable.tree.config.TreeConfigAttributes;
import org.eclipse.nebula.widgets.nattable.tree.painter.IndentedTreeImagePainter;

import org.hpctoolkit.db.local.experiment.scope.Scope;
import edu.rice.cs.hpctree.internal.ScopeTreeExpandCollapseCommandHandler;
import edu.rice.cs.hpctree.internal.ScopeTreeExpandToLevelCommandHandler;
import edu.rice.cs.hpctree.internal.config.ScopeTreeLayerConfiguration;

public class ScopeTreeLayer extends AbstractLayerTransform implements IUniqueIndexLayer 
{
    public static final String TREE_COLUMN_CELL = "TREE_COLUMN_CELL"; //$NON-NLS-1$
    public static final int TREE_COLUMN_NUMBER = 0;

    private final ScopeTreeRowModel treeRowModel;

    /**
     * The IndentedTreeImagePainter that paints indentation to the left of the
     * configured base painter and icons for expand/collapse if possible, to
     * render tree structure accordingly.
     */
    private IndentedTreeImagePainter indentedTreeImagePainter;

	
	public ScopeTreeLayer(IUniqueIndexLayer underlyingLayer, ScopeTreeRowModel treeRowModel) {
		super(underlyingLayer);
		this.treeRowModel = treeRowModel;
		this.indentedTreeImagePainter = new IndentedTreeImagePainter();
		
		addConfiguration(new ScopeTreeLayerConfiguration());
		
        registerCommandHandler(new ScopeTreeExpandCollapseCommandHandler(this));
        registerCommandHandler(new ScopeTreeExpandToLevelCommandHandler(this));
        
        List<?> listBodyData = ((IScopeTreeData)treeRowModel.getTreeData()).getList();
        registerCommandHandler(new RowInsertCommandHandler<>(listBodyData));
        registerCommandHandler(new RowDeleteCommandHandler<>(listBodyData));
	}

    /**
     * @return The underlying layer.
     * @since 2.0
     */
    @Override
    protected IUniqueIndexLayer getUnderlyingLayer() {
        return (IUniqueIndexLayer) super.getUnderlyingLayer();
    }



    @Override
    public LabelStack getConfigLabelsByPosition(int columnPosition, int rowPosition) {
        LabelStack configLabels = super.getConfigLabelsByPosition(columnPosition, rowPosition);

        if (columnPosition == TREE_COLUMN_NUMBER) {
            configLabels.addLabelOnTop(TREE_COLUMN_CELL);

            ILayerCell cell = getCellByPosition(columnPosition, rowPosition);
            if (cell != null) {
                int rowIndex = getRowIndexByPosition(cell.getOriginRowPosition());
                configLabels.addLabelOnTop(
                        DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + this.treeRowModel.depth(rowIndex));
                if (!this.treeRowModel.hasChildren(rowIndex)) {
                    configLabels.addLabelOnTop(DefaultTreeLayerConfiguration.TREE_LEAF_CONFIG_TYPE);
                } else {
                    if (this.treeRowModel.isCollapsed(rowIndex)) {
                        configLabels.addLabelOnTop(DefaultTreeLayerConfiguration.TREE_COLLAPSED_CONFIG_TYPE);
                    } else {
                        configLabels.addLabelOnTop(DefaultTreeLayerConfiguration.TREE_EXPANDED_CONFIG_TYPE);
                    }
                }
            }
        }
        return configLabels;
    }


    @Override
    public ICellPainter getCellPainter(
            int columnPosition, int rowPosition,
            ILayerCell cell, IConfigRegistry configRegistry) {
        ICellPainter cellPainter = super.getCellPainter(
                columnPosition, rowPosition, cell, configRegistry);

        if (cell.getConfigLabels().hasLabel(TREE_COLUMN_CELL)) {

            ICellPainter treeCellPainter = configRegistry.getConfigAttribute(
                    TreeConfigAttributes.TREE_STRUCTURE_PAINTER,
                    cell.getDisplayMode(),
                    cell.getConfigLabels());

            if (treeCellPainter != null) {
                IndentedTreeImagePainter treePainter = findIndentedTreeImagePainter(treeCellPainter);

                if (treePainter != null) {
                    treePainter.setBaseCellPainter(cellPainter);
                    cellPainter = treeCellPainter;
                } else {
                    // log error
                    // fallback
                    this.indentedTreeImagePainter.setBaseCellPainter(cellPainter);
                    cellPainter = new BackgroundPainter(this.indentedTreeImagePainter);
                }
            } else {
                // backwards compatibility fallback
                this.indentedTreeImagePainter.setBaseCellPainter(cellPainter);
                cellPainter = new BackgroundPainter(this.indentedTreeImagePainter);
            }
        }

        return cellPainter;
    }

    private IndentedTreeImagePainter findIndentedTreeImagePainter(ICellPainter painter) {
        IndentedTreeImagePainter result = null;
        if (painter instanceof IndentedTreeImagePainter) {
            result = (IndentedTreeImagePainter) painter;
        } else if (painter instanceof CellPainterWrapper
                && ((CellPainterWrapper) painter).getWrappedPainter() != null) {
            result = findIndentedTreeImagePainter(((CellPainterWrapper) painter).getWrappedPainter());
        } else if (painter instanceof CellPainterDecorator) {
            result = findIndentedTreeImagePainter(((CellPainterDecorator) painter).getBaseCellPainter());
            if (result == null) {
                result = findIndentedTreeImagePainter(((CellPainterDecorator) painter).getDecoratorCellPainter());
            }
        }
        return result;
    }
    
    

    @Override
    public boolean doCommand(ILayerCommand command) {
    	if (command instanceof TreeExpandCollapseCommand) {
    		return handleExpandCollapseCommand((TreeExpandCollapseCommand) command);
    	} else if (command instanceof TreeExpandToLevelCommand) {
    		return handleExpandToLevel((TreeExpandToLevelCommand) command);
    	}
        return super.doCommand(command);
    }
    
    
    protected boolean handleExpandCollapseCommand(TreeExpandCollapseCommand command) {
    	// transform position to index
        if (command.convertToTargetLayer(this)) {
            int rowIndex = command.getParentIndex();
            expandOrCollapseIndex(rowIndex);
            return true;
        }
        return super.doCommand(command);
    }
    
    
    protected boolean handleExpandToLevel(TreeExpandToLevelCommand command) {
    	// transform position to index
        if (command.convertToTargetLayer(this)) {
        	
        }
    	return super.doCommand(command);
    }
    

    /**
     * Performs an expand/collapse action dependent on the current state of the
     * tree node at the given row index.
     *
     * @param parentIndex
     *            The index of the row that shows the tree node for which the
     *            expand/collapse action should be performed.
     */
    public void expandOrCollapseIndex(int parentIndex) {
        if (this.treeRowModel.isCollapsed(parentIndex)) {
            expandTreeRow(parentIndex);
        } else {
            collapseTreeRow(parentIndex);
        }
    }

    /**
     * Collapses the tree node for the given row index.
     *
     * @param parentIndex
     *            The index of the row that shows the node that should be
     *            collapsed
     */
    public void collapseTreeRow(int parentIndex) {
    	// need to grab all child indexes
    	List<Integer> childrenIndexes = this.treeRowModel.getChildIndexes(parentIndex);
    	if (childrenIndexes == null || childrenIndexes.isEmpty())
    		// this should never happen unless the tree is corrupted
    		return;
    	
    	int []indexes = childrenIndexes.stream().mapToInt(Integer::intValue).toArray();

    	// fix issue #177: need to clear selected row if it's within the collapsed row
    	// if it isn't clear, nattable only highlight the first column
    	
    	IUniqueIndexLayer underlyingLayer = getUnderlyingLayer();
    	if (!(underlyingLayer instanceof SelectionLayer))
    		return;

    	SelectionLayer selectionLayer = (SelectionLayer) underlyingLayer;
		var ranges = selectionLayer.getSelectedRowPositions();
		int selectionRow = -1;
		
		if (!ranges.isEmpty()) {
    		var range = ranges.iterator().next();
    		selectionRow = range.start;
		}

    	// remove the children
    	doCommand(new RowDeleteCommand(this, indexes));
    	
    	int lastChild = childrenIndexes.get(childrenIndexes.size()-1);
    	
    	// issue #177: update the position of the selected row
    	if (selectionRow > parentIndex && selectionRow <= lastChild) {
    		// case 1: the selected row in within the expanded nodes
    		// in this case we just to clear the selection
			((SelectionLayer)underlyingLayer).clear(false);
			
    	} else if (selectionRow > lastChild) {
    		// case 2: the selected row is AFTER the index
    		// move the selection to the new row
    		int newRow = selectionRow - childrenIndexes.size();
    		selectionLayer.selectRow(0, newRow, false, false);
    	}
    }
    
    /**
     * Expands the tree node for the given row index.
     *
     * @param parentIndex
     *            The index of the row that shows the node that should be
     *            expanded
     */
    public void expandTreeRow(int parentIndex) {
    	List<Scope> children = this.treeRowModel.getDirectChildren(parentIndex);
    	if (children == null || children.isEmpty())
    		return;
    	
    	// issue #189 need to clear selected row if it's within the expanded rows
    	// if it isn't clear, nattable only highlight the first column
    	    	
    	IUniqueIndexLayer underlyingLayer = getUnderlyingLayer();
    	if (!(underlyingLayer instanceof SelectionLayer))
    		return;    	

		SelectionLayer selectionLayer = (SelectionLayer) underlyingLayer;
    	int selectionRow = -1;
		
		var ranges = selectionLayer.getSelectedRowPositions();
		if (!ranges.isEmpty()) {
    		var range = ranges.iterator().next();
    		selectionRow = range.start;
		}

    	doCommand(new RowInsertCommand<Scope>(parentIndex + 1, children));

    	// issue #189 move the selected row into a new index
    	// if the selected row is r and the expanded parent has x children,
    	// then the new selected row is r + x
    	if (selectionRow > parentIndex) {
    		int newRow = selectionRow + children.size();
    		selectionLayer.selectRow(0, newRow, false, false);
    	}
    }
    

    /**
     * @since 1.4
     */
    @Override
    public Collection<String> getProvidedLabels() {
        Collection<String> result = super.getProvidedLabels();

        result.add(TreeLayer.TREE_COLUMN_CELL);
        result.add(DefaultTreeLayerConfiguration.TREE_LEAF_CONFIG_TYPE);
        result.add(DefaultTreeLayerConfiguration.TREE_COLLAPSED_CONFIG_TYPE);
        result.add(DefaultTreeLayerConfiguration.TREE_EXPANDED_CONFIG_TYPE);
        // configure 5 levels to be configurable via CSS
        // if you need more you need to override this method
        result.add(DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + "0"); //$NON-NLS-1$
        result.add(DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + "1"); //$NON-NLS-1$
        result.add(DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + "2"); //$NON-NLS-1$
        result.add(DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + "3"); //$NON-NLS-1$
        result.add(DefaultTreeLayerConfiguration.TREE_DEPTH_CONFIG_TYPE + "4"); //$NON-NLS-1$

        return result;
    }


	public ITreeRowModel<?> getModel() {
		return this.treeRowModel;
	}


	@Override
	public int getColumnPositionByIndex(int columnIndex) {
        return getUnderlyingLayer().getColumnPositionByIndex(columnIndex);
	}


	@Override
	public int getRowPositionByIndex(int rowIndex) {
        return getUnderlyingLayer().getRowPositionByIndex(rowIndex);
	}
}
