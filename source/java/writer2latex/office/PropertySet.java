/************************************************************************
 *
 *  PropertySet.java
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
 *  Version 2.0 (2018-07-22)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import java.util.Hashtable;
import java.util.Map;

/** Class representing a set of style properties in an ODF document (actually this
    is simply the set of attributes of an element)</p> 
  */
public class PropertySet {
    private Map<String, String> properties = new Hashtable<>();
    private String sName;

    public PropertySet() {
        properties = new Hashtable<String, String>();
        sName="";
    }
    
    public int getSize() {
    	return properties.size();
    }
	
    public String getProperty(String sPropName) {
        if (sPropName!=null) {
            String sValue = properties.get(sPropName);
            if (sValue!=null && sValue.endsWith("inch")) {
                // Cut of inch to in
                return sValue.substring(0,sValue.length()-2);
            }
            else {
                return sValue;
            }
        }
        else {
            return null;
        }
    }
	
    public String getName() { return sName; }

    public void loadFromDOM(Node node) {
        // read the attributes of the node, if any
        if (node!=null) {
            sName = node.getNodeName();
            NamedNodeMap attrNodes = node.getAttributes();
            if (attrNodes!=null) {    
                int nLen = attrNodes.getLength();
                for (int i=0; i<nLen; i++){
                    Node attr = attrNodes.item(i);
                    properties.put(attr.getNodeName(),attr.getNodeValue());
                }
            }
        }
    }
	
    public boolean containsProperty(String sProperty) {
        return sProperty!=null && properties.containsKey(sProperty);
    }
	
    public void setProperty(String sProperty, String sValue){
        properties.put(sProperty,sValue);
    }
	
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String sKey : properties.keySet()) {
            String sValue = properties.get(sKey);
            sb.append(sKey).append("=").append(sValue).append(" ");
        }
        return sb.toString();
    }

}