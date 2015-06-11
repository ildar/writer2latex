/************************************************************************
 *
 *	IndexConverterBase.java
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
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/** This is a base class for conversion of indexes (table of contents, bibliography, alphabetical index,
 *  list of tables, list of figures)
 */
public abstract class IndexConverterBase extends ConverterHelper {
	
	private String sEpubType;
	private String sSourceName;
    
	/** Construct a new index converter
	 * 
	 * @param ofr the office reader used to read the source document
	 * @param config the configuration
	 * @param converter the converter
	 * @param sSourceName the name of the source data element in the index
	 * @param sEpubType the EPUB 3 semantic type
	 */
	public IndexConverterBase(OfficeReader ofr, XhtmlConfig config, Converter converter,
			String sSourceName, String sEpubType) {
        super(ofr,config,converter);
		this.sSourceName = sSourceName;
		this.sEpubType = sEpubType;
    }
	
    /** Generate the actual contents of the index
     * 
     * @param source the index source
     * @param container an ul element to populate with list items
     */
    protected abstract void populateIndex(Element source, Element container);
	
    /** Handle an alphabetical index
     * 
     * @param onode a text:alphabetical-index node
     * @param hnode the index will be added to this block HTML node
     */
    public void handleIndex(Element onode, Element hnode) {
        Element source = Misc.getChildByTagName(onode,sSourceName);
        if (source!=null) {
            Element container = createContainer(onode, hnode); 
            generateTitle(source, container);
            generateIndex(source, container);
        }
    }
    
    // Create a container node for the index
    private Element createContainer(Element source, Element hnode) {
		Element container = converter.createElement(converter.isHTML5() ? "section" : "div");
		hnode.appendChild(container);

		converter.addEpubType(container, sEpubType);
		
		String sName = source.getAttribute(XMLString.TEXT_NAME);
		if (sName!=null) {
			converter.addTarget(container,sName);
		}
		
		String sStyleName = source.getAttribute(XMLString.TEXT_STYLE_NAME);
		if (sStyleName!=null) {
	        StyleInfo sectionInfo = new StyleInfo();
	        getSectionSc().applyStyle(sStyleName,sectionInfo);
	        applyStyle(sectionInfo,container);
		}
		return container;
    }
    
    // Generate the index title and add it to the container
    private void generateTitle(Element source, Element container) {
        Node title = Misc.getChildByTagName(source,XMLString.TEXT_INDEX_TITLE_TEMPLATE);
        if (title!=null) {
            Element h1 = converter.createElement("h1");
            container.appendChild(h1);
            String sStyleName = Misc.getAttribute(title,XMLString.TEXT_STYLE_NAME);
    		StyleInfo info = new StyleInfo();
    		info.sTagName = "h1";
    		getHeadingSc().applyStyle(1, sStyleName, info);
    		applyStyle(info,h1);
            getTextCv().traversePCDATA(title,h1);
        }
    }
    
    // Generate the index and add it to the container
    private void generateIndex(Element source, Element container) {
    	Element ul = converter.createElement("ul");
    	// TODO: Support column formatting from the index source
    	ul.setAttribute("style", "list-style-type:none;margin:0;padding:0");
    	container.appendChild(ul);
    	populateIndex(source,ul);
    }

}
