/************************************************************************
 *
 *	UserIndexConverter.java
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

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.office.IndexMark;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

//Helper class (a struct) to contain information about a user index entry (currently only index marks)
final class UserEntry {
	Element onode; // the original node
	int nChapterNumber; // The chapter number for this entry
	int nFileIndex; // the file index for the generated content
}


class UserIndexConverter extends IndexConverterHelper {

	private static final String USER_LINK_PREFIX = "usr";
	
	private List<UserEntry> userEntries = new ArrayList<>(); // All potential index items
	private int nUserIndex = -1; // Current index for id's (of form usrN)

	UserIndexConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
		super(ofr,config,converter,XMLString.TEXT_USER_INDEX_SOURCE);
	}
	
    /** Handle a user index mark
     * 
     * @param onode a text:user-index-mark or text:user-index-mark-start node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleUserMark(Node onode, Node hnode, int nChapterNumber) {
        hnode.appendChild(converter.createTarget(USER_LINK_PREFIX+(++nUserIndex)));
        UserEntry entry = new UserEntry();
        entry.onode = (Element) onode;
        entry.nChapterNumber = nChapterNumber;
        entry.nFileIndex = converter.getOutFileIndex();
        userEntries.add(entry);
    }
    
	@Override
	void generate(IndexData data) {
    	Element onode = data.onode;
    	//int nChapterNumber = data.nChapterNumber;
    	String sIndexName = data.onode.getAttribute(XMLString.TEXT_INDEX_NAME);
        Element ul = data.hnode;


        // TODO: Read the entire content of the entry templates!
        String[] sEntryStyleName = getEntryStyleName(onode);
        
        int nLen = userEntries.size();

        // Generate entries
        for (int i=0; i<nLen; i++) {
            UserEntry entry = userEntries.get(i);
            //String sNodeName = entry.onode.getTagName();
            //if (XMLString.TEXT_USER_INDEX_MARK.equals(sNodeName) || XMLString.TEXT_USER_INDEX_MARK_START.equals(sNodeName)) {
           	//if (data.nChapterNumber==nChapterNumber) {
            String sEntryIndexName = Misc.getAttribute(entry.onode, XMLString.TEXT_INDEX_NAME);
            if (sEntryIndexName==null || sEntryIndexName.equals(sIndexName)) {
	        	int nLevel = Misc.getPosInteger(entry.onode.getAttribute(XMLString.TEXT_OUTLINE_LEVEL),1);
	            Element p = createEntry(ul,sEntryStyleName[nLevel]);
	            Element a = converter.createLink(USER_LINK_PREFIX+i);
	            p.appendChild(a);
	            a.appendChild(converter.createTextNode(IndexMark.getIndexValue(entry.onode)));
            }
           	//}
            //}
        }		
    }
	
    // Get the style names to use for the individual words from the index source
    private String[] getEntryStyleName(Element source) {
        // TODO: Should read the entire template
    	String[] sStyleName = new String[11];
    	Node child = source.getFirstChild();
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                && child.getNodeName().equals(XMLString.TEXT_USER_INDEX_ENTRY_TEMPLATE)) {
                int nLevel = Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_OUTLINE_LEVEL),0);
                if (1<=nLevel && nLevel<=10) {
                    sStyleName[nLevel]=Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
                }
            }
            child = child.getNextSibling();
        }
        return sStyleName;
    }    
	
	// Create an entry in this list with that paragraph style name
	private Element createEntry(Element ul, String sStyleName) {
    	Element li = converter.createElement("li");
    	ul.appendChild(li);
    	return getTextCv().createParagraph(li,sStyleName);
	}
    
}
