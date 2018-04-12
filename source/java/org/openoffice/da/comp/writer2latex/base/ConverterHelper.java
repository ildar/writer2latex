/************************************************************************
 *
 *  ConverterHelper.java
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
 *  Version 2.0 (2018-03-21)
 *
 */

package org.openoffice.da.comp.writer2latex.base;

import java.io.IOException;
import java.io.InputStream;

import org.openoffice.da.comp.writer2latex.util.MacroExpander;

import writer2latex.api.Config;

import com.sun.star.io.NotConnectedException;
import com.sun.star.io.XInputStream;
import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.ucb.CommandAbortedException;
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
	MacroExpander expander;

	
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
    
    
    public void readConfig(Config config, String sURL) {
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
            	// First get the real URL by expanding macros and substituting
            	// variables
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
