/************************************************************************
 *
 *  XhtmlStyleMap.java
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
 *  Version 1.6 (2014-10-24)
 *
 */

package writer2latex.xhtml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class XhtmlStyleMap {
	private Map<String,XhtmlStyleMapItem> items = new HashMap<String,XhtmlStyleMapItem>();
    
	public boolean contains(String sName) {
        return sName!=null && items.containsKey(sName);
    }
	
    public void put(String sName, XhtmlStyleMapItem item) {
        items.put(sName, item);
    }

    public XhtmlStyleMapItem get(String sName) {
        return items.get(sName);
    }

    public Iterator<String> getNames() {
        return items.keySet().iterator();
    }
	
}
