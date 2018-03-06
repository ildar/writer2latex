/************************************************************************
 *
 *  MetaData.java
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
 *  Version 1.2 (2010-12-15)
 *
 */

package writer2latex.api;

import java.util.Map;

/** This interface provides access to the predefined meta data of the
 *  source document (currently incomplete)
 */
public interface MetaData {
	/** Get the title of the source document
	 * 
	 * @return the title (may return an empty string)
	 */
	public String getTitle();
	
	/** Get the subject of the source document
	 * 
	 * @return the subject (may return an empty string)
	 */
	public String getSubject();
	
	/** Get the keywords of the source document
	 * 
	 * @return the keywords as a comma separated list (may return an empty string)
	 */
	public String getKeywords();
	
	/** Get the description of the source document
	 * 
	 * @return the description (may return an empty string)
	 */
	public String getDescription();
	
	/** Get the creator of the source document (or the initial creator if none is specified)
	 * 
	 * @return the creator (may return an empty string)
	 */
	public String getCreator();

	/** Get the (main) language of the document
	 * 
	 * @return the language
	 */
	public String getLanguage();

	/** Get the date of the source document
	 * 
	 * @return the date (may return an empty string)
	 */
	public String getDate();
	
	/** Get the user-defined meta data
	 * 
	 * @return the user-defined meta data as a name-value map
	 */
	public Map<String,String> getUserDefinedMetaData();
}
