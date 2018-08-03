/************************************************************************
 *
 *  StyleMapthis.java
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
 *  Version 2.0 (2018-08-01) 
 * 
 */
 
package writer2latex.latex.util;

import java.util.HashSet;
import java.util.Set;

/** This class defines a LaTeX style map, that is a mapping from a named style to LaTeX code
 */
public class StyleMapItem {
	public static final int NONE = 0;
	public static final int LINE = 1;
	public static final int PAR = 2;
	
	// General properties
	private String sStyleName;
	private String sBefore;
	private String sAfter;
	private int nBreakAfter;
	private boolean bLineBreak;
	private boolean bVerbatim;
	
	// Properties specific for paragraph block maps
	private Set<String> next = new HashSet<>();
	private boolean bNegative = false;
	private boolean bNesting = false;
	private Set<String> include = new HashSet<>();
    
    public StyleMapItem(String sStyleName, String sBefore, String sAfter, boolean bLineBreak, int nBreakAfter, boolean bVerbatim) {
    	this.sStyleName = sStyleName;
    	this.sBefore = sBefore;
        this.sAfter = sAfter;
        this.bLineBreak = bLineBreak;
        this.nBreakAfter = nBreakAfter;
        this.bVerbatim = bVerbatim;
    }

    public StyleMapItem(String sStyleName, String sBefore, String sAfter) {
    	this(sStyleName, sBefore, sAfter, true, StyleMapItem.PAR, false);
    }
    
    public void addNext(String sStyleName) {
    	next.add(sStyleName);
    }
    
    public void setNegative(boolean bNegative) {
    	this.bNegative = bNegative;
    }
    
    public void setNesting(boolean bNesting) {
    	this.bNesting = bNesting;
    }
    
    public void addInclude(String sItem) {
    	include.add(sItem);
    }
    
    public String getStyleName() {
    	return sStyleName;
    }
    
    public String getBefore() {
        return sBefore;
    }

    public String getAfter() {
        return sAfter;
    }
    
    public boolean isNext(String sStyleName) {
        return (!bNegative && next.contains(sStyleName)) || (bNegative && !next.contains(sStyleName));
    }
    
    public boolean allowsNesting() {
    	return bNesting;
    }
    
    public boolean includes(String sItem) {
    	return include.contains(sItem);
    }
	
    public boolean getLineBreak() {
        return bLineBreak;
    }
    
    public int getBreakAfter() {
    	return nBreakAfter;
    }

    public boolean getVerbatim() {
        return bVerbatim;
    }

}
