/************************************************************************
 *
 *  W2XExportFilter.java
 *
 *  Copyright: 2002-2014 by Henrik Just
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
 *  Version 1.6 (2014-10-06)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.filter.ExportFilterBase;


/** This class implements the xhtml export filter component
 */
public class W2XExportFilter extends ExportFilterBase {
    
    /** Service name for the component */
    public static final String __serviceName = "org.openoffice.da.comp.writer2xhtml.W2XExportFilter";
	
    /** Implementation name for the component */
    public static final String __implementationName = "org.openoffice.da.comp.writer2xhtml.W2XExportFilter";
	
    /** Filter name to include in error messages */
	public final String __displayName = "Writer2xhtml";

    public W2XExportFilter(XComponentContext xComponentContext1) {
        super(xComponentContext1);
    }

		
}



