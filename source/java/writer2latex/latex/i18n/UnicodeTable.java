/************************************************************************
 *
 *  UnicodeTable.java
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

// Helper class: Table of up to 65536 unicode characters
class UnicodeTable {
    protected UnicodeRow[] table=new UnicodeRow[256];
    private UnicodeTable parent;
    
    // Constructor; creates a new table, possibly based on a parent
    // Note: The parent must be fully loaded before the child is created.
    public UnicodeTable(UnicodeTable parent){
        this.parent = parent;
        if (parent!=null) {
            // *Copy* the rows from the parent
            for (int i=0; i<256; i++) {
                table[i] = parent.table[i];
            }
        }
    }

    // Make sure the required entry exists
    private void createEntry(int nRow, int nCol) {
        if (table[nRow]==null) {
            table[nRow]=new UnicodeRow();
        }
        else if (parent!=null && table[nRow]==parent.table[nRow]) {
            // Before changing a row it must be *cloned*
            table[nRow] = (UnicodeRow) parent.table[nRow].clone();
        }
        if (table[nRow].entries[nCol]==null) {
            table[nRow].entries[nCol]=new UnicodeCharacter();
        }
    }

    // Addd a single character (type only), by number
    protected void addCharType(char c, int nType) {
        int nRow=c/256; int nCol=c%256;
        createEntry(nRow,nCol);
        table[nRow].entries[nCol].nType = nType;
    }

    // Addd a single character (type only), by name
    protected void addCharType(char c, String sType) {
        int nRow=c/256; int nCol=c%256;
        createEntry(nRow,nCol);
        if ("combining".equals(sType)) {
            table[nRow].entries[nCol].nType = UnicodeCharacter.COMBINING;
        }
        else if ("ignore".equals(sType)) {
            table[nRow].entries[nCol].nType = UnicodeCharacter.IGNORE;
        }
        else {
            table[nRow].entries[nCol].nType = UnicodeCharacter.NORMAL;
        }
    }

    // Add a single math character to the table
    protected void addMathChar(char c, String sLaTeX){
        int nRow=c/256; int nCol=c%256;
        createEntry(nRow,nCol);
        table[nRow].entries[nCol].sMath=sLaTeX;
    }
    
    // Add a single text character to the table
    protected void addTextChar(char c, String sLaTeX, int nFontencs, char cProtect){
        int nRow=c/256; int nCol=c%256;
        createEntry(nRow,nCol);
        table[nRow].entries[nCol].sText=sLaTeX;
        table[nRow].entries[nCol].nFontencs=nFontencs;
        table[nRow].entries[nCol].cProtect=cProtect;
    }

    // Retrieve entry for a character (or null)
    private UnicodeCharacter getEntry(char c) {
        int nRow=c/256; int nCol=c%256;
        if (table[nRow]==null) return null;
        return table[nRow].entries[nCol];
    }

    // Get character type
    public int getCharType(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return UnicodeCharacter.UNKNOWN;
        return entry.nType;
    }
	
    // Check to see if this math character exists?
    public boolean hasMathChar(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return false;
        return entry.sMath!=null;
    }
	
    // Get math character (or null)
    public String getMathChar(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return null;
        return entry.sMath;
    }
	
    // Check to see if this text character exists?
    public boolean hasTextChar(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return false;
        return entry.sText!=null;
    }
	
    // Get text character (or null)
    public String getTextChar(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return null;
        return entry.sText;
    }
	
    // Get font encoding(s) for text character (or 0)
    public int getFontencs(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return 0;
        return entry.nFontencs;
    }
	
    // Get ligature protect character for text character 
    public char getProtectChar(char c) {
        UnicodeCharacter entry = getEntry(c);
        if (entry==null) return '\u0000';
        return entry.cProtect;
    }
	
    // Get number of defined characters
    public int getCharCount() {
        int nCount = 0;
        for (int nRow=0; nRow<256; nRow++) {
            if (table[nRow]!=null) {
                for (int nCol=0; nCol<256; nCol++) {
                    UnicodeCharacter entry = table[nRow].entries[nCol];
                    if (entry!=null) nCount++;
                }
            }
        }
        return nCount;
    }
	
}

