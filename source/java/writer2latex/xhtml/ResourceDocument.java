/************************************************************************
 *
 *  ResourceDocument.java
 *
 *  Copyright: 2002-2015 by Henrik Just
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
 *  Version 1.5 (2015-05-05)
 *
 */
 
package writer2latex.xhtml;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import writer2latex.api.OutputFile;
import writer2latex.util.Misc;

/** An implementation of <code>OutputFile</code> for resource documents.
 *  (A resource document is an arbitrary binary file to include in the converter result)
 */
public class ResourceDocument implements OutputFile {
	
    // Content
	private String sFileName;
	private String sMediaType;
	private byte[] content;
    
    /**
     *  Constructor (creates an empty document)
     *  @param sFileName  <code>Document</code> name.
     *  @param sMediaType the media type
     */
    public ResourceDocument(String sFileName, String sMediaType) {
    	this.sFileName = sFileName;
    	this.sMediaType = sMediaType;
    	content = new byte[0];
    }

    // Implement OutputFile
    
	public String getFileName() {
		return sFileName;
	}

	public String getMIMEType() {
		return sMediaType;
	}

	public boolean isMasterDocument() {
		return false;
	}
	
	public boolean containsMath() {
		return false;
	}

	public void write(OutputStream os) throws IOException {
		os.write(content); 
	}
	
	/** Load the resource document bytes from an arbitrary input stream
	 * 
	 * @param is the input stream
	 * @throws IOException if any error occurs reading the input stream
	 */
	public void read(InputStream is) throws IOException {
		content = Misc.inputStreamToByteArray(is);
	}
    
    
}