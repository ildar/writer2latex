/************************************************************************
 *
 *  CssDocument.java
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
 *  Version 1.6 (2015-05-05)
 *
 */
 
package writer2latex.xhtml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import writer2latex.api.OutputFile;

/**
 *  An implementation of <code>OutputFile</code> for CSS documents.
 *  (Actually this is a trivial implementation which never parses the files)
 */
public class CssDocument implements OutputFile {
	
    // Content
	private String sName;
	private String sContent;
    
    /**
     *  Constructor (creates an empty document)
     *  @param  sName  <code>Document</code> name.
     */
    public CssDocument(String sName) {
    	this.sName = sName;
    	sContent = "";
    }

	public String getFileName() {
		return sName;
	}

	public String getMIMEType() {
		return "text/css";
	}

	public boolean isMasterDocument() {
		return false;
	}
	
	public boolean containsMath() {
		return false;
	}

	public void write(OutputStream os) throws IOException {
        OutputStreamWriter osw = new OutputStreamWriter(os,"UTF-8");
        osw.write(sContent);
        osw.flush();
        osw.close();
	}
	
	public void read(InputStream is) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is,"UTF-8"));
		StringBuilder buf = new StringBuilder();
		String sLine;
		while ((sLine=reader.readLine())!=null) {
			buf.append(sLine).append('\n');
		}
		sContent = buf.toString();
	}
	
	public void read(String s) {
		sContent = s;
	}
    
    
}



        




