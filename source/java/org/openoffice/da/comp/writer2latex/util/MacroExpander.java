/************************************************************************
 *
 *  MacroExpander.java
 *
 *  Copyright: 2002-2010 by Henrik Just
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
 *  Version 1.2 (2010-03-12)
 *
 */ 

package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XMacroExpander;

public class MacroExpander {
	
	private XMacroExpander xExpander; 
	
	/** Convenience wrapper class for the UNO Macro Expander singleton
	 * 
	 *  @param xContext the UNO component context from which "theMacroExpander" can be created
	 */
	public MacroExpander(XComponentContext xContext) {
        Object expander = xContext.getValueByName("/singletons/com.sun.star.util.theMacroExpander");
        xExpander = (XMacroExpander) UnoRuntime.queryInterface (XMacroExpander.class, expander);
	}
	
	/** Expand macros in a string
	 * 
	 * @param s the string
	 * @return the expanded string
	 */
    public String expandMacros(String s) {
        if (xExpander!=null && s.startsWith("vnd.sun.star.expand:")) {
            // The string contains a macro, usually as a result of using %origin% in the registry
            s = s.substring(20);
            try {
                return xExpander.expandMacros(s);
            }
            catch (IllegalArgumentException e) {
                // Unknown macro name found, proceed and hope for the best
                return s;
            }
        }
        else {
            return s;
        }
    }
	
}
