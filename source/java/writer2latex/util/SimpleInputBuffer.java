/************************************************************************
 *
 *  SimpleInputBuffer.java
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
 *  Version 2.0 (2018-07-24)
 *
 */

package writer2latex.util;

/** This class provides a simple string input buffer; it can be used as the
 *  basis of a tokenizer. 
 */
public class SimpleInputBuffer {
    
    private String sContent;
    private int nIndex, nLen;
    
    /*private static boolean isEndOrLineEnd(char cChar) {
        switch (cChar){
            case '\0':
            case '\n':
            case '\r':
                return true;
            default:
                return false;
        }
    }*/
    
    private static boolean isDigit(char cChar) {
        return cChar>='0' && cChar<='9';
    }
	
    private static boolean isDigitOrDot(char cChar) {
        return isDigit(cChar) || cChar=='.';
    }
	
    private static boolean isDigitOrDotOrComma(char cChar) {
        return isDigitOrDot(cChar) || cChar==',';
    }

    public SimpleInputBuffer(String sContent) {
        this.sContent=sContent;
        nLen=sContent.length();
        nIndex=0;
    }
    
    public int getIndex() { return nIndex; }
	
    public boolean atEnd() {
        return nIndex>=nLen;
    }
    
    public char peekChar() {
        return nIndex<nLen ? sContent.charAt(nIndex) : '\0';
    }
    
    public char peekFollowingChar() {
        return nIndex+1<nLen ? sContent.charAt(nIndex+1) : '\0';
    }
    
    public char getChar() {
        return nIndex<nLen ? sContent.charAt(nIndex++) : '\0';
    }
    
    public String getText() {
        int nStart=nIndex;
        while (nIndex<nLen && Character.isLetter(sContent.charAt(nIndex))) 
            nIndex++;
        return sContent.substring(nStart,nIndex);    	
    }
    
    public String getIdentifier() {
        int nStart=nIndex;
        while (nIndex<nLen && (Character.isLetter(sContent.charAt(nIndex)) ||
               isDigitOrDot(sContent.charAt(nIndex))))
            nIndex++;
        return sContent.substring(nStart,nIndex);
    }
    
    public String getNumber() {
        int nStart=nIndex;
        while (nIndex<nLen && isDigitOrDotOrComma(sContent.charAt(nIndex)))
            nIndex++;
        return sContent.substring(nStart,nIndex);
    }
    
    public String getInteger() {
        int nStart=nIndex;
        while (nIndex<nLen && sContent.charAt(nIndex)>='0' && sContent.charAt(nIndex)<='9'){
            nIndex++;
        }
        return sContent.substring(nStart,nIndex);
    }
    
    public String getSignedDouble() {
    	boolean bHasNumber = false;
    	int nStart=nIndex;
    	if (nIndex<nLen && (sContent.charAt(nIndex)=='+' || sContent.charAt(nIndex)=='-')) {
    		nIndex++;
    	}
    	while (nIndex<nLen && isDigit(sContent.charAt(nIndex))) {
    		nIndex++;
    		bHasNumber = true;
    	}
    	if (nIndex<nLen && sContent.charAt(nIndex)=='.') {
    		nIndex++;
    	}
    	while (nIndex<nLen && isDigit(sContent.charAt(nIndex))) {
    		nIndex++;
    		bHasNumber = true;
    	}
    	if (bHasNumber) {
    		// Check for optional exponent
    		boolean bHasExponent = false;
    		int nExpStart = nIndex;
    		if (nIndex<nLen && (sContent.charAt(nIndex)=='E' || sContent.charAt(nIndex)=='e')) {
    			nIndex++;
            	if (nIndex<nLen && (sContent.charAt(nIndex)=='+' || sContent.charAt(nIndex)=='-')) {
            		nIndex++;
            	}
            	while (nIndex<nLen && isDigit(sContent.charAt(nIndex))) {
            		nIndex++;
            		bHasExponent = true;
            	}
    		}
        	if (!bHasExponent) { // No exponent, back up
        		nIndex = nExpStart;
        	}
    		return sContent.substring(nStart, nIndex);
    	}
    	else { // Failed to parse a signed float
    		nIndex = nStart;
    		return "";
    	}
    }
	
    public void skipSpaces() {
        while (nIndex<nLen && sContent.charAt(nIndex)==' ') { nIndex++; }
    }

    public void skipSpacesAndTabs() {
        while (nIndex<nLen && (sContent.charAt(nIndex)==' ' || sContent.charAt(nIndex)=='\u0009')) { nIndex++; }
    }
}
