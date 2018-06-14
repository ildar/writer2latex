/************************************************************************
 *
 *  MasterPage.java
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
 *  Version 2.0 (2018-06-12)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;
import writer2latex.util.Misc;

/** This class represent a master page in an ODF text document
 */
public class MasterPage extends OfficeStyle {
    private PropertySet properties = new PropertySet();
    private Node header = null;
    private Node headerLeft = null;
    private Node headerFirst = null;
    private Node footer = null;
    private Node footerLeft = null;
    private Node footerFirst = null;
	
    public String getProperty(String sPropName) {
        return properties.getProperty(sPropName);
    }
	
    /*public String getPageLayoutName() {
    	return properties.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME);
    }*/
    
    public Node getHeader() { return header; }
    public Node getHeaderLeft() { return headerLeft; }
    public Node getHeaderFirst() { return headerFirst; }
    public Node getFooter() { return footer; }
    public Node getFooterLeft() { return footerLeft; }
    public Node getFooterFirst() { return footerFirst; }
	
    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        properties.loadFromDOM(node);
        header = Misc.getChildByTagName(node,XMLString.STYLE_HEADER);
        headerLeft = Misc.getChildByTagName(node,XMLString.STYLE_HEADER_LEFT);
        headerFirst = Misc.getChildByTagName(node,XMLString.STYLE_HEADER_FIRST);
        footer = Misc.getChildByTagName(node,XMLString.STYLE_FOOTER);
        footerLeft = Misc.getChildByTagName(node,XMLString.STYLE_FOOTER_LEFT);
        footerFirst = Misc.getChildByTagName(node,XMLString.STYLE_FOOTER_FIRST);
    }
	
}