/************************************************************************
 *
 *	NoteConverter.java
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
 *  Version 2.0 (2018-06-30)
 *
 */
package writer2latex.xhtml;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.OfficeReader;
import writer2latex.office.PropertySet;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/** This is a base class handles the conversion of footnotes and endnotes
 */
class NoteConverter extends ConverterHelper {
	
	// The notes configuration
	private PropertySet noteConfig;
	
	// The collection of notes
	private List<Node> notes = new ArrayList<Node>();

	/** Construct a new note converter
	 * 
	 * @param ofr the office reader used to read the source document
	 * @param config the configuration
	 * @param converter the converter
	 * @param noteConfig the configuration of the notes
	 */
    NoteConverter(OfficeReader ofr, XhtmlConfig config, Converter converter, PropertySet noteConfig) {
        super(ofr,config,converter);
        this.noteConfig = noteConfig;
    }
    
    /** Handle a footnote or endnote. This method inserts the citation and stores the actual note for later processing
     * 
     * @param onode a text:note element
     * @param hnode the inline HTML element to contain the citation
     */
    void handleNote(Node onode, Node hnode) {
    	// Create a style span for the citation
        String sCitBodyStyle = noteConfig.getProperty(XMLString.TEXT_CITATION_BODY_STYLE_NAME);
		Element span = getTextCv().createInline((Element) hnode,sCitBodyStyle);
        // Add target and back-link to the span
        String sId = Misc.getAttribute(onode,XMLString.TEXT_ID);
        Element link = converter.createLink(sId);
        converter.addTarget(link,"body"+sId);
		span.appendChild(link);
		// Get and insert the citation
        Element citation = Misc.getChildByTagName(onode,XMLString.TEXT_NOTE_CITATION);
        // Insert the citation
        if (citation!=null) {
        	getTextCv().traversePCDATA(citation,link);
        }
        // Remember the actual note
        notes.add(onode);
	}
    
    boolean hasNotes() {
    	return notes.size()>0;
    }
    
    Element createNoteSection(Node hnode) {
    	Element section = converter.createElement("section");
    	hnode.appendChild(section);
    	return section;
    }
     
    void insertNoteHeading(Node hnode, String sHeading, String sTarget) {
    	if (sHeading.length()>0) {
    		// Create heading
    		Element heading = converter.createElement("h1");
    		hnode.appendChild(heading);
    		heading.appendChild(converter.createTextNode(sHeading));

    		//if (config.getXhtmlSplitLevel()==0) {
    			//For single output file we need a target
    			//converter.addTarget(heading,sTarget);                
    		//}
    	}
    }
    
    void flushNotes(Node hnode) {
    	int nSize = notes.size();
		for (int i=0; i<nSize; i++) {
			Node note = notes.get(i);
			// Create container
			Element aside = converter.createElement("aside");
			hnode.appendChild(aside);
			// Get the citation
			Node citation = Misc.getChildByTagName(note,XMLString.TEXT_NOTE_CITATION);
			// Get the body
			Node body = Misc.getChildByTagName(note,XMLString.TEXT_NOTE_BODY);
			// Export the note
			String sId = Misc.getAttribute(note,XMLString.TEXT_ID); 
	        converter.addTarget(aside,sId);
			createAnchor(sId,citation);
	        getTextCv().traverseBlockText(body,aside);
		}
		notes.clear();
    }

    private void createAnchor(String sId, Node citation) {
        // Create target and link
        Element link = converter.createLink("body"+sId);

        // Style it
        String sCitStyle = noteConfig.getProperty(XMLString.TEXT_CITATION_STYLE_NAME);
        StyleInfo linkInfo = new StyleInfo();
        getTextSc().applyStyle(sCitStyle,linkInfo);
        applyStyle(linkInfo,link);

        // Add prefix
        String sPrefix = noteConfig.getProperty(XMLString.STYLE_NUM_PREFIX);
        if (sPrefix!=null) {
        	link.appendChild(converter.createTextNode(sPrefix));
        }

        // Add citation
        getTextCv().traversePCDATA(citation,link);

        // Add suffix
        String sSuffix = noteConfig.getProperty(XMLString.STYLE_NUM_SUFFIX);
        if (sSuffix!=null) {
        	link.appendChild(converter.createTextNode(sSuffix));        	
        }

        // Add space
        Element span = converter.createElement("span");
        span.appendChild(link);
        span.appendChild(converter.createTextNode(" "));

        // Save it for later insertion
        getTextCv().setAsapNode(span);
    }
    
}
