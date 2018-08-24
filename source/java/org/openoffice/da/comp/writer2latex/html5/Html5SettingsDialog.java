/************************************************************************
 *
 *  ToolbarSettingsDialog.java
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
 *  Version 2.0 (2018-08-17)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.html5;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XWindow;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

import com.sun.star.lib.uno.helper.WeakBase;

import org.openoffice.da.comp.writer2latex.util.DialogAccess;
import org.openoffice.da.comp.writer2latex.util.FilePicker;
import org.openoffice.da.comp.writer2latex.util.RegistryHelper;
import org.openoffice.da.comp.writer2latex.util.XPropertySetHelper;

/** This class provides a uno component which implements the configuration
 *  of the writer2xhtml toolbar
 */
public final class Html5SettingsDialog
    extends WeakBase
    implements XServiceInfo, XContainerWindowEventHandler {
	
	public static final String REGISTRY_PATH = "/org.openoffice.da.Writer2LaTeX.Options/Html5Settings";

    private XComponentContext xContext;
    private FilePicker filePicker;
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.Html5SettingsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = Html5SettingsDialog.class.getName();

    /** Create a new ToolbarSettingsDialog */
    public Html5SettingsDialog(XComponentContext xContext) {
        this.xContext = xContext;
        filePicker = new FilePicker(xContext);
    }
	
    // Implement XContainerWindowEventHandler
    public boolean callHandlerMethod(XWindow xWindow, Object event, String sMethod)
        throws com.sun.star.lang.WrappedTargetException {
		XDialog xDialog = (XDialog)UnoRuntime.queryInterface(XDialog.class, xWindow);
		DialogAccess dlg = new DialogAccess(xDialog);

        try {
            if (sMethod.equals("external_event") ){
                return handleExternalEvent(dlg, event);
            }
            else if (sMethod.equals("XhtmlViewChange")) {
            	return xhtmlViewChange(dlg);
            }
            else if (sMethod.equals("XhtmlBrowseClick")) {
            	return xhtmlBrowseClick(dlg);
            }
        }
        catch (com.sun.star.uno.RuntimeException e) {
            throw e;
        }
        catch (com.sun.star.uno.Exception e) {
            throw new com.sun.star.lang.WrappedTargetException(sMethod, this, e);
        }
        return false;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "external_event", "XhtmlViewChange", "XhtmlBrowseClick" };
        return sNames;
    }
    
    // Implement the interface XServiceInfo
    public boolean supportsService(String sServiceName) {
        return sServiceName.equals(__serviceName);
    }

    public String getImplementationName() {
        return __implementationName;
    }
    
    public String[] getSupportedServiceNames() {
        String[] sSupportedServiceNames = { __serviceName };
        return sSupportedServiceNames;
    }
	
    // Private stuff
    
    private boolean handleExternalEvent(DialogAccess dlg, Object aEventObject)
        throws com.sun.star.uno.Exception {
        try {
            String sMethod = AnyConverter.toString(aEventObject);
            if (sMethod.equals("ok")) {
                saveConfiguration(dlg);
                return true;
            } else if (sMethod.equals("back") || sMethod.equals("initialize")) {
                loadConfiguration(dlg);
                enableXhtmlExecutable(dlg);
                return true;
            }
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            throw new com.sun.star.lang.IllegalArgumentException(
            "Method external_event requires a string in the event object argument.", this,(short) -1);
        }
        return false;
    }
    
    private void loadConfiguration(DialogAccess dlg) {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	
		try {
			Object view = registry.getRegistryView(REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
			
        	dlg.setListBoxSelectedItem("XhtmlView",
        			XPropertySetHelper.getPropertyValueAsShort(xProps, "XhtmlView"));
        	dlg.setTextFieldText("XhtmlExecutable",
        			XPropertySetHelper.getPropertyValueAsString(xProps, "XhtmlExecutable"));
		} catch (Exception e) {
    		// Failed to get registry view
		}
    }
    
    private void saveConfiguration(DialogAccess dlg) {
		RegistryHelper registry = new RegistryHelper(xContext);
    	try {
    		Object view = registry.getRegistryView(REGISTRY_PATH, true);
    		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
   			XPropertySetHelper.setPropertyValue(xProps, "XhtmlView", dlg.getListBoxSelectedItem("XhtmlView"));
   			XPropertySetHelper.setPropertyValue(xProps, "XhtmlExecutable", dlg.getTextFieldText("XhtmlExecutable"));
   			
            // Commit registry changes
            XChangesBatch  xUpdateContext = (XChangesBatch)
                UnoRuntime.queryInterface(XChangesBatch.class,view);
            try {
                xUpdateContext.commitChanges();
            }
            catch (Exception e) {
                // ignore
            }
                        
        	registry.disposeRegistryView(view);
    	}
    	catch (Exception e) {
    		// Failed to get registry view
    	}		    	
    }
    
    private boolean xhtmlViewChange(DialogAccess dlg) {
    	enableXhtmlExecutable(dlg);
    	return true;
    }

    private void enableXhtmlExecutable(DialogAccess dlg) {
    	//dlg.setControlEnabled("XhtmlExecutable", nItem==2);
    	//dlg.setControlEnabled("XhtmlBrowseButton", nItem==2);
    }
    
    private boolean xhtmlBrowseClick(DialogAccess dlg) {
    	browseForExecutable(dlg,"XhtmlExecutable");
    	return true;
    }
	
    private boolean browseForExecutable(DialogAccess dlg, String sControlName) {
    	String sPath = filePicker.getPath();
    	if (sPath!=null) {
    		try {
				dlg.setComboBoxText(sControlName, new File(new URI(sPath)).getCanonicalPath());
			}
    		catch (IOException e) {
			}
    		catch (URISyntaxException e) {
			}
    	}     
    	return true;
    }

	
}


