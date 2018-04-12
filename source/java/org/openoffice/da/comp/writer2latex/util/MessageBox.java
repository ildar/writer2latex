/************************************************************************
 *
 *  MessageBox.java
 *
 *  Copyright: 2002-2015 by Henrik Just
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
 *  Version 1.6 (2015-02-16)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.awt.Rectangle;
import com.sun.star.awt.WindowAttribute;
import com.sun.star.awt.WindowClass;
import com.sun.star.awt.WindowDescriptor;
import com.sun.star.awt.XMessageBox;
import com.sun.star.awt.XToolkit;
import com.sun.star.awt.XWindowPeer;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides simple access to a uno awt message box 
 */
public class MessageBox {

    private XFrame xFrame;
    private XToolkit xToolkit;
	
    /** Create a new MessageBox belonging to the current frame
     */
    public MessageBox(XComponentContext xContext) {
        this(xContext,null);
    }
	
    /** Create a new MessageBox belonging to a specific frame
     */
    public MessageBox(XComponentContext xContext, XFrame xFrame) {
        try {
            Object toolkit = xContext.getServiceManager()
                .createInstanceWithContext("com.sun.star.awt.Toolkit",xContext);
            xToolkit = (XToolkit) UnoRuntime.queryInterface(XToolkit.class,toolkit);
            if (xFrame==null) {
                Object desktop = xContext.getServiceManager()
                    .createInstanceWithContext("com.sun.star.frame.Desktop",xContext);
                XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,desktop);
                xFrame = xDesktop.getCurrentFrame();
            }
            this.xFrame = xFrame;
        }
        catch (Exception e) {
            // Failed to get toolkit or frame
            xToolkit = null;
            xFrame = null;
        }
    }

	
    public void showMessage(String sTitle, String sMessage) {
        if (xToolkit==null || xFrame==null) { return; }
        try {
            WindowDescriptor descriptor = new WindowDescriptor();
            descriptor.Type = WindowClass.MODALTOP;
            descriptor.WindowServiceName = "infobox";
            descriptor.ParentIndex = -1;
            descriptor.Parent = (XWindowPeer) UnoRuntime.queryInterface(
                XWindowPeer.class,xFrame.getContainerWindow());
            descriptor.Bounds = new Rectangle(200,100,300,200);
            descriptor.WindowAttributes = WindowAttribute.BORDER |
                WindowAttribute.MOVEABLE | WindowAttribute.CLOSEABLE;
            XWindowPeer xPeer = xToolkit.createWindow(descriptor);
            if (xPeer!=null) {
                XMessageBox xMessageBox = (XMessageBox)
                    UnoRuntime.queryInterface(XMessageBox.class,xPeer);
                if (xMessageBox!=null) {
                    xMessageBox.setCaptionText(sTitle);
                    xMessageBox.setMessageText(sMessage);
                    xMessageBox.execute();
                }
            }
        }
        catch (Exception e) {
            // ignore, give up...
        }
    }
	
}
