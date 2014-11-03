/************************************************************************
 *
 *  XhtmlUNOPublisher.java
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
 *  Version 1.6 (2014-11-03)
 *  
 */
package org.openoffice.da.comp.writer2xhtml;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import org.openoffice.da.comp.w2lcommon.filter.UNOPublisher;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;

import writer2latex.util.Misc;

import com.sun.star.frame.XFrame;
import com.sun.star.uno.XComponentContext;

public class XhtmlUNOPublisher extends UNOPublisher {
	
    public XhtmlUNOPublisher(XComponentContext xContext, XFrame xFrame, String sAppName) {
    	super(xContext, xFrame, sAppName);
    }
    
    
	/** Display the converted document in the default application
	 * 
	 *  @param sURL the URL of the converted document
	 */
    @Override protected void postProcess(String sURL) {
        File file = Misc.urlToFile(sURL);
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
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2xhtml","Error: Failed to open exported document");
        }
    }
    
}
