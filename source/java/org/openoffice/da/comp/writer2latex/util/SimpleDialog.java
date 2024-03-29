/************************************************************************
 *
 *  SimpleDialog.java
 *
 *  Copyright: 2002-2011 by Henrik Just
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
 *  Version 1.2 (2011-02-23)
 *
 */ 

package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This is a simple helper class to display and access a dialog based on an
 *  XML description (using the DialogProvider2 service).
 *  Unlike DialogBase, this class creates a dialog <em>without</em> event handlers.
 *  
 *  TODO: Use this class in ConfigurationDialogBase
 */
public class SimpleDialog {
	private XDialog xDialog;
	private DialogAccess dialogAccess;
	
	/** Create a new dialog
	 * 
	 * @param xContext the component context from which to get the service manager
	 * @param sDialogPath the path to the dialog 
	 */
	public SimpleDialog(XComponentContext xContext, String sDialogPath) {
		XMultiComponentFactory xMCF = xContext.getServiceManager();
		try {
			Object provider = xMCF.createInstanceWithContext("com.sun.star.awt.DialogProvider2", xContext);
			XDialogProvider2 xDialogProvider = (XDialogProvider2) UnoRuntime.queryInterface(XDialogProvider2.class, provider);
			String sDialogUrl = "vnd.sun.star.script:"+sDialogPath+"?location=application";
			xDialog = xDialogProvider.createDialog(sDialogUrl);
			dialogAccess = new DialogAccess(xDialog);
		} catch (com.sun.star.uno.Exception e) {
			xDialog = null;
			dialogAccess = null;
		}
	}
	
	/** Get the UNO dialog
	 * 
	 * @return the dialog, or null if creation failed
	 */
	public XDialog getDialog() {
		return xDialog;
	}
	
	/** Get access to the controls of the dialog
	 * 
	 * @return the control access helper, or null if creation failed
	 */
	public DialogAccess getControls() {
		return dialogAccess;
	}
}
