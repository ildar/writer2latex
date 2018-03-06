/************************************************************************
 *
 *  ConverterResultImpl.java
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
 *  Version 1.2 (2010-03-24)
 *
 */ 

package writer2latex.base;

import writer2latex.api.ContentEntry;
import writer2latex.api.OutputFile;

public class ContentEntryImpl implements ContentEntry {
	private String sTitle;
	private int nLevel;
	private OutputFile file;
	private String sTarget;
	
	public ContentEntryImpl(String sTitle, int nLevel, OutputFile file, String sTarget) {
		this.sTitle = sTitle;
		this.nLevel = nLevel;
		this.file = file;
		this.sTarget = sTarget;
	}
	
	public String getTitle() {
		return sTitle;
	}
	
	public int getLevel() {
		return nLevel;
	}
	
	public OutputFile getFile() {
		return file;
	}
	
	public String getTarget() {
		return sTarget;
	}
}
