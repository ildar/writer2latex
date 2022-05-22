/************************************************************************
 *
 *  LogViewerDialog.java
 *
 *  Copyright: 2002-2022 by Henrik Just
 *
 *  This file is part of Writer2LaTeX.
 *  
 *  Writer2LaTeX is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  Writer2LaTeX is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with Writer2LaTeX.  If not, see <http://www.gnu.org/licenses/>.
 * 
 *  Version 2.0 (2022-05-20)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.latex;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.awt.XDialog;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.writer2latex.util.DialogAccess;
import org.openoffice.da.comp.writer2latex.util.DialogBase;

/** This class provides a uno component which displays logfiles
 */
public class LogViewerDialog extends DialogBase 
    implements com.sun.star.lang.XInitialization {

    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.LogViewerDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = LogViewerDialog.class.getName();

    /** Return the name of the library containing the dialog
     */
    public String getDialogLibraryName() {
        return "W2LDialogs";
    }
	
    private String sBaseUrl = null;
    private String sLaTeXLog = null;
    private String sLaTeXErrors = null;
    private String sBiberLog = null;
    private String sMakeindexLog = null;

    /** Return the name of the dialog within the library
     */
    public String getDialogName() {
        return "LogViewer";
    }
	
    public void initialize() {
        if (sBaseUrl!=null) {
            sLaTeXLog = readTextFile(sBaseUrl+".log");
            sLaTeXErrors = errorFilter(sLaTeXLog);
            sBiberLog = readTextFile(sBaseUrl+".blg");
            sMakeindexLog = readTextFile(sBaseUrl+".ilg");
            setComboBoxText("LogContents",sLaTeXLog);
        }
    }
	
    public void endDialog() {
    }

    /** Create a new LogViewerDialog */
    public LogViewerDialog(XComponentContext xContext) {
        super(xContext);
    }
	
    // Implement com.sun.star.lang.XInitialization
    public void initialize( Object[] object )
        throws com.sun.star.uno.Exception {
        if ( object.length > 0 ) {
            if (object[0] instanceof String) {
                sBaseUrl = (String) object[0];
            }
        }
    }

   // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ViewLaTeXLog")) {
            setComboBoxText("LogContents",
            	getCheckBoxState("ErrorFilter")==DialogAccess.CHECKBOX_CHECKED ? sLaTeXErrors : sLaTeXLog);
            setControlEnabled("ErrorFilter",true);
        }
        else if (sMethod.equals("ViewBiberLog")) {
            setComboBoxText("LogContents", sBiberLog);
            setControlEnabled("ErrorFilter",false);
        }
        else if (sMethod.equals("ViewMakeindexLog")) {
            setComboBoxText("LogContents", sMakeindexLog);
            setControlEnabled("ErrorFilter",false);
        }
        else if (sMethod.equals("ErrorFilterChange")) {
            setComboBoxText("LogContents",
                	getCheckBoxState("ErrorFilter")==DialogAccess.CHECKBOX_CHECKED ? sLaTeXErrors : sLaTeXLog);            
        }
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "ViewLaTeXLog", "ViewBiberLog", "ViewMakeindexLog", "ErrorFilterChange" };
        return sNames;
    }
	
    // Utility methods
	
    private String readTextFile(String sUrl) {
        StringBuilder buf = new StringBuilder();
        try {
            File file = new File(new URI(sUrl));
            if (file.exists() && file.isFile()) {
                InputStreamReader isr = new InputStreamReader(new FileInputStream(file));
                int n;
                do {
                    n = isr.read();
                    if (n>-1) { buf.append((char)n); }
                }
                while (n>-1);
                isr.close();
            }
        }
        catch (URISyntaxException e) {
            return "";
        }
        catch (IOException e) {
            return "";
        }
        return buf.toString();
    }
    
    // Extract errors from LaTeX log file only
    private String errorFilter(String log) {
    	StringBuilder buf = new StringBuilder();
    	int nLen = log.length();
    	int nIndex = 0;
    	boolean bIncludeLines = false;
    	while (nIndex<nLen) {
    		int nNewline = log.indexOf('\n', nIndex);
    		if (nNewline==-1) nNewline = nLen;
    		if (nNewline-nIndex>1 && log.charAt(nIndex)=='!') {
    			bIncludeLines = true;
    		}
    		else if (nNewline==nIndex) {
    			if (bIncludeLines) {
    				buf.append('\n');
    			}
    			bIncludeLines = false;
    		}
    		if (bIncludeLines) {
    			buf.append(log.substring(nIndex,nNewline)).append('\n');
    		}
    		nIndex = nNewline+1;
    	}
    	return buf.toString();
    }
	
}



