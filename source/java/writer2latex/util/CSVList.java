/************************************************************************
 *
 *  CSVList.java
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
 *  Version 2.0 (2018-06-14)
 *
 */

package writer2latex.util;

import java.util.LinkedHashMap;
import java.util.Map;

/** This class maintains a list of items separated by commas or another separation character.
 * The items may be simple values or key/value pairs separated by a colon or another separation character.
 * Simple values and key/values pairs may be freely mixed within the same <code>CSVList</code>.
 */
public class CSVList{
    private String sItemSep;
    private String sKeyValueSep;
    // The CSVList is backed by a Map, which is accessible for other CSVList instances
    Map<String,String> items = new LinkedHashMap<>();
    
    /** Create a new <code>CSVList</code> with specific separators
     * 
     * @param sItemSep the separator between items
     * @param sKeyValueSep the separator between keys and values
     */
    public CSVList(String sItemSep, String sKeyValueSep) {
        this.sItemSep=sItemSep;
        this.sKeyValueSep=sKeyValueSep;
    }
	
    /** Create a new <code>CSVList</code> with a specific item separator (use default colon for key/values separator)
     * 
     * @param sItemSep the separator between items
     */
    public CSVList(String sItemSep) {
        this(sItemSep,":");
    }
    
    /** Create a new <code>CSVList</code> with a specific character as item separator (use default colon for key/values separator)
     * 
     * @param cItemSep the separator between items
     */
    public CSVList(char cItemSep) {
        this(Character.toString(cItemSep),":");
    }

    /** Add a simple value to the <code>CSVList</code>
     * 
     * @param sVal the value (ignored if null)
     */
    public void addValue(String sVal){
    	if (sVal!=null) {
    		items.put(sVal, null);
    	}
    }

    /** Add a key/value pair to the <code>CSVList</code>, replacing a previous value if the key already exists in the <code>CSVList</code>
     * 
     * @param sKey the key of the pair (ignored if null)
     * @param sVal the value of the pair (may be null, which creates a simple value)
     */
    public void addValue(String sKey, String sVal) {
    	if (sKey!=null) {
    		items.put(sKey, sVal);
    	}
    }
    
    /** Add all items from another <code>CSVList</code>. The separator strings for the other list is ignored.
     * 
     * @param list the <code>CSVList</code> containing the items to add
     */
    public void addValues(CSVList list) {
    	for (String sKey : list.items.keySet()) {
    		items.put(sKey, list.items.get(sKey));
    	}
    }
    
    /** Remove all values from the list
     */
    public void clear() {
    	items.clear();
    }
	
    /** Test whether this <code>CSVList</code> contains any items
     * 
     * @return true if the list is empty
     */
    public boolean isEmpty() {
        return items.size()==0;
    }
	
    public String toString() {
        StringBuilder buf = new StringBuilder();
        boolean bFirst=true;
        for (String sKey : items.keySet()) {
        	if (bFirst) { bFirst=false; } else { buf.append(sItemSep); }
        	buf.append(sKey);
        	if (items.get(sKey)!=null) {
        		buf.append(sKeyValueSep).append(items.get(sKey));
        	}
        }
        return buf.toString();
    }
	
}
