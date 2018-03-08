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
 *  Version 2.0 (2018-03-08)
 *  
 */
package org.openoffice.da.comp.writer2xhtml;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.util.Vector;

import org.openoffice.da.comp.w2lcommon.filter.UNOPublisher;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;
import org.openoffice.da.comp.w2lcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2lcommon.helper.StreamGobbler;
import org.openoffice.da.comp.w2lcommon.helper.XPropertySetHelper;

import writer2latex.util.Misc;

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class XhtmlUNOPublisher extends UNOPublisher {
	
    public XhtmlUNOPublisher(XComponentContext xContext, XFrame xFrame, String sAppName) {
    	super(xContext, xFrame, sAppName);
    }
    
    
	/** Display the converted document depending on user settings
	 * 
	 *  @param sURL the URL of the converted document
	 *  @param format the target format
	 */
    @Override protected void postProcess(String sURL, TargetFormat format) {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	
    	short nView = 1;
    	String sExecutable = null;
    	
		try {
			Object view = registry.getRegistryView(ToolbarSettingsDialog.REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			
			if (format==TargetFormat.html5) {
				nView = XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlView");
				sExecutable = XPropertySetHelper.getPropertyValueAsString(xProps, "XhtmlExecutable");				
			}
			else { // EPUB				
				nView = XPropertySetHelper.getPropertyValueAsShort(xProps, "EpubView");
				sExecutable = XPropertySetHelper.getPropertyValueAsString(xProps, "EpubExecutable");
			}
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
        msgBox.showMessage("Writer2xhtml","Error: Failed to open exported document");
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
