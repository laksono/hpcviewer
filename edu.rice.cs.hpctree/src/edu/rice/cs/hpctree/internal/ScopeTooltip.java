// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctree.internal;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.IConfigRegistry;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.layer.cell.ILayerCell;
import org.eclipse.nebula.widgets.nattable.painter.cell.ICellPainter;
import org.eclipse.nebula.widgets.nattable.painter.cell.TextPainter;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Event;

import org.hpctoolkit.db.local.experiment.metric.BaseMetric;
import org.hpctoolkit.db.local.experiment.scope.CallSiteScope;
import org.hpctoolkit.db.local.experiment.scope.LineScope;
import org.hpctoolkit.db.local.experiment.scope.Scope;
import org.hpctoolkit.db.local.util.string.StringUtil;



/************************************************************
 * 
 * Class to display tooltips only for column header and the tree column
 *
 ************************************************************/
public class ScopeTooltip extends NatTableContentTooltip 
{
	private final static int MAX_TOOLTIP_CHAR = 80;
	private final ScopeTreeDataProvider bodyDataProvider;

	public ScopeTooltip(NatTable natTable, ScopeTreeDataProvider bodyDataProvider) {
		super(natTable, GridRegion.BODY, GridRegion.COLUMN_HEADER);
		this.bodyDataProvider = bodyDataProvider;
	}
	
	@Override
    protected String getText(Event event) {

        int col = this.natTable.getColumnPositionByX(event.x);
        int row = this.natTable.getRowPositionByY(event.y);
        int colIndex = this.natTable.getColumnIndexByPosition(col);
        int rowIndex = this.natTable.getRowIndexByPosition(row);
        
        // We only show the tooltip for column header and the tree column (col index = 0)
    	if (rowIndex == 0) {
    		// header of the table
    		if (colIndex > 0) {
        		BaseMetric metric = bodyDataProvider.getMetric(colIndex);
        		String name = metric.getDisplayName();
        		String desc = StringUtil.wrapScopeName(metric.getDescription(), MAX_TOOLTIP_CHAR);
        		if (desc == null)
        			return name;
        		return name + "\n" + desc;
    		}
    		return null;
    	}
    	if (colIndex > 0)
    		return null;

        ILayerCell cell = this.natTable.getCellByPosition(col, row);
        if (cell == null)
        	return null;
    	IConfigRegistry configRegistry = this.natTable.getConfigRegistry();
        ICellPainter painter = cell.getLayer().getCellPainter(col, row, cell, configRegistry); 

        GC gc = new GC(natTable);
        try {
        	Rectangle adjustedBounds = natTable.getLayerPainter().adjustCellBounds(col, row, cell.getBounds());
        	ICellPainter clickedCell = painter.getCellPainterAt(event.x, event.y, cell, gc, adjustedBounds, configRegistry);
        	if (clickedCell == null )
        		return null;

        	if (clickedCell instanceof CallSiteArrowPainter) {
        		Scope scope = bodyDataProvider.getRowObject(rowIndex);

        		if (scope instanceof CallSiteScope) {
        			LineScope ls = ((CallSiteScope)scope).getLineScope();
        			String filename = ls.getName();
        			return "Callsite at " + filename;
        		}
        	} else if (clickedCell instanceof TextPainter) {
        		String text = super.getText(event);
        		if (text != null && text.length() > 0) {
        			text = StringUtil.wrapScopeName(text, MAX_TOOLTIP_CHAR);
        		}
        		return text;        		
        	}
        } finally {
        	gc.dispose();
        }
    	return null;
	}
}
