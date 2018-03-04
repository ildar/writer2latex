/************************************************************************
 *
 *	AlphabeticalIndexConverter.java
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
 *  Version 1.6 (2015-06-19)
 *
 */
package writer2latex.xhtml;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import writer2latex.office.IndexMark;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;
import writer2latex.util.StringComparator;

// Helper class (a struct) to contain information about an alphabetical index entry.
final class AlphabeticalEntry {
	String sWord; // the word for the index
	int nIndex; // the original index of this entry
}

/** This class processes alphabetical index marks and the associated index table
 */
class AlphabeticalIndexConverter extends IndexConverterHelper {
	
    private List<AlphabeticalEntry> index = new ArrayList<AlphabeticalEntry>(); // All words for the index
    private int nIndexIndex = -1; // Current index used for id's (of form idxN) 
    private int nAlphabeticalIndex = -1; // File containing alphabetical index

    AlphabeticalIndexConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter,XMLString.TEXT_ALPHABETICAL_INDEX_SOURCE,"index");
    }
    
    /** Return the id of the file containing the alphabetical index
     * 
     * @return the file id
     */
    int getFileIndex() {
    	return nAlphabeticalIndex;
    }
    
    /** Handle an alphabetical index mark
     * 
     * @param onode a text:alphabetical-index-mark node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleIndexMark(Node onode, Node hnode) {
        handleIndexMark(Misc.getAttribute(onode,XMLString.TEXT_STRING_VALUE),hnode);
    }

    /** Handle an alphabetical index mark start
     * 
     * @param onode a text:alphabetical-index-mark-start node
     * @param hnode the link target will be added to this inline HTML node
     */
    void handleIndexMarkStart(Node onode, Node hnode) {
        handleIndexMark(IndexMark.getIndexValue(onode),hnode);
    }
    
    // Create an entry for an index mark
    void handleIndexMark(String sWord, Node hnode) {
        if (sWord!=null) {
	        AlphabeticalEntry entry = new AlphabeticalEntry();
	        entry.sWord = sWord;
	        entry.nIndex = ++nIndexIndex; 
	        index.add(entry);
	        hnode.appendChild(converter.createTarget("idx"+nIndexIndex));
        }    	
    }

    /** Handle an alphabetical index
     * 
     * @param onode a text:alphabetical-index node
     * @param hnode the index will be added to this block HTML node
     */
    @Override void handleIndex(Element onode, Element hnode) {
    	// Register the file index (we assume that there is only one alphabetical index)
        nAlphabeticalIndex = converter.getOutFileIndex();
        converter.setIndexFile(null);
        super.handleIndex(onode, hnode);
    }
    
    @Override void populateIndex(Element source, Element container) {
       	sortEntries(source);
        String sEntryStyleName = getEntryStyleName(source);
        for (int i=0; i<=nIndexIndex; i++) {
            AlphabeticalEntry entry = index.get(i);
            Element li = converter.createElement("li");
            container.appendChild(li);
            Element p = getTextCv().createParagraph(li,sEntryStyleName);
            Element a = converter.createLink("idx"+entry.nIndex);
            p.appendChild(a);
            a.appendChild(converter.createTextNode(entry.sWord));
        }
    }
    
    // Sort the list of words based on the language defined by the index source
    private void sortEntries(Element source) {
		Comparator<AlphabeticalEntry> comparator = new StringComparator<AlphabeticalEntry>(
				Misc.getAttribute(source,XMLString.FO_LANGUAGE),
        		Misc.getAttribute(source, XMLString.FO_COUNTRY)) {

			public int compare(AlphabeticalEntry a, AlphabeticalEntry b) {
				return getCollator().compare(a.sWord, b.sWord);
			}
		};
		Collections.sort(index,comparator);
    }
    
    // Get the style name to use for the individual words
    private String getEntryStyleName(Element source) {
        // TODO: Should read the entire template
    	Node child = source.getFirstChild();
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE
                && child.getNodeName().equals(XMLString.TEXT_ALPHABETICAL_INDEX_ENTRY_TEMPLATE)) {
                // Note: There are actually three outline-levels: separator, 1, 2 and 3
                int nLevel = Misc.getPosInteger(Misc.getAttribute(child,XMLString.TEXT_OUTLINE_LEVEL),0);
                if (nLevel==1) {
                    return Misc.getAttribute(child,XMLString.TEXT_STYLE_NAME);
                }
            }
            child = child.getNextSibling();
        }
        return null;
    }    

}
