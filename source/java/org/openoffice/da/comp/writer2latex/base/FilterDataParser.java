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
 *  Version 2.0 (2018-04-17)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.base;

import java.util.Enumeration;

import com.sun.star.beans.PropertyValue;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.XComponentContext;
import org.openoffice.da.comp.writer2latex.util.PropertyHelper;

import writer2latex.api.Converter;


/** This class parses the FilterData property passed to the filter and
 *  applies it to a <code>Converter</code>
 *  All errors are silently ignored
 */
public class FilterDataParser {
    
    private XComponentContext xComponentContext = null;
    
    public FilterDataParser(XComponentContext xComponentContext) {
        this.xComponentContext = xComponentContext;
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
        ConverterHelper converterHelper = new ConverterHelper(xComponentContext);
        Object tpl = props.get("TemplateURL");
        if (tpl!=null && AnyConverter.isString(tpl)) {
        	converterHelper.readTemplate(AnyConverter.toString(tpl), converter);
        }
        
        Object cfg = props.get("ConfigURL");
        if (cfg!=null && AnyConverter.isString(cfg)) {
            converterHelper.readConfig(AnyConverter.toString(cfg), converter.getConfig());
        }
        
        // Read further configuration properties
        Enumeration<String> keys = props.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            if (!"ConfigURL".equals(sKey) && !"TemplateURL".equals(sKey)) {
                Object value = props.get(sKey);
            	if (sKey.startsWith("param:")) { sKey = sKey.substring(6); }
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
    
}



