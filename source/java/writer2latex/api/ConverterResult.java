/************************************************************************
 *
 *  ConverterResult.java
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
 *  Version 2.0 (2018-03-07)
 *
 */
 
package writer2latex.api;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/** A <code>ConverterResult</code> represent a document, which is the result
 *  of a conversion performed by a <code>Converter</code>implementation.
 */
public interface ConverterResult {
    
    /** Gets an <code>Iterator</code> to access all files in the
     *  <code>ConverterResult</code>. The iterator will return the master documents first
     *  in logical order (starting with the primary master document)
     *  @return  an <code>Iterator</code> of all files
     */
    public Iterator<OutputFile> iterator();
    
    /** Get the meta data associated with the source document
     *  @return the meta data
     */
    public MetaData getMetaData();
    
    /** Get the content table (based on headings) for this <code>ConverterResult</code>
     * 
     *  @return list view of the content
     */
    public List<ContentEntry> getContent();
    
    /** Get the entry which contains the table page
     * 
     *  @return the entry or null if there is no title page
     */
    public ContentEntry getTitlePageFile();
        
    /** Get the entry which contains the start of the actual text (the first chapter, or simply the start of
     *  the document if there are no headings)
     * 
     *  @return the entry
     */
    public ContentEntry getTextFile();
        
    /** Get the entry which contains the table of contents
     * 
     *  @return the entry or null if a table of content does not exist
     */
    public ContentEntry getTocFile();
        
    /** Get the entry which contains the list of tables
     * 
     *  @return the entry or null if a list of tables does not exist
     */
    public ContentEntry getLotFile();
    
    /** Get the entry which contains the list of figures
     * 
     *  @return the entry or null if a list of figures does not exist
     */
    public ContentEntry getLofFile();
    
    /** Get the entry which contains the alphabetical index
     * 
     *  @return the entry or null if an alphabetical index does not exist
     */
    public ContentEntry getIndexFile();
    
    /** Get the entry which contains the bibliography
     * 
     *  @return the entry or null if a bibliography does not exist
     */
    public ContentEntry getBibliographyFile();
    
    /** Get the entry which contains the cover (which usually will contain a cover image)
     * 
     *  @return the entry or null if a cover does not exist
     */
    public ContentEntry getCoverFile();
    
    /** Get the entry which contains the actual cover image
     * 
     *  @return the entry or null if a cover image does not exist
     */
    public ContentEntry getCoverImageFile();
    

    
    /** Write all files of the <code>ConverterResult</code> to a directory.
     *  Subdirectories are created as required by the individual
     *  <code>OutputFile</code>s.
     *  @param dir the directory to write to (this directory must exist).
               If the parameter is null, the default directory is used
     *  @throws IOException if the directory does not exist or one or more files
     *  		could not be written
     */
    public void write(File dir) throws IOException;

}
