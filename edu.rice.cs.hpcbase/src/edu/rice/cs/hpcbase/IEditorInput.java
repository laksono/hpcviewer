// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpcbase;

public interface IEditorInput extends IEditorViewerInput
{
	
	String getContent();
	
	int getLine();
}
