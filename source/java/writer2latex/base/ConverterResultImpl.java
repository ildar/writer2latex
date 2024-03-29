/************************************************************************
 *
 *  ConverterResultImpl.java
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
 *  Version 2.0 (2018-03-10)
 *
 */ 

package writer2latex.base;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Vector;
import java.util.Iterator;

import writer2latex.api.ConverterResult;
import writer2latex.api.OutputFile;

/** <code>ConverterResultImpl</code> is a straightforward implementation of <code>ConverterResult</code>
 */
public class ConverterResultImpl implements ConverterResult {
	
	private List<OutputFile> files;
	
	private int nMasterCount;
	
	/** Construct a new <code>ConverterResultImpl</code> with empty content
	 */
	public ConverterResultImpl() {
		reset();
	}
	
    /** Resets all data.  This empties all <code>OutputFile</code> and <code>ContentEntry</code> objects
     *  objects from this class.  This allows reuse of a <code>ConvertResult</code> object.
     */
    public void reset() {
        files = new Vector<OutputFile>();
        nMasterCount = 0;
	}

    /** Adds an <code>OutputFile</code> to the list
     *
     *  @param  file  The <code>OutputFile</code> to add.
     */
    public void addDocument(OutputFile file) {
    	if (file.isMasterDocument()) {
    		files.add(nMasterCount++, file);
    	}
    	else {
    		files.add(file);
    	}
	}
		
    /**
     *  Gets an <code>Iterator</code> to access the <code>List</code>
     *  of <code>OutputFile</code> objects
     *
     *  @return  The <code>Iterator</code> to access the
     *           <code>List</code> of <code>OutputFile</code> objects.
     */
    public Iterator<OutputFile> iterator() {
        return files.iterator();
	}

    /** Write all files to a given directory
     * 
     *  @param dir the directory to use
     */
    public void write(File dir) throws IOException {
        if (dir!=null && !dir.exists()) throw new IOException("Directory does not exist");
        Iterator<OutputFile> docEnum = iterator();
        while (docEnum.hasNext()) {
            OutputFile docOut = docEnum.next();
            String sDirName = "";
            String sFileName = docOut.getFileName();
            File subdir = dir;
            int nSlash = sFileName.indexOf("/");
            if (nSlash>-1) {
                sDirName = sFileName.substring(0,nSlash);
                sFileName = sFileName.substring(nSlash+1);
                subdir = new File(dir,sDirName);
                if (!subdir.exists()) { subdir.mkdir(); }
            }
            File outfile = new File (subdir,sFileName);
            FileOutputStream fos = new FileOutputStream(outfile);
            docOut.write(fos);
            fos.flush();
            fos.close();
        }

    }
}

