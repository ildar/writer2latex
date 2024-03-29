/************************************************************************
 *
 *  IndexMark.java
 *
 *  Copyright: 2002-2014 by Henrik Just
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
 *  Version 1.4 (2014-09-16)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;

import writer2latex.util.*;

/**
 *  <p>This class contains static methods to read an index-mark.</p>
 */
public final class IndexMark {

    // Node must be text:*-mark or text:*-mark-start 
    public final static String getIndexValue(Node node) {
        if (!node.getNodeName().endsWith("start")) {
            return Misc.getAttribute(node,XMLString.TEXT_STRING_VALUE);
        }
        else {
            return collectMark(node);
        }
    }

    // Node must be text:*-mark or text:*-mark-start 
    public final static String getKey1(Node node) {
        return Misc.getAttribute(node,XMLString.TEXT_KEY1);
    }

    // Node must be text:*-mark or text:*-mark-start 
    public final static String getKey2(Node node) {
        return Misc.getAttribute(node,XMLString.TEXT_KEY2);
    }

    // Collect a mark
    private final static Node getRightNode(Node node) {
        Node nextNode;
        do {nextNode = node.getNextSibling();
            if (nextNode!=null) { return nextNode; }
            node = node.getParentNode();
        } while (node!=null);
        return null;
    }

    private final static String collectMark(Node node) {
        StringBuilder buf = new StringBuilder();
        String sId = Misc.getAttribute(node,XMLString.TEXT_ID);
        node = getRightNode(node);
        while (node!=null) {
            if (node.getNodeType()==Node.TEXT_NODE) {
                buf.append(node.getNodeValue());
                node = getRightNode(node);
            }
            else if (node.getNodeType()==Node.ELEMENT_NODE) {
               boolean bReady = false;
               //String sNodeName = node.getNodeName();
               if (sId.equals(Misc.getAttribute(node,XMLString.TEXT_ID))) {
                   node = null; // found the end mark
                   bReady = true;
               }
               else if (OfficeReader.isTextElement(node) &&
                       !OfficeReader.isNoteElement(node)) {
                   if (node.hasChildNodes()) {
                       node = node.getFirstChild(); bReady=true;
                   }
               }
               if (!bReady) { node=getRightNode(node); };
            }
        }
        return buf.toString();
    }
	
	
}