// SPDX-FileCopyrightText: Contributors to the HPCToolkit Project
//
// SPDX-License-Identifier: Apache-2.0

package edu.rice.cs.hpctraceviewer.data.color;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Display;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.rice.cs.hpcbase.map.ProcedureClassData;
import edu.rice.cs.hpctraceviewer.config.TracePreferenceManager;
import edu.rice.cs.hpcdata.util.Constants;
import edu.rice.cs.hpctraceviewer.data.util.ProcedureClassMap;

/**************************************************************
 * A data structure designed to hold all the name-color pairs
 * needed for the actual drawing.
 **************************************************************/
public class ColorTable
{	
	/**A null function*/
	private static final String NULL_FUNCTION = "-Outside Timeline-";
	
	/** user defined color */
	private ProcedureClassMap classMap;
	
	private IColorGenerator colorGenerator;

	// data members
	
	private	ConcurrentMap<String, Color> colorMatcher;
	private	ConcurrentMap<String, Color> predefinedColorMatcher;
	
	private Map<Integer, ProcedureColor>  mapRGBtoProcedure;

	
	/**********************************************************************
	 * Constructor: Creates a new ColorTable with Display _display.
	 **********************************************************************/
	public ColorTable()
	{
		// initialize the procedure-color map (user-defined color)
		Display display = Display.getCurrent();
		classMap = new ProcedureClassMap(display);
		
		colorMatcher 		   = new ConcurrentHashMap<String, Color>();		
		predefinedColorMatcher = new ConcurrentHashMap<String, Color>();
		mapRGBtoProcedure	   = new HashMap<Integer, ProcedureColor>();
		
		setColorGenerator();
		initializeWhiteColor();
		resetPredefinedColor();
	}
	
	/**********************************************************************
	 * Dispose the allocated resources
	 **********************************************************************/
	public void dispose() {
		for (Color col: colorMatcher.values()) {
			if (col != null) col.dispose();
		}
		
		for (Color col: predefinedColorMatcher.values()) {
			if (col != null) col.dispose();
		}
		colorMatcher.clear();
		predefinedColorMatcher.clear();
		mapRGBtoProcedure.clear();
		classMap.dispose();
	}
	
	
	/***********************************************************************
	 * Reset user defined color into the default one.
	 ***********************************************************************/
	public void resetPredefinedColor() 
	{
		for (Color col: predefinedColorMatcher.values()) {
			if (col != null) {
				mapRGBtoProcedure.remove(col.getRGB().hashCode());
				col.dispose();
			}
		}
		predefinedColorMatcher.clear();
		classMap.clear();
		
		// reset the default
		classMap.refresh();
		
		Object []entries = classMap.getEntrySet();
		
		// iterate the map of pattern-to-color from the file, 
		// then create a hashmap of user-defined colors, and 
		//    the map from rgb to procedure name
		
		for (Object obj:entries) {
			@SuppressWarnings("unchecked")
			Entry<String, ProcedureClassData> entry = (Entry<String, ProcedureClassData>) obj;
			
			String proc = entry.getKey();
			ProcedureClassData data = entry.getValue();

			final RGB rgb = data.getRGB();
			Color color = createColor(rgb);
			
			// add to the map from procedure to color
			predefinedColorMatcher.put(proc, color);
			
			// add to the map from rgb to list of procedures
			// it's possible a rgb is associated to multiple procedures
			ProcedureColor colProcs = mapRGBtoProcedure.get(rgb.hashCode());
			
			if (colProcs == null) {
				colProcs = new ProcedureColor();
				colProcs.procName.add(proc);
				colProcs.color = color;
			} else if (!colProcs.procName.contains(proc)){
				colProcs.procName.add(proc);
				
			}
			mapRGBtoProcedure.put(rgb.hashCode(), colProcs);
		}
	}
	
	/**********************************************************************
	 * Returns the color in the colorMatcher that corresponds to the name's class
	 * @param name
	 * @return
	 **********************************************************************/
	public Color getColor(String name)
	{		
		return  createColorIfAbsent(name);
	}

	
	/************************************************************************
	 * Return the name of the procedure for a given RGB or Color hashcode.<br/>
	 * If the hashcode is not recognized, it returns null.<br/>
	 * Notes: on Mac, the hashcode for RGB is the same as the hashcode for Color
	 * 
	 * @param int hashcode
	 * @return {@code ProcedureColor} containing the name of the procedure and its color
	 ************************************************************************/
	public ProcedureColor getProcedureNameByColorHash(int hashcode) 
	{
		// get the normal procedure (if exist)
		return mapRGBtoProcedure.get(Integer.valueOf(hashcode));
	}
	
	
	/************************************************************************
	 * Reset the policy to generate color.
	 * The policy is defined in the viewer's preference. 
	 * Can be either name-based color (default) or random.
	 ************************************************************************/
	public void setColorGenerator() {
		// pick the color generator policy
		if (TracePreferenceManager.useNameBasedColorPolicy()) {
			colorGenerator = new NameBasedColorGenerator();
		} else {
			colorGenerator = new RandomColorGenerator();
		}
	}
	

	/************************************************************************
	 * Main method to generate color if necessary <br/>
	 * This creates a pair of color and image based on the procedure name.
	 * If a procedure is already assigned a color, we do nothing. <br/>
	 * Otherwise, it creates color and image to be assigned to this procedure.
	 * <br> If the list of colors is too big, it will pick randomly from existing
	 * color to avoid too many handles created. Some OS like Windows has limitation
	 * of the number of handles to be generated.
	 * 
	 * @param procName the name of the procedure
	 * 
	 * @return Color
	 ************************************************************************/
	private Color createColorIfAbsent(String procName)
	{
		String name = procName;
		Color color;
		
		// 1. check if it matches predefined colors
		final Entry<String, ProcedureClassData> data = classMap.getEntry(name);
		
		if (data != null) {
			name = data.getKey();
			color = predefinedColorMatcher.
					computeIfAbsent(name, val -> createColor(data.getValue().getRGB() )
								    );
		} else {			
			// 2. check if it match the existing color
			color = colorMatcher.computeIfAbsent(procName, 
										 	     val -> createColor( colorGenerator.createColor(procName) ) 
										 	     );
		}
		  
		// store in a hashmap the pair of RGB hashcode and procedure name
		// if the hash is already stored, we concatenate the procedure name
		storeProcedureName(color, name);
		
		return color;
	}
	
	
	/************************************************************************
	 * Store a procedure name to the map from rgb to procedure name
	 *   
	 * @param rgb
	 * @param procName
	 ************************************************************************/
	private void storeProcedureName(Color color, String procName)
	{		
		// store in a hashmap the pair of RGB hashcode and procedure name
		// if the hash is already stored, we concatenate the procedure name
		RGB rgb = color.getRGB();
		Integer key = Integer.valueOf(rgb.hashCode());
		ProcedureColor setOfProcs = mapRGBtoProcedure.get(key);
		
		if (setOfProcs != null) {
			if (!setOfProcs.procName.contains(procName)) {
				setOfProcs.procName.add(procName);
			}
			return;
		}
		setOfProcs = new ProcedureColor();
		setOfProcs.procName.add(procName);
		setOfProcs.color = color;
		
		mapRGBtoProcedure.put(key, setOfProcs);
	}
	
	
	/************************************************************************
	 * Create a pair color with a specified RGB.
	 * If the OS limits the number of colors, and we exceeds the quota,
	 * it will display an error message and throw a runtime exception.
	 * 
	 * @param rgb
	 * @return Color
	 ************************************************************************/
	private Color createColor(RGB rgb) 
	{
		Display display = Display.getCurrent();
		
		try {
			return new Color(display, rgb);

		} catch (Exception e) {
			// Windows only: in case we don't have enough GDI objects to be
			// created, we should notify users. They can then set the max
			// GGI object to higher number
			String msg = "The number of colors exceeds the quota from the OS.\n" +
						 e.getLocalizedMessage();
			Logger logger = LoggerFactory.getLogger(getClass());
			logger.error("Error resource creation: " + msg, e);
			
			MessageDialog.openError(display.getActiveShell(), 
									"Error " + e.getClass(), 
									msg
									);
			
			throw new RuntimeException(msg);
		}
	}

	
	/************************************************************************
	 * Initialize the predefined-value of white color
	 * 
	 * If the white color value is not initialize, we create a new one
	 * Otherwise, do nothing.
	 ************************************************************************/
	private void initializeWhiteColor() {
		// create our own white color so we can dispose later, instead of disposing
		//	Eclipse's white color
		Display display = Display.getCurrent();
		final RGB rgb_white = display.getSystemColor(SWT.COLOR_WHITE).getRGB();
		final Color col_white = new Color(display, rgb_white);
		
		colorMatcher.put(NULL_FUNCTION, col_white);
		
		ProcedureColor pc = new ProcedureColor(Constants.PROC_NO_ACTIVITY, col_white);
		mapRGBtoProcedure.put(rgb_white.hashCode(), pc);
	}
	
		
	@Override
	public String toString() {
		return "pc: " + predefinedColorMatcher.size() + ", cm: " + colorMatcher.size() + ", mr: " + mapRGBtoProcedure.size();
	}
}