/************************************************************************
 *
 *  ContentEntry.java
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
 *  Version 1.2 (2010-03-15)
 *
 */

package writer2latex.api;

/** This interface represents a content entry, that is a named reference
 *  to a position within the output document.
 */
public interface ContentEntry {
	/** Get the outline level of this <code>ContentEntry</code>.
	 *  The top level is 1 (entries corresponding to indexes are considered
	 *  top level).
	 *  Note that intermediate levels may be missing (e.g. a heading of
	 *  level 3 may follow immediately after a heading of level 1).
	 * 
	 * @return the outline level
	 */
	public int getLevel();
	
	/** Get the title for this entry
	 * 
	 * @return the title
	 */
	public String getTitle();
	
	/** Get the file associated with the entry
	 * 
	 * @return the output file
	 */
	public OutputFile getFile();
	
	/** Get the name of a target within the file, if any 
	 * 
	 * @return the target name, or null if no target is needed
	 */
	public String getTarget();

}
