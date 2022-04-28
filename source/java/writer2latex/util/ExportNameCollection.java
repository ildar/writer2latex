/************************************************************************
 *
 *  ExportNameCollection.java
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
 *  Version 2.0 (2022-04-26)
 *
 */

package writer2latex.util;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.HashMap;

/** Maintain a collection of export names. 
 *  This is used to map named collections to simpler names (only A-Z, a-z and 0-9, and possibly additional characters)
 */
public class ExportNameCollection{
    private Map<String, String> exportNames = new HashMap<String, String>();
    private String sPrefix;
    private String sAdditionalChars;
    private boolean bAcceptNumbers;
    
    public ExportNameCollection(String sPrefix, boolean bAcceptNumbers, String sAdditionalChars) {
        this.sPrefix=sPrefix;
        this.bAcceptNumbers = bAcceptNumbers;
        this.sAdditionalChars = sAdditionalChars;
    }
    
    public ExportNameCollection(String sPrefix, boolean bAcceptNumbers) {
    	this(sPrefix,bAcceptNumbers,"");
    }
	
    public ExportNameCollection(boolean bAcceptNumbers) {
        this("",bAcceptNumbers,"");
    }
	
    public Enumeration<String> keys() {
        return Collections.enumeration(exportNames.keySet());
    }
    
    public void addName(String sName){
        if (containsName(sName)) { return; }
        StringBuilder outbuf=new StringBuilder();
        // _20_ will usually represent a space in ODF-documents created by LibreOffice; we get rid of that
        SimpleInputBuffer inbuf=new SimpleInputBuffer(sName.replaceAll("_20_", ""));
		
        // Don't start with a digit
        if (bAcceptNumbers && inbuf.peekChar()>='0' && inbuf.peekChar()<='9') {
            outbuf.append('a');
        }

        char c;
        // convert numbers to roman numbers and discard unwanted characters
        while ((c=inbuf.peekChar())!='\0'){
            if ((c>='a' && c<='z') || (c>='A' && c<='Z')) {
                outbuf.append(inbuf.getChar());
            }
            else if (c>='0' && c<='9'){
                if (bAcceptNumbers) {
                    outbuf.append(inbuf.getInteger());
                }
                else {
                    outbuf.append(Misc.int2roman(
                                  Integer.parseInt(inbuf.getInteger())));
                }
            }
            else if (sAdditionalChars.indexOf(c)>-1) {
            	outbuf.append(inbuf.getChar());
            }
            else {
                inbuf.getChar(); // ignore this character
            }
        }
        String sExportName=outbuf.toString();
        if (sExportName.length()==0) {
        	// Do not accept empty export names
        	sExportName = "qwerty";
        }
        if (!exportNames.containsValue(sExportName)) {
        	// Everything's fine, we can use the stripped name directly
        	exportNames.put(sName,sExportName);
        }
        else {
        	// Otherwise add letters at the end until a unique export name is found
        	int i=1;
        	while (true) {
        		String sSuffix = Misc.int2alph(i++, false);
        		if (!exportNames.containsValue(sExportName+sSuffix)) {
        			exportNames.put(sName,sExportName+sSuffix);
        			break;
        		}
        	}
        }
    }
    
    public String getExportName(String sName) {
        // add the name, if it does not exist
        if (!containsName(sName)) { addName(sName); }
        return sPrefix + exportNames.get(sName);
    }

    public boolean containsName(String sName) {
        return exportNames.containsKey(sName);
    }
    
    public boolean isEmpty() {
        return exportNames.size()==0;
    }
}
