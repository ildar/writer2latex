/************************************************************************
 *
 *  EmbeddedObject.java
 *
 *  Copyright: 2002-2014 by Henrik Just
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
 *  Version 1.4 (2014-08-27)
 *
 */

package writer2latex.office;

/** This class represents and embedded object within an ODF package document
 */
public abstract class EmbeddedObject {
	private OfficeDocument doc;
    private String sName;
    private String sType;
    
    /** Construct a new embedded object
     *
     * @param   sName    The name of the object.
     * @param   sType    The MIME-type of the object.
     * @param   doc      The document to which the object belongs.
     */
    protected EmbeddedObject(String sName, String sType, OfficeDocument doc) {
        this.sName = sName;
        this.sType = sType;
        this.doc = doc;
    }
    
    /** Get the name of the embedded object represented by this instance.
     *  The name refers to the manifest.xml file
     *
     * @return  The name of the object.
     */
    public final String getName() {
        return sName;
    }
    
    /** Get the MIME type of the embedded object represented by this instance.
     *  The MIME type refers to the manifest.xml file
     */
    public final String getType() {
        return sType;
    }
    
    /** Dispose this <code>EmbeddedObject</code>. This implies that the content is nullified and the object
     *  is removed from the collection in the <code>OfficeDocument</code>.
     * 
     */
    public void dispose() {
    	doc.removeEmbeddedObject(sName);
    }
    
}