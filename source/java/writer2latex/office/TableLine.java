/************************************************************************
 *
 *  TableLine.java
 *
 *  Copyright: 2002-2008 by Henrik Just
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
 *  Version 1.0 (2008-09-04) 
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;
import writer2latex.util.Misc;

/**
 * <p> This class represents the properties of a row or column in a table</p>
 */
public class TableLine {
    private String sStyleName;
    private String sVisibility;
    private String sDefaultCellStyleName;
    private boolean bDisplay;
    private boolean bHeader;
	
    public TableLine(Node node, boolean bHeader, boolean bDisplay) {
        // Node must be table:table-column or table:table-row
        sStyleName = Misc.getAttribute(node,XMLString.TABLE_STYLE_NAME);
        sVisibility = Misc.getAttribute(node,XMLString.TABLE_VISIBILITY);
        if (sVisibility==null) { sVisibility = "visible"; }
        sDefaultCellStyleName = Misc.getAttribute(node,XMLString.TABLE_DEFAULT_CELL_STYLE_NAME);
        this.bDisplay = bDisplay;
        this.bHeader = bHeader;
    }
	
    public String getStyleName() { return sStyleName; }

    public String getVisibility() { return sVisibility; }
	
    public boolean isCollapse() { return "collapse".equals(sVisibility); }

    public boolean isFilter() { return "filter".equals(sVisibility); }

    public String getDefaultCellStyleName() { return sDefaultCellStyleName; }

    public boolean isDisplay() { return bDisplay; }

    public boolean isHeader() { return bHeader; }

}
