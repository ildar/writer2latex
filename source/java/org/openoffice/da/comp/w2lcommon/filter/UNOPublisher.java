/************************************************************************
 *
 *  UNOPublisher.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 *  
 *  Version 1.6 (2014-10-22)
 *  
 */
package org.openoffice.da.comp.w2lcommon.filter;

import java.io.IOException;

import org.openoffice.da.comp.w2lcommon.helper.MessageBox;
import writer2latex.util.Misc;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.XInputStream;
import com.sun.star.task.XStatusIndicator;
import com.sun.star.task.XStatusIndicatorFactory;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifiable;

/** This class converts an open office document to another format
 */
public class UNOPublisher {
	
    public enum TargetFormat { xhtml, xhtml11, xhtml_mathml, html5, epub, latex };
    
    private XComponentContext xContext;
    private XFrame xFrame;
    private XModel xModel=null;
    private PropertyValue[] mediaProps = null;
        
    /** Create a new <code>UNOPublisher</code> based on a loaded office document
     * 
     * @param xContext the component context from which new UNO services are instantiated
     * @param xFrame the current frame
     */
    public UNOPublisher(XComponentContext xContext, XFrame xFrame) {
    	this.xContext = xContext;
    	this.xFrame = xFrame;
        // Get the model for the document from the frame
        XController xController = xFrame.getController();
        if (xController!=null) {
            xModel = xController.getModel();
        }
    }
    
    /** Publish the document associated with this <code>UNOPublisher</code>. This involves four steps:
     *  (1) Check that the document is saved in the local file system.
     *  (2) Display the options dialog.
     *  (3) Save the document (if the modified flag is true).
     *  (4) Convert the document.
     * 
     * @param format the target format
     * @return the URL of the converted document, or null if the document was not converted
     */
    public String publish(TargetFormat format) {
        if (documentSaved() && updateMediaProperties(format)) {
	        // Create a (somewhat coarse grained) status indicator/progress bar
	        XStatusIndicatorFactory xFactory = (com.sun.star.task.XStatusIndicatorFactory)
	            UnoRuntime.queryInterface(com.sun.star.task.XStatusIndicatorFactory.class, xFrame);
	        XStatusIndicator xStatus = xFactory.createStatusIndicator();
	        xStatus.start("Writer2xhtml",10);
	        xStatus.setValue(1); // At least we have started, that's 10% :-)
	        
            try {
            	// Save document if required
            	saveDocument();
            	xStatus.setValue(4);
            	
	            // Convert to desired format
	            UNOConverter converter = new UNOConverter(mediaProps, xContext);
				// Initialize the file access (to read the office document)
				XSimpleFileAccess2 sfa2 = null;
				try {
					Object sfaObject = xContext.getServiceManager().createInstanceWithContext(
							"com.sun.star.ucb.SimpleFileAccess", xContext);
					sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
				}
				catch (com.sun.star.uno.Exception e) {
					// failed to get SimpleFileAccess service (should not happen)
				}
	            XInputStream xis = sfa2.openFileRead(xModel.getURL());
	            converter.convert(xis);
	            xis.closeInput();
	        }
	        catch (IOException e) {
	            xStatus.end();
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage("Writer2xhtml","Error: Failed to export document");
	            return null;
	        }
	        catch (com.sun.star.uno.Exception e) {
	            xStatus.end();
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage("Writer2xhtml","Error: Failed to export document");
	            return null;
			}
	        xStatus.setValue(10); // Export is finished (The user will usually not see this...)
	        xStatus.end();
	        return getTargetURL(format);
	    }
        return null;
    }
    
    private boolean documentSaved() {
        String sDocumentUrl = xModel.getURL();
        if (sDocumentUrl.length()==0) { // The document has no location
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2xhtml","Please save the document before publishing the file");
            return false;
        }
        else if (!".odt".equals(Misc.getFileExtension(sDocumentUrl)) && !".fodt".equals(Misc.getFileExtension(sDocumentUrl))) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2xhtml","Please save the document in OpenDocument format (.odt)");
            return false;        	        	
        }
        else if (!sDocumentUrl.startsWith("file:")) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2xhtml","Please save the document in the local file system");
            return false;        	
        }
        return true;
    }
    
    private boolean saveDocument() {
        XModifiable xModifiable = (XModifiable) UnoRuntime.queryInterface(XModifiable.class, xModel);
        if (xModifiable.isModified()) { // The document is modified and need to be saved
	        XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xModel);
	        try {
				xStorable.store();
			} catch (com.sun.star.io.IOException e) {
				return false;
			}
        }
        return true;
    }
    
    // Some utility methods
    private String getTargetURL(TargetFormat format) {
    	return Misc.removeExtension(xModel.getURL())+getTargetExtension(format);
    }
    
    private void prepareMediaProperties(TargetFormat format) {
        // Create inital media properties
        mediaProps = new PropertyValue[2];
        mediaProps[0] = new PropertyValue();
        mediaProps[0].Name = "FilterName";
        mediaProps[0].Value = getFilterName(format);
        mediaProps[1] = new PropertyValue();
        mediaProps[1].Name = "URL";
        mediaProps[1].Value = getTargetURL(format);
    }
    
    private boolean updateMediaProperties(TargetFormat format) {
    	prepareMediaProperties(format);
    	
        try {
            // Display options dialog
            Object dialog = xContext.getServiceManager()
                .createInstanceWithContext(getDialogName(format), xContext);

            XPropertyAccess xPropertyAccess = (XPropertyAccess)
                UnoRuntime.queryInterface(XPropertyAccess.class, dialog);
            xPropertyAccess.setPropertyValues(mediaProps);

            XExecutableDialog xDialog = (XExecutableDialog)
                UnoRuntime.queryInterface(XExecutableDialog.class, dialog);
            if (xDialog.execute()==ExecutableDialogResults.OK) {
                mediaProps = xPropertyAccess.getPropertyValues();
                return true;
            }
            else {
                mediaProps = null;
                return false;
            }
        }
        catch (com.sun.star.beans.UnknownPropertyException e) {
            // setPropertyValues will not fail..
            mediaProps = null;
            return false;
        }
        catch (com.sun.star.uno.Exception e) {
            // getServiceManager will not fail..
            mediaProps = null;
            return false;
        }
    }
    
    private static String getTargetExtension(TargetFormat format) {
    	switch (format) {
    	case xhtml: return ".html";
    	case xhtml11: return ".xhtml";
    	case xhtml_mathml: return ".xhtml";
    	case html5: return ".html";
    	case epub: return ".epub";
    	case latex: return ".tex";
    	default: return "";
    	}
    }
      
    private static String getDialogName(TargetFormat format) {
    	switch (format) {
    	case xhtml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
    	case xhtml11: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
    	case xhtml_mathml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath";
    	case html5: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath";
    	case epub: return "org.openoffice.da.comp.writer2xhtml.EpubOptionsDialog";
    	case latex: return "org.openoffice.da.comp.writer2xhtml.LaTeXOptionsDialog";
    	default: return "";
    	}
    }
      
    private static String getFilterName(TargetFormat format) {
    	switch (format) {
    	case xhtml: return "org.openoffice.da.writer2xhtml";
    	case xhtml11: return "org.openoffice.da.writer2xhtml11";
    	case xhtml_mathml: return "org.openoffice.da.writer2xhtml.mathml";
    	case html5: return "org.openoffice.da.writer2xhtml5";
    	case epub: return "org.openoffice.da.writer2xhtml.epub";
    	case latex: return "org.openoffice.da.writer2latex.latex";
    	default: return "";
    	}
    }

}
