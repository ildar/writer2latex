/************************************************************************
 *
 *  ConfigBase.java
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
 *  Version 2.0 (2018-03-25)
 *
 */

package writer2latex.base;

/** Base implementation of writer2latex.api.Config 
*/

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.DOMImplementation;

import writer2latex.api.ComplexOption;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;

public abstract class ConfigBase implements writer2latex.api.Config {
	
    protected abstract int getOptionCount();
    protected abstract String getDefaultConfigPath();
	
    // Simple, named options
    protected Option[] options;
    // Complex, named options
    protected Map<String,ComplexOption> optionGroups;
    // Parameters (map from name to list of possible values)
    protected Map<String,List<String>> parameters;
    // Parameter value maps (map from name to value->final value maps)
    protected Map<String,Map<String,String>> paramValueMaps;
    // Current parameter values (map from name to current value;
    protected Map<String,String> currentParamValues;
	
    public ConfigBase() {
        options = new Option[getOptionCount()];
        optionGroups = new HashMap<String,ComplexOption>();
        parameters = new HashMap<String,List<String>>();
        paramValueMaps = new HashMap<String,Map<String,String>>();
        currentParamValues = new HashMap<String,String>();
    }
    
    /** Get the parameters defined by this configuration
     * 
     *  @return a copy of all parameters as a map from parameter names
     *  to a list of possible values
     */
    public Map<String,List<String>> getParameters() {
    	Map<String,List<String>> copy = new HashMap<String,List<String>>();
    	for (String sName : parameters.keySet()) {
    		List<String> itemCopy = new Vector<String>();
    		itemCopy.addAll(parameters.get(sName));
    		copy.put(sName, itemCopy);
    	}
    	return copy;
    }
    
    // Replace parameters in a string with their values
    // Parameters take the form {%name%} or a comma separated list
    // of several names {%name,name%}
    // The subclass can use this for whatever purpose is relevant
    protected String replaceParameters(String sSource) {
    	StringBuilder sb = new StringBuilder();
    	int i = 0;
    	int j,k;
    	while ((j=sSource.indexOf("{%", i))>-1) {
    		// Text from index i to j contains no parameters
    		sb.append(sSource.substring(i, j));
    		if ((k=sSource.indexOf("%}",j))>-1) {
    			// Text from index j+2 to index k is a parameter list
        		String[] sParams = sSource.substring(j+2, k).split(",");
        		CSVList values = new CSVList(",");
        		for (String sParam : sParams) {
        			// Ignore undefined parameters
        			if (parameters.containsKey(sParam)) {
        				String sCurrentValue
        				  = paramValueMaps.get(sParam).get(currentParamValues.get(sParam));
        				if (sCurrentValue.length()>0) {
        					// Only add non-empty parameter values to result
        					values.addValue(sCurrentValue);
        				}
        			}
        		}
        		sb.append(values.toString());
        		i=k+2;
    		}
    		else {
    			i=j;
    			break;
    		}
    	}
		// No more parameters, append remaining part of string
    	sb.append(sSource.substring(i));
    	return sb.toString();
    }
    	
    public void setOption(String sName,String sValue) {
    	if (sName!=null && sValue!=null) {
    		// First look for an option
    		for (int j=0; j<getOptionCount(); j++) {
    			if (sName.equals(options[j].getName())) {
    				options[j].setString(sValue);
    				return;
    			}
    		}
    		// Otherwise try parameters
    		if (parameters.containsKey(sName) && parameters.get(sName).contains(sValue)) {
				// Parameter exists, and value is valid
				currentParamValues.put(sName, sValue);
    		}
        }
    }
	
    public String getOption(String sName) {
    	if (sName!=null) {
    		// First look for an option
    		for (int j=0; j<getOptionCount(); j++) {
    			if (sName.equals(options[j].getName())) {
    				return options[j].getString();
    			}
    		}
    		// Otherwise try parameters
    		if (parameters.containsKey(sName)) {
				return currentParamValues.get(sName);
    		}
    	}
        return null;
    }
    
    public ComplexOption getComplexOption(String sGroup) {
   		return optionGroups.get(sGroup);
    }
    
	// The subclass may use this method to define option groups
	protected ComplexOption addComplexOption(String sGroup) {
		optionGroups.put(sGroup, new ComplexOption());
		return optionGroups.get(sGroup);
	}

    public void readDefaultConfig(String sName) throws IllegalArgumentException {
        InputStream is = this.getClass().getResourceAsStream(getDefaultConfigPath()+sName);
        if (is==null) {
            throw new IllegalArgumentException("The internal configuration '"+sName+ "' does not exist");
        }
        try {
            read(is);
        }
        catch (IOException e) {
            // This would imply a bug in the configuration file!
            throw new IllegalArgumentException("The internal configuration '"+sName+ "' is invalid");
        }
    }

	
    /** <p>Read configuration from a specified input stream</p>
     *  @param is the input stream to read the configuration from
     */
    public void read(InputStream is) throws IOException {
        DOMDocument doc = new DOMDocument("config",".xml");
        doc.read(is); // may throw an IOException
        Document dom = doc.getContentDOM();
        if (dom==null) {
            throw new IOException("Failed to parse configuration");
        }

        Node root = dom.getDocumentElement();
        Node child = root.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                Element elm = (Element)child;
                if (elm.getTagName().equals("option")) {
                	readOption(elm);
                }
                else if (elm.getTagName().equals("parameter")) {
                	readParameter(elm);
                }
                else {
                    readInner(elm);
                }
            }
            child = child.getNextSibling();
        }
    }
    
    public void read(File file) throws IOException {
    	read(new FileInputStream(file));
    }
    
    // Read an option node
    private void readOption(Element option) {
        String sName = option.getAttribute("name");
        String sValue = option.getAttribute("value");
        if (sName.length()>0) { setOption(sName,sValue); }
    }
    
    // Read a parameter node and set current value to default value
    private void readParameter(Element parameter) {
        String sName = parameter.getAttribute("name");
        String[] sValues = parameter.getAttribute("values").split(",");
        if (sName.length()>0 && sValues.length>0) {
        	List<String> values = new Vector<String>();
        	values.addAll(Arrays.asList(sValues));
        	parameters.put(sName,values);
        	currentParamValues.put(sName, values.get(0));
        	readValueMaps(parameter, sName);
        }
    }
    
    // Read value maps for a parameter
    private void readValueMaps(Element parameter, String sName) {
    	List<String> values = parameters.get(sName);
    	
    	// First create trivial maps for all values
    	Map<String,String> valueMap = new HashMap<String,String>();
    	for (String sValue : values) {
    		valueMap.put(sValue, sValue);
    	}
    	paramValueMaps.put(sName, valueMap);
    	
    	// Then read the actual maps
    	Node child = parameter.getFirstChild();
    	while (child!=null) {
    		if (child.getNodeType()==Node.ELEMENT_NODE) {
    			Element elm = (Element)child;
    			if (elm.getTagName().equals("value-map")) {
    				readValueMap(elm, sName);
    			}
    		}
    		child=child.getNextSibling();
    	}
    }
    
    private void readValueMap(Element valueMap, String sName) {
    	List<String> values = parameters.get(sName);

    	String sValue = valueMap.getAttribute("value");
		if (values.contains(sValue)) {
            String sTargetValue = Misc.getPCDATA(valueMap);
            paramValueMaps.get(sName).put(sValue, sTargetValue);
		}
    }
	
    /** Read configuration information from an xml element.
     *  The subclass must define this to read richer configuration data
     */
    protected abstract void readInner(Element elm);

    public void write(OutputStream os) throws IOException {
        DOMDocument doc = new DOMDocument("config",".xml");
        Document dom = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            dom = domImpl.createDocument("","config",null);
    	} catch (ParserConfigurationException e) {
    		// This will not happen
            e.printStackTrace();
            return;
        }
        Element rootElement = dom.getDocumentElement();

        // Write parameters first
        writeParameters(rootElement);

        // Then simple options
        writeOptions(rootElement);
        
        // Finally complex options
        writeInner(dom);
		
        doc.setContentDOM(dom);
        doc.write(os); // may throw an IOException
    }
	
    public void write(File file) throws IOException {
    	write(new FileOutputStream(file));
    }
    
    private void writeParameters(Element root) {
        for (String sName : parameters.keySet()) {
            Element paramNode = root.getOwnerDocument().createElement("parameter");
            paramNode.setAttribute("name",sName);
            List<String> values = parameters.get(sName);
            CSVList valueList = new CSVList(',');
            for (int i=1; i<values.size(); i++) {
            	valueList.addValue(values.get(i));
            }
            paramNode.setAttribute("values",valueList.toString());
            root.appendChild(paramNode);
            writeValueMaps(paramNode, sName);
        }
    }
    
    private void writeValueMaps(Element parameter, String sName) {
        Map<String,String> map = paramValueMaps.get(sName); 
        for (String sValue : map.keySet()) {
        	if (!sValue.equals(map.get(sValue))) {
        		Element valueMapNode = parameter.getOwnerDocument().createElement("value-map");
        		valueMapNode.setAttribute("value", sValue);
        		valueMapNode.appendChild(parameter.getOwnerDocument().createTextNode(map.get(sValue)));
        		parameter.appendChild(valueMapNode);
        	}
        }
    }
    
    private void writeOptions(Element root) {
        for (int i=0; i<getOptionCount(); i++) {
            Element optionNode = root.getOwnerDocument().createElement("option");
            optionNode.setAttribute("name",options[i].getName());
            optionNode.setAttribute("value",options[i].getString());
            root.appendChild(optionNode);
        }
    }

    /** Write configuration information to an xml document.
     *  The subclass must define this to write richer configuration data
     */
    protected abstract void writeInner(Document dom);
    
	
}

