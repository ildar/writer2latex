/************************************************************************
 *
 *  BibTeXDialog.java
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
 *  Version 1.6 (2014-12-11)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import com.sun.star.awt.XDialog;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.DialogBase;

/** This class provides a UNO dialog to insert a BibTeX bibliographic reference
 */
public class BibTeXDialog extends DialogBase { 
    //implements com.sun.star.lang.XInitialization {

    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.BibTeXDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.BibTeXDialog";

    /** Return the name of the library containing the dialog
     */
    public String getDialogLibraryName() {
        return "W4LDialogs";
    }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() {
        return "BibTeXEntry";
    }
	
    public void initialize() {
    }
	
    public void endDialog() {
    }

    /** Create a new BibTeXDialog */
    public BibTeXDialog(XComponentContext xContext) {
        super(xContext);
    }
	
    // Implement com.sun.star.lang.XInitialization
    /*public void initialize( Object[] object )
        throws com.sun.star.uno.Exception {
    }*/

   // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { };
        return sNames;
    }
	
	
}