/************************************************************************
 *
 *  Token.java
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
 *  Version 1.2 (2010-10-25)
 *
 */

package org.openoffice.da.comp.writer2latex.latex.tokenizer;

/** This class represent a token in TeX
 */
public class Token implements Cloneable {
	private TokenType type;
	private char[] tokenChars;
	private int nTokenLen;
	private int nCapacity;
	
	/** Construct a new <code>Token</code>, initialized as a <code>TokenTYPE.ENDINPUT</code>-token
	 */
	public Token() {
		type = TokenType.ENDINPUT;
		tokenChars = new char[25];
		nCapacity = 25;
		nTokenLen = 0;
	}
	
	/** Set the type of this token to a specific <code>TokenType</code>
	 *  (the character content is not changed)
	 * 
	 * @param type the new <code>TokenType</code>
	 */
	protected void setType(TokenType type) {
		this.type = type;
	}
	
	/** Set the character content of this token to a single character
	 *  (the type of the token is not changed)
	 * 
	 * @param c the character
	 */
	protected void setChar(char c) {
		tokenChars[0] = c;
		nTokenLen = 1;
	}
	
	/** Set this token as a character token with a specific <code>TokenType</code>
	 * 
	 * @param c the character
	 * @param type the <code>TokenType</code> to use
	 */
	protected void set(char c, TokenType type) {
		setType(type);
		setChar(c);
	}
	
	/** Delete the character content of this token
	 */
	protected void clearChars() {
		nTokenLen = 0;
	}
	
	/** Append a character to the character content of this token
	 * 
	 *  @param c the character to be appended
	 */
	protected void addChar(char c) {
		if (nTokenLen == nCapacity) {
			char[] temp = tokenChars;
			nCapacity+=25;
			tokenChars = new char[nCapacity];
			System.arraycopy(temp, 0, tokenChars, 0, temp.length);
		}
		tokenChars[nTokenLen++] = c;
	}
	
	/** Test wether this token is a character token of the given type (that is, a single character
	 *  with a token type that is neither <code>COMMAND_SEQUENCE</code> nor <code>ENDINPUT</code>) 
	 * 
	 * @param c the character to test
	 * @param type the <code>TokenType</code> to test
	 * @return true if the test was successful
	 */
	public boolean is(char c, TokenType type) {
		return this.type==type && type!=TokenType.COMMAND_SEQUENCE && type!=TokenType.ENDINPUT &&
			nTokenLen==1 && tokenChars[0]==c;
	}
	
	/** Test wether this token is a <code>COMMAND_SEQUENCE</code> token with a given name
	 * 
	 * @param sName the name of the command sequence
	 * @return true if the test was successful
	 */
	public boolean isCS(String sName) {
		if (type==TokenType.COMMAND_SEQUENCE && sName.length()==nTokenLen) {
			for (int i=0; i<nTokenLen; i++) {
				if (sName.charAt(i)!=tokenChars[i]) { return false; }
			}
			return true;
		}
		return false;
	}
	
	/** Get the <code>TokenType</code> of this token
	 * 
	 * @return the type
	 */
	public TokenType getType() {
		return type;
	}
	
	/** Get the first character in this token
	 * 
	 * @return the character or U+FFFF is no characters exist
	 */
	public char getChar() {
		return nTokenLen>0 ? tokenChars[0] : '\uFFFF';
	}
	
	/** Get the character content of this token as a string
	 * 
	 * @return the character content
	 */
	public String getString() {
		return new String(tokenChars,0,nTokenLen);
	}
	
	@Override public String toString() {
		switch (type) {
		case COMMAND_SEQUENCE:
			return "\\"+getString();
		case ENDINPUT:
			return "<EOF>";
		default:
			return Character.toString(getChar());
		}
	}
	
	@Override public Object clone() {
		Token newToken = new Token();
		newToken.type = this.type;
		newToken.nTokenLen = this.nTokenLen;
		newToken.nCapacity = this.nCapacity;
		newToken.tokenChars = new char[newToken.nCapacity];
		System.arraycopy(this.tokenChars, 0, newToken.tokenChars, 0, newToken.nCapacity);
		return newToken;
	}
	
}
