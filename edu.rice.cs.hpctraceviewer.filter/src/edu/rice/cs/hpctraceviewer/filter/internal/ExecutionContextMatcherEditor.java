// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctraceviewer.filter.internal;

import ca.odell.glazedlists.matchers.AbstractMatcherEditor;
import ca.odell.glazedlists.matchers.TextMatcherEditor;
import edu.rice.cs.hpcdata.db.IdTupleType;
import edu.rice.cs.hpcfilter.FilterDataItem;

public class ExecutionContextMatcherEditor extends AbstractMatcherEditor<FilterDataItem<IExecutionContext>> 
{
	ExecutionContextMatcher matcher;
	
	public ExecutionContextMatcherEditor(IdTupleType idTupleType) {
		matcher = new ExecutionContextMatcher(idTupleType);
	}
	
	
	public void filterText(String text) {
		matcher.setTextToFilter(text);
		fireChanged();
	}
	
	
	public void filterMinSamples(int minSamples) {
		matcher.setMinSamplesToFilter(minSamples);
		fireChanged();
	}
	
	public TextMatcherEditor<FilterDataItem<IExecutionContext>> getTextMatcherEditor() {
		return matcher.getTextMatcherEditor();
	}
	
	public void fireChanged() {
		fireChanged(matcher);
	}
}
