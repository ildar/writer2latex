/************************************************************************
 *
 *	BibliographyConverter.java
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
 *  Version 2.0 (2018-08-06)
 *
 */
package writer2latex.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/** This class handles the export of the bibliography. Most of the work is delegated to the
 *  {@link XhtmlBibliographyGenerator}.
 *  Note that a bibliography cannot be by chapter in ODF
 */
class BibliographyConverter extends IndexConverterHelper {
	
	private XhtmlBibliographyGenerator bibGenerator;

    BibliographyConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,XMLString.TEXT_BIBLIOGRAPHY_SOURCE);
        bibGenerator = new XhtmlBibliographyGenerator(ofr,converter);
    }
    
    void handleBibliographyMark(Node onode, Node hnode) {
    	String sKey = Misc.getAttribute(onode, XMLString.TEXT_IDENTIFIER);
    	if (sKey!=null) {
	        Element anchor = converter.createLink("bib"+sKey);
	        hnode.appendChild(anchor);
	        anchor.appendChild(converter.createTextNode(bibGenerator.generateCitation(sKey)));
    	}
    }
    
    @Override void generate(IndexData data) {
    	bibGenerator.populateBibliography(data.onode, data.hnode);
    }

}
