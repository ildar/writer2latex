/************************************************************************
 *
 *  CSVList.java
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
 *  Version 2.0 (2018-06-09)
 *
 */

package writer2latex.util;

// Create a list of values separated by commas or another seperation character 
public class CSVList{
    private String sSep;
    private String sNameValueSep;
    private boolean bEmpty = true;
    private StringBuilder buf = new StringBuilder();
	
    public CSVList(String sSep, String sNameValueSep) {
        this.sSep=sSep;
        this.sNameValueSep=sNameValueSep;
    }
	
    public CSVList(String sSep) {
        this(sSep,":");
    }
    
    public CSVList(char cSep) {
        this(Character.toString(cSep),":");
    }

    public void addValue(String sVal){
        if (sVal==null) { return; }
        if (bEmpty) { bEmpty=false; } else { buf.append(sSep); }
        buf.append(sVal);
    }

    public void addValue(String sName, String sVal) {
        if (sName==null) { return; }
        if (bEmpty) { bEmpty=false; } else { buf.append(sSep); }
        buf.append(sName).append(sNameValueSep).append(sVal);
    }
    
    // temp. hack
    public void addValues(CSVList list) {
    	if (!list.isEmpty()) {
    		if (!bEmpty) { buf.append(sSep); }
    		buf.append(list.toString());
    		bEmpty=false;
    	}
    }
	
    public String toString() {
        return buf.toString();
    }
	
    public boolean isEmpty() {
        return bEmpty;
    }
	
}
