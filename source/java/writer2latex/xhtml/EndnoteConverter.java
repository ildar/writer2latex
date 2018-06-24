/************************************************************************
 *
 *	EndnoteConverter.java
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
 *  Version 2.0 (2018-03-08)
 *
 */
package writer2latex.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.OfficeReader;

class EndnoteConverter extends NoteConverter {
	
    EndnoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,ofr.getEndnotesConfiguration());
    }
    
    /** Insert all the endnotes
     * 
     * @param hnode a block HTML element to contain the endnotes
     */
    void insertEndnotes(Node hnode) {
        if (hasNotes()) {
        	if (config.splitLevel()>0) { hnode = converter.nextOutFile(); }
        	Element section = createNoteSection(hnode);
        	insertNoteHeading(section, config.endnotesHeading(), "endnotes");
        	flushNotes(section);
        }
    }
}
