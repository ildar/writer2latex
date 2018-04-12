/************************************************************************
 *
 *  FiledMasterNameProvider.java
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
 *  Version 1.2 (2010-12-09)
 *
 */ 

package org.openoffice.da.comp.writer2latex.util;

import java.util.HashSet;
import java.util.Set;

import com.sun.star.container.XNameAccess;
import com.sun.star.frame.XController;
import com.sun.star.frame.XDesktop;
import com.sun.star.frame.XModel;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

/** This class provides access to the names of all field masters in the current document
 */
public class FieldMasterNameProvider {
	private String[] fieldMasterNames;
	
	/** Construct a new <code>FieldMasterNameProvider</code>
	 * 
	 *  @param xContext the component context to get the desktop from
	 */
	public FieldMasterNameProvider(XComponentContext xContext) {
		fieldMasterNames = new String[0];

		// TODO: This code should be shared (identical with StyleNameProvider...)
		// Get the model for the current frame
		XModel xModel = null;
		try {
			Object desktop = xContext.getServiceManager().createInstanceWithContext("com.sun.star.frame.Desktop", xContext); 
			XDesktop xDesktop = (XDesktop)UnoRuntime.queryInterface(XDesktop.class, desktop);
            XController xController = xDesktop.getCurrentFrame().getController();
            if (xController!=null) {
                xModel = xController.getModel();
            }
		}
		catch (Exception e) {
			// do nothing
		}

		// Get the field masters from the model
		if (xModel!=null) {
			XTextFieldsSupplier xSupplier = (XTextFieldsSupplier) UnoRuntime.queryInterface(
					XTextFieldsSupplier.class, xModel);
			if (xSupplier!=null) {
				XNameAccess xFieldMasters = xSupplier.getTextFieldMasters();
				fieldMasterNames = xFieldMasters.getElementNames();
			}
		}
	}
	
	/** Get the names of all field masters relative to a given prefix
	 * 
	 * @param sPrefix the prefix to look for, e.g. "com.sun.star.text.fieldmaster.SetExpression."
	 * @return a read only <code>Set</code> containing all known names with the given prefix, stripped for the prefix
	 */
	public Set<String> getFieldMasterNames(String sPrefix) {
		Set<String> names = new HashSet<String>();
		for (String sName : fieldMasterNames) {
			if (sName.startsWith(sPrefix)) {
				names.add(sName.substring(sPrefix.length()));
			}
		}
		return names;
	}
	
}
