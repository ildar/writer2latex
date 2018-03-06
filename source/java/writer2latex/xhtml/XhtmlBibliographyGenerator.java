/************************************************************************
 *
 *  BibliographyGenerator.java
 *
 *  Copyright: 2002-2015 by Henrik Just
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

package writer2latex.xhtml;

import org.w3c.dom.Element;

import writer2latex.base.BibliographyGenerator;
import writer2latex.office.OfficeReader;

class XhtmlBibliographyGenerator extends BibliographyGenerator {
	
	private Converter converter;
	private Element ul; // The container element
	private Element currentPar; // The paragraph of the current item
	
	XhtmlBibliographyGenerator(OfficeReader ofr, Converter converter) {
		super(ofr,false);
		this.converter = converter;
	}
	
	/** Populate the bibliography
	 * 
	 * @param bibliography a text:bibliography element
	 * @param ul an XHTML list element to contain the code
	 */
	void populateBibliography(Element bibliography, Element ul) {
		this.ul = ul;
		generateBibliography(bibliography);
	}

	@Override protected void insertBibliographyItem(String sStyleName, String sKey) {
		Element li = converter.createElement("li");
		converter.addTarget(li, "bib"+sKey);
		converter.addEpubType(li, "biblioentry");
		ul.appendChild(li);
		currentPar = converter.getTextCv().createParagraph(li, sStyleName);
	}
	
	@Override protected void insertBibliographyItemElement(String sStyleName, String sText) {
		if (sStyleName!=null) {
			converter.getTextCv().createInline(currentPar, sStyleName).appendChild(converter.createTextNode(sText));
		}
		else {
			currentPar.appendChild(converter.createTextNode(sText));			
		}
	}

}
