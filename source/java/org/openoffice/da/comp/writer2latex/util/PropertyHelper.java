/************************************************************************
 *
 *  PropertyHelper.java
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
 *  Version 1.0 (2008-07-21)
 *
 */
 
package org.openoffice.da.comp.writer2latex.util;

import java.util.Enumeration;
import java.util.Hashtable;

import com.sun.star.beans.PropertyValue; 

/** This class provides access by name to a <code>PropertyValue</code> array
 */
public class PropertyHelper {

    private Hashtable<String, Object> data;
	
    public PropertyHelper() {
        data = new Hashtable<String, Object>();
    }

    public PropertyHelper(PropertyValue[] props) {
        data = new Hashtable<String, Object>();
        int nLen = props.length;
        for (int i=0; i<nLen; i++) {
            data.put(props[i].Name,props[i].Value);
        }
    }
	
    public void put(String sName, Object value) {
        data.put(sName,value);
    }
	
    public Object get(String sName) {
        return data.get(sName);
    }
	
    public Enumeration<String> keys() {
        return data.keys();
    }
	
    public PropertyValue[] toArray() {
        int nSize = data.size();
        PropertyValue[] props = new PropertyValue[nSize];
        int i=0;
        Enumeration<String> keys = keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            props[i] = new PropertyValue();
            props[i].Name = sKey;
            props[i++].Value = get(sKey);
        }
        return props;
    }
	
}
