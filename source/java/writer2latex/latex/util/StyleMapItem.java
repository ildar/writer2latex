/************************************************************************
 *
 *  StyleMapItem.java
 *
 *  Copyright: 2002-2011 by Henrik Just
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
 *  Version 1.2 (2011-03-30) 
 * 
 */
 
package writer2latex.latex.util;

// A struct to hold data about a style map 
public class StyleMapItem {
	public static final int NONE = 0;
	public static final int LINE = 1;
	public static final int PAR = 2;
	
    String sBefore;
    String sAfter;
    String sNext;
    int nBreakAfter;
    boolean bLineBreak;
    boolean bVerbatim;
}
