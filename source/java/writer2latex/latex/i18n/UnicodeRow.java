/************************************************************************
 *
 *  UnicodeRow.java
 *
 *  Copyright: 2002-2007 by Henrik Just
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
 *  Version 0.5 (2007-07-24) 
 * 
 */

package writer2latex.latex.i18n;

// Helper class: A row of 256 unicode characters
class UnicodeRow implements Cloneable {
    UnicodeCharacter[] entries;
    UnicodeRow(){ entries=new UnicodeCharacter[256]; }
	
    protected Object clone() {
        UnicodeRow ur = new UnicodeRow();
        for (int i=0; i<256; i++) {
            if (this.entries[i]!=null) {
                ur.entries[i] = (UnicodeCharacter) this.entries[i].clone();
            }
        }
        return ur;
    }
}
