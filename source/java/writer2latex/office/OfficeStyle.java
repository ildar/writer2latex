/************************************************************************
 *
 *  OfficeStyle.java
 *
 *  Copyright: 2002-2006 by Henrik Just
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
 *  Version 0.5 (2006-11-23)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;
import writer2latex.util.Misc;

/** <p> Abstract class representing a style in OOo </p> */
public abstract class OfficeStyle {
    // These attributes are defined by OfficeStyleFamily upon collection of styles
    protected String sName;
    protected OfficeStyleFamily family;
    protected boolean bAutomatic;

    private String sDisplayName;
    private String sParentName;
    private String sListStyleName; 
    private String sMasterPageName;

    public String getName() { return sName; }

    public OfficeStyleFamily getFamily() { return family; }

    public boolean isAutomatic() { return bAutomatic; }
	
    public String getDisplayName() { return sDisplayName; }

    public String getParentName() { return sParentName; }
    
    public OfficeStyle getParentStyle() {
    	return family.getStyle(sParentName);
    }
	
    public String getListStyleName() { return sListStyleName; }

    public String getMasterPageName() { return sMasterPageName; }
	
    public void loadStyleFromDOM(Node node){
        sDisplayName = Misc.getAttribute(node,XMLString.STYLE_DISPLAY_NAME);
        if (sDisplayName==null) { sDisplayName = sName; }
        sParentName = Misc.getAttribute(node,XMLString.STYLE_PARENT_STYLE_NAME);
        sListStyleName = Misc.getAttribute(node,XMLString.STYLE_LIST_STYLE_NAME);
        sMasterPageName = Misc.getAttribute(node,XMLString.STYLE_MASTER_PAGE_NAME);
    }
	
}