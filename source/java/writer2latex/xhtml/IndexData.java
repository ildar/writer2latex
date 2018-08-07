/************************************************************************
 *
 *	IndexData.java
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
 *  Version 2.0 (2018-08-06)
 *
 */

package writer2latex.xhtml;

import org.w3c.dom.Element;

/** This class holds data about an index, which should be populated later
 */
class IndexData {
	// Source data
	int nChapterNumber; // The chapter number containing this index
	Element onode; // The index source in the ODF document
	
	// Target data
	int nOutFileIndex; // The outfile containing this index
	Element hnode; // The container to hold the generated index

}