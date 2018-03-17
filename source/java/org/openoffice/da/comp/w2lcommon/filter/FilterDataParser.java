/************************************************************************
 *
 *  FilterDataParser.java
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
 *  Version 2.0 (2018-03-17)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.filter;

import java.io.InputStream;
import java.io.IOException;
import java.util.Enumeration;

import com.sun.star.beans.PropertyValue;
import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.ucb.CommandAbortedException;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XStringSubstitution;

import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import writer2latex.api.Converter;


/** This class parses the FilterData property passed to the filter and
 *  applies it to a <code>Converter</code>
 *  All errors are silently ignored
 */
public class FilterDataParser {
	// TODO: Use JSON format
    
    //private static XComponentContext xComponentContext = null;
    
    private XSimpleFileAccess2 sfa2;
    private XStringSubstitution xPathSub;
    
    public FilterDataParser(XComponentContext xComponentContext) {
        //this.xComponentContext = xComponentContext;

        // Get the SimpleFileAccess service
        sfa2 = null;
        try {
            Object sfaObject = xComponentContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.ucb.SimpleFileAccess", xComponentContext);
            sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get SimpleFileAccess service (should not happen)
        }
        
        // Get the PathSubstitution service
        xPathSub = null;
        try {
            Object psObject = xComponentContext.getServiceManager().createInstanceWithContext(
               "com.sun.star.util.PathSubstitution", xComponentContext);
            xPathSub = (XStringSubstitution) UnoRuntime.queryInterface(XStringSubstitution.class, psObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get PathSubstitution service (should not happen)
        }     
    }
    
    /** Apply the given FilterOptions property to the given converter.
     *  The property must be a comma separated list of name=value items.
     * @param options an <code>Any</code> containing the FilterOptions property
     * @param converter a <code>writer2latex.api.Converter</code> implementation
     */
    public void applyFilterOptions(Object options, Converter converter) {
    	// Get the string from the data, if possible
    	if (AnyConverter.isString(options)) {
    		String sOptions = AnyConverter.toString(options);
    		if (sOptions!=null) {
	    		// Convert to array
	    		String[] sItems = sOptions.split(",");
	    		int nItemCount = sItems.length;
	        	PropertyValue[] filterData = new PropertyValue[nItemCount];
	        	for (int i=0; i<nItemCount; i++) {
	        		String[] sItem = sItems[i].split("=");
	        		filterData[i] = new PropertyValue();
	        		filterData[i].Name = sItem[0];
	        		filterData[i].Value = sItem.length>1 ? sItem[1] : "";
	        		System.out.println(filterData[i].Name+" "+filterData[i].Value);
	        	}
	        	applyParsedFilterData(filterData,converter);
    		}
    	}
    }
    
    /** Apply the given FilterData property to the given converter.
     *  The property must be an array of PropertyValue objects.
     *  @param data an <code>Any</code> containing the FilterData property
     *  @param converter a <code>writer2latex.api.Converter</code> implementation
     */
    public void applyFilterData(Object data, Converter converter) {
        // Get the array from the data, if possible
        PropertyValue[] filterData = null;
        if (AnyConverter.isArray(data)) {
            try {
                Object[] arrayData = (Object[]) AnyConverter.toArray(data);
                if (arrayData instanceof PropertyValue[]) {
                    filterData = (PropertyValue[]) arrayData;
                    if (filterData!=null) {
                    	applyParsedFilterData(filterData,converter);
                    }
                }
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to array; should not happen - ignore   
            }
        }
    }
    
    private void applyParsedFilterData(PropertyValue[] filterData, Converter converter) {
        PropertyHelper props = new PropertyHelper(filterData);
        
        // Get the special properties TemplateURL and ConfigURL
        Object tpl = props.get("TemplateURL");
        String sTemplate = null;
        if (tpl!=null && AnyConverter.isString(tpl)) {
            try {
                sTemplate = substituteVariables(AnyConverter.toString(tpl));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }
        
        Object cfg = props.get("ConfigURL");
        String sConfig = null;
        if (cfg!=null && AnyConverter.isString(cfg)) {
            try {
                sConfig = substituteVariables(AnyConverter.toString(cfg));
            }
            catch (com.sun.star.lang.IllegalArgumentException e) {
                // Failed to convert to String; should not happen - ignore   
            }
        }

        // Load the template from the specified URL, if any
        if (sfa2!=null && sTemplate!=null && sTemplate.length()>0) {
            try {
                XInputStream xIs = sfa2.openFileRead(sTemplate);
                if (xIs!=null) {
                    InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                    converter.readTemplate(is);
                    is.close();
                    xIs.closeInput();
                }
            }
            catch (IOException e) {
                // ignore
            }
            catch (NotConnectedException e) {
                // ignore
            }
            catch (CommandAbortedException e) {
                // ignore
            }
            catch (com.sun.star.uno.Exception e) {
                // ignore
            }
        }
        
        // Load the configuration from the specified URL, if any
        if (sConfig!=null) {
            if (sConfig.startsWith("*")) { // internal configuration
                try {
                    converter.getConfig().readDefaultConfig(sConfig.substring(1)); 
                }
                catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            else if (sfa2!=null) { // real URL
                try {
                    XInputStream xIs = sfa2.openFileRead(sConfig);;
                    if (xIs!=null) {
                        InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                        converter.getConfig().read(is);
                        is.close();
                        xIs.closeInput();
                    }
                }
                catch (IOException e) {
                    // Ignore
                }
                catch (NotConnectedException e) {
                    // Ignore
                }
                catch (CommandAbortedException e) {
                    // Ignore
                }
                catch (com.sun.star.uno.Exception e) {
                    // Ignore
                }
            }
        }
        
        // Read further configuration properties
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            if (!"ConfigURL".equals(sKey) && !"TemplateURL".equals(sKey)) {
                Object value = props.get(sKey);
                if (AnyConverter.isString(value)) {
                    try {
                        converter.getConfig().setOption(sKey,AnyConverter.toString(value));
                    }
                    catch (com.sun.star.lang.IllegalArgumentException e) {
                        // Failed to convert to String; should not happen - ignore   
                    }
                }
            } 
        }
    }
    
    private String substituteVariables(String sUrl) {
        if (xPathSub!=null) {
            try {
                return xPathSub.substituteVariables(sUrl, false);
            }
            catch (com.sun.star.container.NoSuchElementException e) {
                // Found an unknown variable, no substitution
                // (This will only happen if false is replaced by true above)
                return sUrl;
            }
        }
        else { // Not path substitution available
            return sUrl;
        }
    }
    	
}



