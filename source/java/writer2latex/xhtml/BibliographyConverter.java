/************************************************************************
 *
 *	BibliographyConverter.java
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
 *  Version 1.6 (2015-06-12)
 *
 */
package writer2latex.xhtml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

public class BibliographyConverter extends ConverterHelper {

    public BibliographyConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
    }
    
    public void handleBibliographyMark(Node onode, Node hnode) {
        Element anchor = converter.createLink("bibliography");
        hnode.appendChild(anchor);
        getTextCv().traversePCDATA(onode,anchor);
    }
    
    public void handleBibliography (Node onode, Node hnode) {
        // Use the content, not the template
        // This is a temp. solution. Later we want to be able to create
        // hyperlinks from the bib-item to the actual entry in the bibliography,
        // so we have to recreate the bibliography from the template.
        Node body = Misc.getChildByTagName(onode,XMLString.TEXT_INDEX_BODY);
        if (body!=null) {
            Element container = converter.createElement(converter.isHTML5() ? "section" : "div");
    		String sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);
    		if (sStyleName!=null) {
    	        StyleInfo sectionInfo = new StyleInfo();
    	        getSectionSc().applyStyle(sStyleName,sectionInfo);
    	        applyStyle(sectionInfo,container);
    		}
            
            converter.addTarget(container,"bibliography");
            converter.addEpubType(container, "bibliography");
            hnode.appendChild(container);
            //asapNode = converter.createTarget("bibliography");
            Node title = Misc.getChildByTagName(body,XMLString.TEXT_INDEX_TITLE);
            if (title!=null) { getTextCv().traverseBlockText(title,container); }
            getTextCv().traverseBlockText(body,container);
        }     
    }

}
