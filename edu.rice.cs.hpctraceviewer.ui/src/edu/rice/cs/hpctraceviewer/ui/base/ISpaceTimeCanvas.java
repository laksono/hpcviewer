// SPDX-FileCopyrightText: 2024 Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: BSD-3-Clause

package edu.rice.cs.hpctraceviewer.ui.base;


public interface ISpaceTimeCanvas {

    /**Conversion factor from actual time to pixels on the x axis. To be implemented in subclasses.*/
    public double getScalePixelsPerTime();
    
    /**Conversion factor from actual processes to pixels on the y axis.  To be implemented in subclasses.*/
    public double getScalePixelsPerRank();

    /** dislay a temporary message on the canvas **/
	public void setMessage(String message);

}
