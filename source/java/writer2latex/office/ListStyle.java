/************************************************************************
 *
 *  ListStyle.java
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
 *  Version 2.0 (2018-04-30)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;

import writer2latex.util.Misc;

/** <p> Class representing a list style (including outline numbering) in OOo Writer</p>
*/
public class ListStyle extends OfficeStyle {
    // the file format doesn't specify a maximum nesting level, but OOo
    // currently supports 10
    private static final int MAX_LEVEL = 10; 
    private PropertySet[] level;
    private PropertySet[] levelStyle;

    public ListStyle() {
        level = new PropertySet[MAX_LEVEL+1];
        levelStyle = new PropertySet[MAX_LEVEL+1];
        for (int i=1; i<=MAX_LEVEL; i++) {
            level[i] = new PropertySet();
            levelStyle[i] = new PropertySet();
        }
    }
	
    public String getLevelType(int i) {
        if (i>=1 && i<=MAX_LEVEL) {
            return level[i].getName();
        }
        else {
            return null;
        }
    }
	
    public boolean isNumber(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_NUMBER.equals(level[i].getName()) ||
        XMLString.TEXT_OUTLINE_LEVEL_STYLE.equals(level[i].getName());
    }
	
    public boolean isBullet(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_BULLET.equals(level[i].getName());
    }

    public boolean isImage(int i) {
        return XMLString.TEXT_LIST_LEVEL_STYLE_IMAGE.equals(level[i].getName());
    }
    
    // Return true if this level is using the new list formatting of ODT 1.2
    public boolean isNewType(int i) {
    	return "label-alignment".equals(getLevelStyleProperty(i,XMLString.TEXT_LIST_LEVEL_POSITION_AND_SPACE_MODE));
    }

    public String getLevelProperty(int i, String sName) {
        if (i>=1 && i<=MAX_LEVEL) {
            return level[i].getProperty(sName);
        }
        else {
            return null;
        }
    }
	
    public String getLevelStyleProperty(int i, String sName) {
        if (i>=1 && i<=MAX_LEVEL) {
            return levelStyle[i].getProperty(sName);
        }
        else {
            return null;
        }
    }

    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        // Collect level information from child elements (text:list-level-style-*):
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE){
                String sLevel = Misc.getAttribute(child,XMLString.TEXT_LEVEL);
                if (sLevel!=null) {
                    int nLevel = Misc.getPosInteger(sLevel,1);
                    if (nLevel>=1 && nLevel<=MAX_LEVEL) {
                    	loadLevelPropertiesFromDOM(nLevel,child);
                    }
                }
            }
            child = child.getNextSibling();
        }
    }
    
    private void loadLevelPropertiesFromDOM(int nLevel, Node node) {
    	// Load the attributes
        level[nLevel].loadFromDOM(node);
        // Also include style:properties
        Node child = node.getFirstChild();
        while (child!=null) {
        	if (child.getNodeType()==Node.ELEMENT_NODE){
                if (child.getNodeName().equals(XMLString.STYLE_LIST_LEVEL_PROPERTIES)) {
                    levelStyle[nLevel].loadFromDOM(child);
        			loadLevelLabelPropertiesFromDOM(nLevel,child);
                }                                
            }                                   
            child = child.getNextSibling();
        }
    }
    
    private void loadLevelLabelPropertiesFromDOM(int nLevel, Node node) {
    	// Merge the properties from style:list-level-label-alignment
        Node child = node.getFirstChild();
        while (child!=null) {
        	if (child.getNodeType()==Node.ELEMENT_NODE){
        		if (child.getNodeName().equals(XMLString.STYLE_LIST_LEVEL_LABEL_ALIGNMENT)) {
        			levelStyle[nLevel].loadFromDOM(child);
                }
            }                                   
            child = child.getNextSibling();
        }    	
    }
	
}