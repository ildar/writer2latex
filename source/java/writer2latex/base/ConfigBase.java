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
 *  Version 2.0 (2018-03-12)
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

public abstract class ConfigBase implements writer2latex.api.Config {
	
    protected abstract int getOptionCount();
    protected abstract String getDefaultConfigPath();
	
    // Simple, named options
    protected Option[] options;
    // Complex, named options
    protected Map<String,ComplexOption> optionGroups;
    // Parameters (First item is current value; tail contains all valid values)
    protected Map<String,List<String>> parameters;
	
    public ConfigBase() {
        options = new Option[getOptionCount()];
        optionGroups = new HashMap<String,ComplexOption>();
        parameters = new HashMap<String,List<String>>();
    }
    
    public Map<String,List<String>> getParameters() {
    	Map<String,List<String>> allParams = new HashMap<String,List<String>>();
    	allParams.putAll(parameters);
    	allParams.remove(0);
    	return allParams;
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
				// Valid value
				parameters.get(sName).set(0, sValue);
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
				return parameters.get(sName).get(0);
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
                    String sName = elm.getAttribute("name");
                    String sValue = elm.getAttribute("value");
                    if (sName.length()>0) { setOption(sName,sValue); }
                }
                else if (elm.getTagName().equals("parameter")) {
                    String sName = elm.getAttribute("name");
                    String[] sValues = elm.getAttribute("values").split(",");
                    if (sName.length()>0 && sValues.length>0) {
                    	List<String> values = new Vector<String>();
                    	// Set first item as default value
                    	values.add(sValues[0]);
                    	values.addAll(Arrays.asList(sValues));
                    	parameters.put(sName,values);
                    }
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
        for (String sName : parameters.keySet()) {
            Element paramNode = dom.createElement("parameter");
            paramNode.setAttribute("name",sName);
            List<String> values = parameters.get(sName);
            CSVList valueList = new CSVList(',');
            for (int i=1; i<values.size(); i++) {
            	valueList.addValue(values.get(i));
            }
            paramNode.setAttribute("values",valueList.toString());
            rootElement.appendChild(paramNode);        	
        }

        // Then simple options
        for (int i=0; i<getOptionCount(); i++) {
            Element optionNode = dom.createElement("option");
            optionNode.setAttribute("name",options[i].getName());
            optionNode.setAttribute("value",options[i].getString());
            rootElement.appendChild(optionNode);
        }
        
        // Finally complex options
        writeInner(dom);
		
        doc.setContentDOM(dom);
        doc.write(os); // may throw an IOException
    }
	
    public void write(File file) throws IOException {
    	write(new FileOutputStream(file));
    }

    /** Write configuration information to an xml document.
     *  The subclass must define this to write richer configuration data
     */
    protected abstract void writeInner(Document dom);
    
	
}

