/************************************************************************
 *
 *	FootnoteConverter.java
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
import writer2latex.office.PropertySet;
import writer2latex.office.XMLString;

class FootnoteConverter extends NoteConverter {
	
    // Footnote position (can be page or document)
    private boolean bFootnotesAtPage = true;

    FootnoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,ofr.getFootnotesConfiguration());
        PropertySet configuration=ofr.getFootnotesConfiguration();
        if (configuration!=null) {
        	bFootnotesAtPage = !"document".equals(configuration.getProperty(XMLString.TEXT_FOOTNOTES_POSITION));
        }
    }
    
    /** Insert the footnotes gathered so far. Export will only happen if the source document configures footnotes
     *  per page, or if this is the final call of the method.
     * 
     * @param hnode a block HTML element to contain the footnotes
     * @param bFinal true if this is the final call
     */
    void insertFootnotes(Node hnode, boolean bFinal) {
        if (hasNotes()) {
        	if (bFootnotesAtPage) {
        		Element section = createNoteSection(hnode);

        		// Add footnote rule
        		Element rule = converter.createElement("hr");
        		StyleInfo info = new StyleInfo();
        		getPageSc().applyFootnoteRuleStyle(info);
        		getPageSc().applyStyle(info, rule);
        		section.appendChild(rule);

        		flushNotes(section);
        	}
        	else if (bFinal) {
        		// New page if required for footnotes as endnotes
        		if (config.getXhtmlSplitLevel()>0) { hnode = converter.nextOutFile(); }
        		Element section = createNoteSection(hnode);
        		insertNoteHeading(section, config.getFootnotesHeading(), "footnotes");        	
        		flushNotes(section);
        	}
        }
    }

}
