/************************************************************************
 *
 *  Writer2xhtml.java
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

// TODO: Create common base for dispatcher classes

import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XComponent;
import com.sun.star.lib.uno.helper.WeakBase;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import org.openoffice.da.comp.w2lcommon.filter.UNOPublisher.TargetFormat;
import org.openoffice.da.comp.w2lcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2lcommon.helper.XPropertySetHelper;
       
/** This class implements the ui (dispatch) commands provided by Writer2xhtml.
 */
public final class Writer2xhtml extends WeakBase
    implements com.sun.star.lang.XServiceInfo,
    com.sun.star.frame.XDispatchProvider,
    com.sun.star.lang.XInitialization,
    com.sun.star.frame.XDispatch {
	
    private static final String PROTOCOL = "org.openoffice.da.writer2xhtml:";
    
    // From constructor+initialization
    private final XComponentContext m_xContext;
    private XFrame m_xFrame;
    private XhtmlUNOPublisher unoPublisher = null;
	
    // Global data
    public static final String __implementationName = Writer2xhtml.class.getName();
    public static final String __serviceName = "com.sun.star.frame.ProtocolHandler"; 
    private static final String[] m_serviceNames = { __serviceName };
    
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
            else if ( aURL.Path.compareTo("EditEPUBDocumentProperties") == 0 )
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
            if ( aURL.Path.compareTo("PublishAsXHTML") == 0 ) {
        		publish(TargetFormat.html5);
                return;
            }
            else if ( aURL.Path.compareTo("PublishAsEPUB") == 0 ) {
                publishAsEpub();
                return;
            }
            else if ( aURL.Path.compareTo("EditEPUBDocumentProperties") == 0 ) {
                editDocumentProperties();
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
    private void editDocumentProperties() {
        Object dialog;
		try {
			dialog = m_xContext.getServiceManager().createInstanceWithContext("org.openoffice.da.writer2xhtml.EpubMetadataDialog", m_xContext);
	        XExecutableDialog xDialog = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, dialog);
	        xDialog.execute();
	        // Dispose the dialog after execution (to free up the memory)
	        XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, dialog);
	        if (xComponent!=null) {
	        	xComponent.dispose();
	        }
		} catch (Exception e) {
			// Failed to get dialog
		}
    }
    
    private void publishAsEpub() {
    	RegistryHelper registry = new RegistryHelper(m_xContext);
		try {
			Object view = registry.getRegistryView(ToolbarSettingsDialog.REGISTRY_PATH, false);
			XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);			
			short nEpubFormat = XPropertySetHelper.getPropertyValueAsShort(xProps, "EpubFormat");
			switch (nEpubFormat) {
			case 0: publish(TargetFormat.epub); break;
			case 1: publish(TargetFormat.epub3);
			}
		} catch (Exception e) {
    		// Failed to get registry view
		}
    	
    }
    
    private void publish(TargetFormat format) {
    	if (unoPublisher==null) { 
    		unoPublisher = new XhtmlUNOPublisher(m_xContext,m_xFrame,"Writer2xhtml");
    	}
    	unoPublisher.publish(format);
    }	
	
}