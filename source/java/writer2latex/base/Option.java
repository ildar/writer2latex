/************************************************************************
 *
 *  Option.java
 *
 *  Copyright: 2002-2008 by Henrik Just
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
 *  Version 1.0 (2008-09-08)
 *
 */

package writer2latex.base;

// The mother of all options; reads and writes string values 
public class Option {
    protected String sValue;
    private String sName;
	
    public void setString(String sValue) { this.sValue = sValue; }

    public String getString() { return sValue; }
	
    public String getName() { return sName; }
	
    public Option(String sName, String sDefaultValue) {
       this.sName = sName;
       setString(sDefaultValue);
    }	
} 

