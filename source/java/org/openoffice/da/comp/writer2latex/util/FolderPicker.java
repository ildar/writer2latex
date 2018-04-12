/************************************************************************
 *
 *  FolderPicker.java
 *
 *  Copyright: 2002-2010 by Henrik Just
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
 *  Version 1.2 (2010-10-11)
 *
 */ 

package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.lang.XComponent;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.ui.dialogs.XFolderPicker;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class FolderPicker {
	
	private XComponentContext xContext; 
	
	/** Convenience wrapper class for the UNO folder picker service
	 * 
	 *  @param xContext the UNO component context from which the folder picker can be created
	 */
	public FolderPicker(XComponentContext xContext) {
        this.xContext = xContext;
	}
	
	/** Get a user selected path with a folder picker
	 * 
	 * @return the path or null if the dialog is canceled
	 */
	public String getPath() {
		// Create FolderPicker
		Object folderPicker = null;
		try {
			folderPicker = xContext.getServiceManager().createInstanceWithContext("com.sun.star.ui.dialogs.FolderPicker", xContext);
		}
		catch (com.sun.star.uno.Exception e) {
			return null;
		}

		// Display the FolderPicker
		XFolderPicker xFolderPicker = (XFolderPicker) UnoRuntime.queryInterface(XFolderPicker.class, folderPicker);
		XExecutableDialog xExecutable = (XExecutableDialog) UnoRuntime.queryInterface(XExecutableDialog.class, xFolderPicker);

		// Get the path
		String sPath = null;
		
		if (xExecutable.execute() == ExecutableDialogResults.OK) {
			sPath = xFolderPicker.getDirectory();
		}

		// Dispose the folder picker
		XComponent xComponent = (XComponent) UnoRuntime.queryInterface(XComponent.class, folderPicker);
		if (xComponent!=null) { // Seems not to be ??
			xComponent.dispose();
		}
		
		return sPath;
	}

}
