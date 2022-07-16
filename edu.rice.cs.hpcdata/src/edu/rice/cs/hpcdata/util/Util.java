//////////////////////////////////////////////////////////////////////////
//																		//
//	Util.java															//
//																		//
//	util.Util -- miscellaneous useful operations						//
//	Last edited: September 18, 2001 at 6:55 pm							//
//																		//
//	(c) Copyright 2002-2022 Rice University. All rights reserved.			//
//																		//
//////////////////////////////////////////////////////////////////////////




package edu.rice.cs.hpcdata.util;


import java.io.File;
import java.io.FilenameFilter;
import java.text.NumberFormat;

import edu.rice.cs.hpcdata.db.DatabaseManager;
import edu.rice.cs.hpcdata.experiment.scope.Scope;
import edu.rice.cs.hpcdata.experiment.source.EmptySourceFile;
import edu.rice.cs.hpcdata.experiment.source.SourceFile;

import java.text.DecimalFormat;



//////////////////////////////////////////////////////////////////////////
//	CLASS UTIL															//
//////////////////////////////////////////////////////////////////////////

 /**
 *
 * Miscellaneous useful operations.
 *
 */


public class Util
{


//////////////////////////////////////////////////////////////////////////
//	PRIVATE CONSTANTS													//
//////////////////////////////////////////////////////////////////////////


//////////////////////////////////////////////////////////////////////////
//	STRING CONVERSION													//
//////////////////////////////////////////////////////////////////////////


/*************************************************************************
 *	Returns the <code>boolean</code> corresponding to a given string.
 ************************************************************************/
	
public static boolean booleanValue(String s)
{
	return Boolean.valueOf(s).booleanValue();
}



public static int countSetBits(int n) 
{ 
    int count = 0; 
    while (n > 0) { 
        count += n & 1; 
        n >>= 1; 
    } 
    return count; 
} 



/*************************************************************************
 *	Returns a new <code>DecimalFormat</code> object with a given pattern.
 ************************************************************************/
	
public static DecimalFormat makeDecimalFormatter(String pattern)
{
	// make a formatter, checking that the locale allows this
	NumberFormat nf = NumberFormat.getInstance();
	assert ( nf instanceof DecimalFormat) : "bad arg to Util::makeDecimalFormatter";
	DecimalFormat df = (DecimalFormat) nf;
	
	// apply the given pattern
	df.applyPattern(pattern);
	
	return df;
}


/**
 * Class to filter the list of files in a directory and return only Database files.
 * Possible database files:
 * <ul>
 * <li> experiment.xml
 * <li> meta.db
 * </ul> 
 * @see {@link edu.rice.cs.hpcdata.util.Constants.DATABASE_FILENAME}
 */
public static class DatabaseFileFilter implements FilenameFilter {
	public boolean accept(File pathname, String sName) {
		if (sName.length() < 4) // the file should contain at least four letters: ".xml" or "*.db"
			return false;
		if (!pathname.canRead())
			return false;
		return DatabaseManager.isDatabaseFile(sName);
	}
}

/****
 * 
 * Class to filter the list of hpcrun file in a directory
 *
 */
public static class FileHpcrunFilter implements FilenameFilter {
	public boolean accept(File pathname, String sName) {
		int iLength = sName.length();
		if (iLength < 7) // the file should contain at least four letters: ".xml"
			return false;
		return (pathname.canRead() && sName.endsWith(".hpcrun"));
	}
}


/**
 *  File filter to find a file with a glob-style pattern
 *  It is primarily used by File.listdir method.
 */
public static class FileThreadsMetricFilter implements FilenameFilter {
	private String db_glob;
	
	public FileThreadsMetricFilter(String pattern) {
		db_glob = pattern.replace("*", ".*");
	}
	
	public boolean accept(File dir, String name) {
		
		boolean b = name.matches(db_glob);
		return b;
	}
	
}

public static File[] getListOfXMLFiles(String sDir) 
{
	// find XML files in this directory
	File files = new File(sDir);
	// for debugging purpose, let have separate variable
	File filesXML[] = files.listFiles(new DatabaseFileFilter());
	return filesXML;
}


static public String getObjectID(Object o) {
	return Integer.toHexString(System.identityHashCode(o));
}

/**
 * Converts a standard POSIX Shell globbing pattern into a regular expression
 * pattern. The result can be used with the standard {@link java.util.regex} API to
 * recognize strings which match the glob pattern.
 * <p/>
 * See also, the POSIX Shell language:
 * http://pubs.opengroup.org/onlinepubs/009695399/utilities/xcu_chap02.html#tag_02_13_01
 * 
 * @param pattern A glob pattern.
 * @return A regex pattern to recognize the given glob pattern.
 */
public static final String convertGlobToRegex(String pattern) {
    StringBuilder sb = new StringBuilder(pattern.length());
    int inGroup = 0;
    int inClass = 0;
    int firstIndexInClass = -1;
    char[] arr = pattern.toCharArray();
    for (int i = 0; i < arr.length; i++) {
        char ch = arr[i];
        switch (ch) {
            case '\\':
                if (++i >= arr.length) {
                    sb.append('\\');
                } else {
                    char next = arr[i];
                    switch (next) {
                        case ',':
                            // escape not needed
                            break;
                        case 'Q':
                        case 'E':
                            // extra escape needed
                            sb.append('\\');
                        default:
                            sb.append('\\');
                    }
                    sb.append(next);
                }
                break;
            case '*':
                if (inClass == 0)
                    sb.append(".*");
                else
                    sb.append('*');
                break;
            case '?':
                if (inClass == 0)
                    sb.append('.');
                else
                    sb.append('?');
                break;
            case '[':
                inClass++;
                firstIndexInClass = i+1;
                sb.append('[');
                break;
            case ']':
                inClass--;
                sb.append(']');
                break;
            case '.':
            case '(':
            case ')':
            case '+':
            case '|':
            case '^':
            case '$':
            case '@':
            case '%':
                if (inClass == 0 || (firstIndexInClass == i && ch == '^'))
                    sb.append('\\');
                sb.append(ch);
                break;
            case '!':
                if (firstIndexInClass == i)
                    sb.append('^');
                else
                    sb.append('!');
                break;
            case '{':
                inGroup++;
                sb.append('(');
                break;
            case '}':
                inGroup--;
                sb.append(')');
                break;
            case ',':
                if (inGroup > 0)
                    sb.append('|');
                else
                    sb.append(',');
                break;
            default:
                sb.append(ch);
        }
    }
    return sb.toString();
}

/***
 * check if the current display is correct or not.
 * This method only check the display configuration on Linux only.
 * 
 * @return true if the display is correct, false otherwise.
 */
static public boolean isCorrectDisplay() {
	
	if (!OSValidator.isUnix())
		return true;
	
	String display = System.getenv("DISPLAY");
	if (display != null) {
		return display.length()>0;
	}
	return false;
}


static public void printMemory() {
	
	final int MegaBytes = 1024*1024;
    long maxMemory = Runtime.getRuntime().maxMemory()/MegaBytes;

    System.out.println("Max memory  : " + maxMemory   + " MB");

}


/**
 * Verify if the file exist or not.
 * Remark: we will update the flag that indicates the availability of the source code
 * in the scope level. The reason is that it is less time consuming (apparently) to
 * access to the scope level instead of converting and checking into FileSystemSourceFile
 * level.
 * @param scope
 * @return true if the source is available. false otherwise
 */
static public boolean isFileReadable(Scope scope) {
	
	if (scope.iSourceCodeAvailability == Scope.SOURCE_CODE_AVAILABLE)
		return true;
	else if (scope.iSourceCodeAvailability == Scope.SOURCE_CODE_NOT_AVAILABLE)
		return false;
	
	SourceFile newFile = (scope.getSourceFile());
	if (newFile instanceof EmptySourceFile) {
		scope.iSourceCodeAvailability = Scope.SOURCE_CODE_NOT_AVAILABLE;
		return false;
	}
		
	if (newFile != null && !newFile.getName().isEmpty()) {
		if( (newFile != SourceFile.NONE) || ( newFile.isAvailable() )  ) {
			if(newFile != null) {
				// find the availability of the source code
				if (newFile.isAvailable()) {
					scope.iSourceCodeAvailability = Scope.SOURCE_CODE_AVAILABLE;
					return true;
				} 
			}
		}
	}
	
	// in this level, we don't think the source code is available
	scope.iSourceCodeAvailability = Scope.SOURCE_CODE_NOT_AVAILABLE;
	return false;
}

}
