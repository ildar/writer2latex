/************************************************************************
 *
 *  Config.java
 *
 *  Copyright: 2002-2009 by Henrik Just
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
 *  Version 1.2 (2009-11-19)
 *
 */

package writer2latex.api;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/** A complex option is a set of named keys, each pointing to a set of named attributes 
 */
public class ComplexOption {
	
	private Map<String,Map<String,String>> options = new HashMap<String,Map<String,String>>();
	
	/** Clear the contents of the set
	 * 
	 */
	public void clear() {
		options.clear();
	}
    
	/** Remove an option from the set, if it exists
	 * 
	 * @param sName the name of the key to remove
	 */
	public void remove(String sName) {
    	if (options.containsKey(sName)) {
    		options.remove(sName);
    	}
    }
    
	/** Define a key. If the key already exists, the old value will be replaced
	 * 
	 * @param sName the name of the key. The name must be non-empty, otherwise the request will be ignored.
	 * @param attributes
	 */
	public void put(String sName, Map<String,String> attributes) {
    	if (sName!=null && sName.length()>0) {
    		options.put(sName, attributes);
    	}
	}
	
	/** Define a key using a <i>copy</i> of a the provided attributes.
	 *  If the key already exists, the old value will be replaced
	 * 
	 * @param sName the name of the key. The name must be non-empty, otherwise the request will be ignored.
	 * @param attributes
	 */
	public void copy(String sName, Map<String,String> attributes) {
    	if (sName!=null && sName.length()>0) {
			Map<String,String> newAttributes = new HashMap<String,String>();
			for (String sAttrName : attributes.keySet()) {
				newAttributes.put(sAttrName, attributes.get(sAttrName));
			}
    		put(sName, newAttributes);
    	}
	}
	
	/** Get the value belonging to a key
	 * 
	 * @param sName the name of the key
	 * @return the attributes, or null if the option doesn't exist
	 */
	public Map<String,String> get(String sName) {
   		return options.get(sName);
	}
	
	/** Copy all values from another <code>ComplexOption</code>
	 * (overwrites existing values)
	 *  @param co another instance of <code>ComplexOption</code>
	 */
	public void copyAll(ComplexOption co) {
		for (String sName : co.keySet()) {
			copy(sName, co.get(sName));
		}
	}
	
	/** Get the names of all options that are currently defined by this complex option
	 * 
	 * @return all names as a <code>Set</code>
	 */
	public Set<String> keySet() {
		return options.keySet();
	}
	
	/** Test if this complex options contains a specific option name
	 * 
	 *  @param sName the name to test
	 *  @return true if the name exists
	 */
	public boolean containsKey(String sName) {
		return options.containsKey(sName);
	}
	

	
}
