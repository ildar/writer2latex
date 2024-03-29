/************************************************************************
 *
 *  UnicodeCharacter.java
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
 *  Version 1.2 (2010-05-11) 
 * 
 */

package writer2latex.latex.i18n;

// Helper class: A struct to hold the LaTeX representations of a unicode character 
class UnicodeCharacter implements Cloneable {
    final static int NORMAL = 0;     // this is a normal character
    final static int COMBINING = 1;  // this character should be ignored
    final static int IGNORE = 2;     // this is a combining character
    final static int UNKNOWN = 3;     // this character is unknown
	
    int nType;       // The type of character
    String sMath;    // LaTeX representation in math mode 
    String sText;    // LaTeX representation in text mode
    int nFontencs;   // Valid font encoding(s) for the text mode representation
    char cProtect;   // This character is represented by this character which may produce unwanted ligatures (-, ', `)
	
    protected Object clone() {
        UnicodeCharacter uc = new UnicodeCharacter();
        uc.nType = this.nType;
        uc.sMath = this.sMath;
        uc.sText = this.sText;
        uc.nFontencs = this.nFontencs;
        uc.cProtect = this.cProtect;
        return uc;
    }
}

