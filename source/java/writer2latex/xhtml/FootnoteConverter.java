/************************************************************************
 *
 *	FootnoteConverter.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2015 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2015-06-11)
 *
 */
package writer2latex.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.OfficeReader;
import writer2latex.office.PropertySet;
import writer2latex.office.XMLString;

public class FootnoteConverter extends NoteConverter {
	
    // Footnote position (can be page or document)
    private boolean bFootnotesAtPage = true;

    public FootnoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
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
    public void insertFootnotes(Node hnode, boolean bFinal) {
        if (hasNotes()) {
        	if (bFootnotesAtPage) { // Add footnote rule
        		Element rule = converter.createElement("hr");
        		StyleInfo info = new StyleInfo();
        		getPageSc().applyFootnoteRuleStyle(info);
        		getPageSc().applyStyle(info, rule);
        		hnode.appendChild(rule);
        	}
        	else if (bFinal) { // New page if required for footnotes as endnotes
        		if (config.getXhtmlSplitLevel()>0) { hnode = converter.nextOutFile(); }
        		insertNoteHeading(hnode, config.getFootnotesHeading(), "footnotes");        	
        	}

        	if (bFinal || bFootnotesAtPage) { // Insert the footnotes
        		insertNotes(hnode);
        	}
        }
    }

}


