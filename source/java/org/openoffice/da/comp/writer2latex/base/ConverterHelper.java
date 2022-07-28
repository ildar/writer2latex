/************************************************************************
 *
 *  ConverterHelper.java
 *
 *  Copyright: 2002-2022 by Henrik Just
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
 *  Version 2.0 (2022-07-28)
 *
 */

package org.openoffice.da.comp.writer2latex.base;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.openoffice.da.comp.writer2latex.util.MacroExpander;

import writer2latex.api.Config;

import com.sun.star.io.XInputStream;
import com.sun.star.io.XOutputStream;
import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.adapter.XOutputStreamToOutputStreamAdapter;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XStringSubstitution;

/** This is a helper class to aid in reading and writing configurations
 *  and templates from/to and URL.
 */
public class ConverterHelper {
	
    // Simple file access service to load and save files
    private XSimpleFileAccess2 sfa2;
    
    // Path substitution service to substitute paths like $(user)
    private XStringSubstitution xPathSub;
    
    // Macro expander to expand macros like %origin%
	private MacroExpander expander;

	
    /** Construct a new <code>ConverterHelper</code> with a given context.
     * 
     * @param xContext the Office context
     */
    public ConverterHelper(XComponentContext xContext) {
        // Get the SimpleFileAccess service
        sfa2 = null;
        try {
            Object sfaObject = xContext.getServiceManager().createInstanceWithContext(
                "com.sun.star.ucb.SimpleFileAccess", xContext);
            sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get SimpleFileAccess service (should not happen)
        }
        
        // Get the PathSubstitution service
        xPathSub = null;
        try {
            Object psObject = xContext.getServiceManager().createInstanceWithContext(
               "com.sun.star.util.PathSubstitution", xContext);
            xPathSub = (XStringSubstitution) UnoRuntime.queryInterface(XStringSubstitution.class, psObject);
        }
        catch (com.sun.star.uno.Exception e) {
            // failed to get PathSubstitution service (should not happen)
        }     
        
        // Get the macro expander
        expander = new MacroExpander(xContext);
    }
    
    
    /** Read a configuration from an URL. Macros in the URL will be expanded
     *  and variables in the URL will be substituted.
     * 
     * @param sURL the URL
     * @param config the Writer2LaTeX configuration to read from the URL
     */
    public void readConfig(String sURL, Config config) {
        if (sURL!=null) {
            if (sURL.startsWith("*")) {
            	// internal configuration
                try {
                    config.readDefaultConfig(sURL.substring(1)); 
                }
                catch (IllegalArgumentException e) {
                    // ignore
                }
            }
            else if (sfa2!=null) {
            	// First get the real URL by expanding macros and substituting variables
                String sRealURL = expander.expandMacros(substituteVariables(sURL));

                try {
                    XInputStream xIs = sfa2.openFileRead(sRealURL);
                    if (xIs!=null) {
                        InputStream is = new XInputStreamToInputStreamAdapter(xIs);
                        config.read(is);
                        is.close();
                        xIs.closeInput();
                    }
                }
                catch (IOException | com.sun.star.uno.Exception e) {
                    // Ignore
                }
            }
        }
    }
    
    /** Read a configuration from an URL. Macros in the URL will be expanded
     *  and variables in the URL will be substituted.
     * 
     * @param sURL the URL
     * @param config the Writer2LaTeX configuration to write to the URL
     */
	public void writeConfig(String sURL, Config config) {
		if (sURL!=null && sfa2!=null) {
			try {
            	// First get the real URL by expanding macros and substituting variables
                String sRealURL = expander.expandMacros(substituteVariables(sURL));

                //Remove the file if it exists
	           	if (sfa2.exists(sRealURL)) {
	           		sfa2.kill(sRealURL);
	           	}
	           	
	           	// Then write the new contents
	            XOutputStream xOs = sfa2.openFileWrite(sRealURL);
	            if (xOs!=null) {
	            	OutputStream os = new XOutputStreamToOutputStreamAdapter(xOs);
	                config.write(os);
	                os.close();
	                xOs.closeOutput();
	            }
	        }
	        catch (IOException | com.sun.star.uno.Exception e) {
	            // ignore
	        }
	    }
	}

    private String substituteVariables(String sURL) {
        if (xPathSub!=null) {
            try {
                return xPathSub.substituteVariables(sURL, false);
            }
            catch (com.sun.star.container.NoSuchElementException e) {
                // Found an unknown variable, no substitution
                // (This will only happen if false is replaced by true above)
                return sURL;
            }
        }
        else { // No path substitution available
            return sURL;
        }
    }

}
