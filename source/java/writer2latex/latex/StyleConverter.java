/************************************************************************
 *
 *  StyleConverter.java
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
 *  Version 2.0 (2018-07-30)
 *
 */

package writer2latex.latex;

import java.util.HashMap;
import java.util.Map;

import writer2latex.latex.util.StyleMapItem;
import writer2latex.util.ExportNameCollection;
import writer2latex.office.OfficeReader;

/**
 *  <p>This is an abstract superclass for style converters.</p>
 */
public abstract class StyleConverter extends ConverterHelper {
    
    // Names and maps + necessary declarations for these styles
    protected ExportNameCollection styleNames = new ExportNameCollection(false);
    protected Map<String,StyleMapItem> styleMap = new HashMap<>();
    protected LaTeXDocumentPortion declarations = new LaTeXDocumentPortion(false);
	
    protected StyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }
	
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        decl.append(declarations);
    }
    
}