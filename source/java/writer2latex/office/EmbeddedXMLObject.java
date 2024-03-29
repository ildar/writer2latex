/************************************************************************
 *
 *  EmbeddedXMLObject.java
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
 *  Version 1.4 (2014-08-28)
 *
 */

package writer2latex.office;

import java.io.IOException;

import org.w3c.dom.Document;

import org.xml.sax.SAXException;

import writer2latex.util.SimpleZipReader;

/** This class represents those embedded objects in an ODF document that have an XML representation:
 *  Formulas, charts, spreadsheets, text, drawings and presentations.              
 * These object types are stored using a combination of content, settings and styles XML files.
 * The settings are application specific and ignored.
 */
public class EmbeddedXMLObject extends EmbeddedObject {
    
	// Byte entries for the XML streams of this object
	private byte[] contentBytes = null;
	private byte[] stylesBytes = null;
	
    // DOM trees representing the XML parts of this object
    protected Document contentDOM  = null;
    protected Document stylesDOM   = null;
    
    /** Read an object from an ODF package document
     *
     * @param   sName   The name of the object.
     * @param   sType   The MIME-type of the object.
     * @param   source  A ZIP reader providing the contents of the package
     */
    protected EmbeddedXMLObject(String sName, String sType, OfficeDocument doc, SimpleZipReader source) {              
        super(sName, sType, doc);
        // Read the bytes, but defer parsing until required (at that point, the bytes are nullified)
        contentBytes = source.getEntry(sName+"/"+OfficeDocument.CONTENTXML);
        stylesBytes = source.getEntry(sName+"/"+OfficeDocument.STYLESXML);
    }  
    
    /**
     * Returns the content data for this embedded object.
     *
     * @return DOM representation of "content.xml"
     *
     * @throws  SAXException    If any parser error occurs
     * @throws  IOException     If any IO error occurs
     */
    public Document getContentDOM() throws SAXException, IOException {
        if (contentDOM==null) {
            contentDOM=getDOM(contentBytes);
            contentBytes=null;
        }
        return contentDOM;
    }
    
    /**
     * Returns the style data for this embedded object.
     *
     * @return DOM representation of "styles.xml"
     *
     * @throws  SAXException    If any parser error occurs
     * @throws  IOException     If any IO error occurs
     */       
    public Document getStylesDOM() throws SAXException, IOException {
        if (stylesDOM==null) {
            stylesDOM = getDOM(stylesBytes);
            stylesBytes=null;
        }
        return stylesDOM;
    }
    
    private Document getDOM(byte[] data) throws SAXException, IOException {
    	if (data!=null) {
    		return OfficeDocument.parse(data);
    	}
    	return null;
    }
    
    public void dispose() {
    	super.dispose();
    	contentBytes = null;
    	stylesBytes = null;
    	contentDOM  = null;
        stylesDOM   = null;
    }

}