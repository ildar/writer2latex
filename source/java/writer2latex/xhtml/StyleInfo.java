/************************************************************************
 *
 *  StyleInfo.java
 *
 *  Copyright: 2002-2007 by Henrik Just
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
 *  Version 0.5 (2007-02-27)
 *
 */

package writer2latex.xhtml;

import writer2latex.util.CSVList;

public class StyleInfo {
    public String sTagName = null;
    public String sClass = null;
    public CSVList props = new CSVList(";");
    public String sLang = null;
    public String sDir = null;
	
    public boolean hasAttributes() {
        return !props.isEmpty() || sClass!=null || sLang!=null || sDir!=null;
    }
}
