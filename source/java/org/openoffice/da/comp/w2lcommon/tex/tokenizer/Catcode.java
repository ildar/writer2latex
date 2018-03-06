/************************************************************************
 *
 *  Catcode.java
 *
 *  Copyright: 2002-2009 by Henrik Just
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
 *  Version 1.2 (2009-06-11)
 *
 */

package org.openoffice.da.comp.w2lcommon.tex.tokenizer;

/** This enumerates TeX category codes (catcodes) for characters as defined in
 *  chapter 7 of "The TeXbook" 
 */
public enum Catcode {
	ESCAPE,
	BEGIN_GROUP,
	END_GROUP,
	MATH_SHIFT,
	ALIGNMENT_TAB,
	END_OF_LINE,
	PARAMETER,
	SUPERSCRIPT,
	SUBSCRIPT,
	IGNORED,
	SPACE,
	LETTER,
	OTHER,
	ACTIVE,
	COMMENT,
	INVALID;
}
