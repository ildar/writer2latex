/************************************************************************
 *
 *  Epub3OptionsDialog.java
 *
 *  Copyright: 2002-2016 by Henrik Just
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
 *  Version 1.6 (2015-05-05)
 *
 */
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.uno.XComponentContext;

/** This class provides a UNO component which implements a filter UI for the
 *  EPUB 3 export. In this version the option to include NCX is enabled.
 */
public class Epub3OptionsDialog extends EpubOptionsDialog {

    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.Epub3OptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.Epub3OptionsDialog";
	
    /** Create a new Epub3OptionsDialog */
    public Epub3OptionsDialog(XComponentContext xContext) {
        super(xContext);
    }

}
