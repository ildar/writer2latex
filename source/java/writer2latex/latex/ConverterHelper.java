/************************************************************************
 *
 *  ConverterHelper.java
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
 *  Version 1.6 (2015-06-20)
 *
 */

package writer2latex.latex;

import writer2latex.office.OfficeReader;

/**
 *  <p>This is an abstract superclass for converter helpers.</p>
 */
abstract class ConverterHelper {
    
    OfficeReader ofr;
    LaTeXConfig config;
    ConverterPalette palette;
	
    ConverterHelper(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        this.ofr = ofr;
        this.config = config;
        this.palette = palette;
    }
	
    abstract void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl);
    
}