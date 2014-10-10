/************************************************************************
 *
 *  Writer2xhtml.java
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
 *  Version 1.6 (2014-10-09)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

// TODO: Create common base for dispatcher classes

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.frame.XController;
import com.sun.star.frame.XFrame;
import com.sun.star.frame.XModel;
import com.sun.star.frame.XStorable;
import com.sun.star.io.XInputStream;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.task.XStatusIndicator;
import com.sun.star.task.XStatusIndicatorFactory;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XModifiable;

import org.openoffice.da.comp.w2lcommon.filter.UNOConverter;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;

import writer2latex.util.Misc;
       
/** This class implements the ui (dispatch) commands provided by Writer2xhtml.
 */
public final class Writer2xhtml extends WeakBase
    implements com.sun.star.lang.XServiceInfo,
    com.sun.star.frame.XDispatchProvider,
    com.sun.star.lang.XInitialization,
    com.sun.star.frame.XDispatch {
	
    private static final String PROTOCOL = "org.openoffice.da.writer2xhtml:";
    
    private enum TargetFormat { xhtml, xhtml11, xhtml_mathml, html5, epub };
        
    // From constructor+initialization
    private final XComponentContext m_xContext;
    private XFrame m_xFrame;
    private XModel xModel = null;
	
    // Global data
    private PropertyValue[] mediaProps = null;

    public static final String __implementationName = Writer2xhtml.class.getName();
    public static final String __serviceName = "com.sun.star.frame.ProtocolHandler"; 
    private static final String[] m_serviceNames = { __serviceName };
    
    // TODO: These should be configurable
    private TargetFormat xhtmlFormat = TargetFormat.xhtml_mathml;
    private TargetFormat epubFormat = TargetFormat.epub;
    
    private String getTargetExtension(TargetFormat format) {
    	switch (format) {
    	case xhtml: return ".html";
    	case xhtml11: return ".xhtml";
    	case xhtml_mathml: return ".xhtml";
    	case html5: return ".html";
    	case epub: return ".epub";
    	default: return "";
    	}
    }
      
    private String getDialogName(TargetFormat format) {
    	switch (format) {
    	case xhtml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
    	case xhtml11: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
    	case xhtml_mathml: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath";
    	case html5: return "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialogMath";
    	case epub: return "org.openoffice.da.comp.writer2xhtml.EpubOptionsDialog";
    	default: return "";
    	}
    }
      
    private String getFilterName(TargetFormat format) {
    	switch (format) {
    	case xhtml: return "org.openoffice.da.writer2xhtml";
    	case xhtml11: return "org.openoffice.da.writer2xhtml11";
    	case xhtml_mathml: return "org.openoffice.da.writer2xhtml.mathml";
    	case html5: return "org.openoffice.da.writer2xhtml5";
    	case epub: return "org.openoffice.da.writer2xhtml.epub";
    	default: return "";
    	}
    }
      
    public Writer2xhtml(XComponentContext xContext) {
        m_xContext = xContext;
    }
	
    // com.sun.star.lang.XInitialization:
    public void initialize( Object[] object )
        throws com.sun.star.uno.Exception {
        if ( object.length > 0 ) {
            // The first item is the current frame
            m_xFrame = (com.sun.star.frame.XFrame) UnoRuntime.queryInterface(
            com.sun.star.frame.XFrame.class, object[0]);
            // Get the model for the document from the frame
            XController xController = m_xFrame.getController();
            if (xController!=null) {
                xModel = xController.getModel();
            }
        }
    }
	
    // com.sun.star.lang.XServiceInfo:
    public String getImplementationName() {
        return __implementationName;
    }

    public boolean supportsService( String sService ) {
        int len = m_serviceNames.length;

        for( int i=0; i < len; i++) {
            if (sService.equals(m_serviceNames[i]))
                return true;
        }
        return false;
    }

    public String[] getSupportedServiceNames() {
        return m_serviceNames;
    }

	
    // com.sun.star.frame.XDispatchProvider:
    public com.sun.star.frame.XDispatch queryDispatch( com.sun.star.util.URL aURL,
        String sTargetFrameName, int iSearchFlags ) {
        if ( aURL.Protocol.compareTo(PROTOCOL) == 0 ) {
            if ( aURL.Path.compareTo("PublishAsXHTML") == 0 )
                return this;
            else if ( aURL.Path.compareTo("PublishAsEPUB") == 0 )
                return this;
        }
        return null;
    }

    public com.sun.star.frame.XDispatch[] queryDispatches(
    com.sun.star.frame.DispatchDescriptor[] seqDescriptors ) {
        int nCount = seqDescriptors.length;
        com.sun.star.frame.XDispatch[] seqDispatcher =
        new com.sun.star.frame.XDispatch[seqDescriptors.length];

        for( int i=0; i < nCount; ++i ) {
            seqDispatcher[i] = queryDispatch(seqDescriptors[i].FeatureURL,
            seqDescriptors[i].FrameName,
            seqDescriptors[i].SearchFlags );
        }
        return seqDispatcher;
    }


    // com.sun.star.frame.XDispatch:
    public void dispatch( com.sun.star.util.URL aURL,
        com.sun.star.beans.PropertyValue[] aArguments ) {
        if ( aURL.Protocol.compareTo(PROTOCOL) == 0 ) {
        	System.out.println(aURL.Protocol+" "+aURL.Path);
            if ( aURL.Path.compareTo("PublishAsXHTML") == 0 ) {
               	publish(xhtmlFormat);
                return;
            }
            else if ( aURL.Path.compareTo("PublishAsEPUB") == 0 ) {
                publish(epubFormat);
                return;
            }
        }
    }

    public void addStatusListener( com.sun.star.frame.XStatusListener xControl,
    com.sun.star.util.URL aURL ) {
    }

    public void removeStatusListener( com.sun.star.frame.XStatusListener xControl,
    com.sun.star.util.URL aURL ) {
    }
	
    // The actual commands...
    
    private void publish(TargetFormat format) {
        if (saveDocument() && updateMediaProperties(xhtmlFormat)) {
	        // Create a (somewhat coarse grained) status indicator/progress bar
	        XStatusIndicatorFactory xFactory = (com.sun.star.task.XStatusIndicatorFactory)
	            UnoRuntime.queryInterface(com.sun.star.task.XStatusIndicatorFactory.class, m_xFrame);
	        XStatusIndicator xStatus = xFactory.createStatusIndicator();
	        xStatus.start("Writer2xhtml",10);
	        xStatus.setValue(1); // At least we have started, that's 10% :-)
	        
	        System.out.println("Document location "+xModel.getURL());
	        
            try {
	            // Convert to desired format
	            UNOConverter converter = new UNOConverter(mediaProps, m_xContext);
				// Initialize the file access (to read the office document)
				XSimpleFileAccess2 sfa2 = null;
				try {
					Object sfaObject = m_xContext.getServiceManager().createInstanceWithContext(
							"com.sun.star.ucb.SimpleFileAccess", m_xContext);
					sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
				}
				catch (com.sun.star.uno.Exception e) {
					// failed to get SimpleFileAccess service (should not happen)
				}
	            XInputStream xis = sfa2.openFileRead(xModel.getURL());
	            converter.convert(xis);
	            xis.closeInput();
	        }
	        catch (IOException | com.sun.star.uno.Exception e) {
	            xStatus.end();
	            MessageBox msgBox = new MessageBox(m_xContext, m_xFrame);
	            msgBox.showMessage("Writer2xhtml Error","Failed to export document");
	            return;
			}
	        xStatus.setValue(6); // Export is finished, that's more than half :-)
	
	        if (xModel.getURL().startsWith("file:")) {
		        File file = urlToFile(getTargetURL(format));
		        if (file.exists()) {
			        // Open the file in the default application on this system (if any)
			        if (Desktop.isDesktopSupported()) {
			            Desktop desktop = Desktop.getDesktop();
			            try {
							desktop.open(file);
						} catch (IOException e) {
							System.err.println(e.getMessage());
						}
			        }        
			    }
		        else {        	
		            MessageBox msgBox = new MessageBox(m_xContext, m_xFrame);
		            msgBox.showMessage("Writer2xhtml Error","Failed to open exported document");
		        }
	        }
	        else {
	            MessageBox msgBox = new MessageBox(m_xContext, m_xFrame);
	            msgBox.showMessage("Writer2xhtml Error","Cannot open document on the location "+getTargetURL(format));	        	
	        }
	        
	        xStatus.setValue(10); // The user will usually not see this...
	
	        xStatus.end();
	    }
    }
    
    private boolean saveDocument() {
        String sDocumentUrl = xModel.getURL();
        if (sDocumentUrl.length()!=0) { // The document has a location
            XModifiable xModifiable = (XModifiable) UnoRuntime.queryInterface(XModifiable.class, xModel);
            if (xModifiable.isModified()) { // It is modified and need to be saved
    	        XStorable xStorable = (XStorable) UnoRuntime.queryInterface(XStorable.class, xModel);
    	        try {
    				xStorable.store();
    			} catch (com.sun.star.io.IOException e) {
    				return false;
    			}
            }
        }
        else { // No location, ask the user to save the document
            MessageBox msgBox = new MessageBox(m_xContext, m_xFrame);
            msgBox.showMessage("Document not saved!","Please save the document before publishing the file");
            return false;
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
            Object dialog = m_xContext.getServiceManager()
                .createInstanceWithContext(getDialogName(format), m_xContext);

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
    
    private File urlToFile(String sUrl) {
        try {
            return new File(new URI(sUrl));
        }
        catch (URISyntaxException e) {
            return new File(".");
        }
    }
	
	
}