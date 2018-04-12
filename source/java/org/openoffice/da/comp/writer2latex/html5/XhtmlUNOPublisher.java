/************************************************************************
 *
 *  XhtmlUNOPublisher.java
 *
 *  Copyright: 2002-2018 by Henrik Just
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
 *  Version 2.0 (2018-04-11)
 *  
 */
package org.openoffice.da.comp.writer2latex.html5;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.openoffice.da.comp.writer2latex.base.UNOPublisher;
import org.openoffice.da.comp.writer2latex.util.MessageBox;
import org.openoffice.da.comp.writer2latex.util.RegistryHelper;
import org.openoffice.da.comp.writer2latex.util.StreamGobbler;
import org.openoffice.da.comp.writer2latex.util.XPropertySetHelper;

import writer2latex.util.Misc;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class XhtmlUNOPublisher extends UNOPublisher {
	
    public XhtmlUNOPublisher(XComponentContext xContext, XFrame xFrame) {
    	super(xContext, xFrame);
    }
    
    protected String getTargetExtension() {
    	return ".html"; //$NON-NLS-1$
    }
    
    protected String getDialogName() { 
    	return "org.openoffice.da.comp.writer2latex.XhtmlOptionsDialog";  //$NON-NLS-1$
	}

    protected  String getFilterName() {
    	return "org.openoffice.da.writer2html5"; //$NON-NLS-1$
   	}

    protected void postProcess(String sURL) {
    	// format is not used as we only handle HTML5
    	RegistryHelper registry = new RegistryHelper(xContext);
    	
    	short nView = 1;
    	String sExecutable = null;
    	
		try {
			Object view = registry.getRegistryView(Html5SettingsDialog.REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			
			nView = XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlView");
			sExecutable = XPropertySetHelper.getPropertyValueAsString(xProps, "XhtmlExecutable");
		} catch (Exception e) {
    		// Failed to get registry view
		}
		
        File file = Misc.urlToFile(sURL);
        if (file.exists()) {
    		if (nView==0) {
    			return;
    		}
    		 else if (nView==1) {
            	if (openWithDefaultApplication(file)) {
            		return;
            	}
    		}
    		else if (nView==2) {
    			if (openWithCustomApplication(file, sExecutable)) {
    				return;
    			}
    		}
        }
        MessageBox msgBox = new MessageBox(xContext, xFrame);
        msgBox.showMessage("Writer2LaTeX","Error: Failed to open exported document");
    }
    
    // Open the file in the default application on this system (if any)
    private boolean openWithDefaultApplication(File file) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
				desktop.open(file);
				return true;
			} catch (IOException e) {
				System.err.println(e.getMessage());
			}
        }
        return false;
    }
    
    // Open the file with the user defined application
    private boolean openWithCustomApplication(File file, String sExecutable) {
        try {
			Vector<String> command = new Vector<String>();
			command.add(sExecutable);
			command.add(file.getPath());
			
            ProcessBuilder pb = new ProcessBuilder(command);
            Process proc = pb.start();        

            // Gobble the error stream of the application
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR");            
            
            // Gobble the output stream of the application
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT");
                
            errorGobbler.start();
            outputGobbler.start();
                                    
            // The application exists if the process exits with 0
            return proc.waitFor()==0;
        }
        catch (InterruptedException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }
	}
    
}
