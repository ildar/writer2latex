/************************************************************************
 *
 *  BooleanOption.java
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

// A BooleanOption interprets the values as booleans
public class BooleanOption extends Option {
    private boolean bValue;
	
    public boolean getValue() { return bValue; }
	
    public void setString(String sValue) {
        super.setString(sValue);
        bValue = "true".equals(sValue);
    }

    public BooleanOption(String sName, String sDefaultValue) {
        super(sName,sDefaultValue);
    }	
}

