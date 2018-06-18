/************************************************************************
 *
 *  ConverterHelper.java
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
 *  Version 2.0 (2018-06-19)
 *
 */

package writer2latex.latex;

import writer2latex.office.OfficeReader;

/** This is an abstract superclass for LaTeX converter helpers</p>
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
	
    /** Add any declarations to the preamble. Either by invoking the methods
     *  {@link writer2latex.latex.ConverterPalette#usepackage(String,String) usepackage} and 
     *  {@link ConverterPalette#usepackage(String) usepackage}
     *  or by adding content to the provided <code>LaTeXDocumentPortion</code> 
     * 
     * @param pacman the <code>LaTeXPacman</code> containing the package loading part of the LaTeX preamble
     * @param decl the <code>LaTeXDocumentPortion</code> containing the free form part of the LaTeX preamble
     */
    abstract void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl);
    
}