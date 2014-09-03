/************************************************************************
 *
 *  BinaryGraphicsDocument.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2014-09-03)
 *
 */

package writer2latex.base;

import java.io.OutputStream;
import java.io.IOException;

import writer2latex.api.OutputFile;
import writer2latex.util.Misc;


/** This class is used to represent a binary graphics document to be included in the converter result.
 *  I may also represent a linked image, which should <em>not</em> be included (and will produce an empty file
 *  if it is).
 */
public class BinaryGraphicsDocument implements OutputFile {

    private String sFileName;
    private String sFileExtension;
    private String sMimeType;
    
    private boolean bIsLinked = false;
    private boolean bIsAcceptedFormat = false;
    
    // Data for an embedded image
    private byte[] blob = null;
    private int nOff;
    private int nLen;
    
    // Data for a linked image
    private String sURL = null;
   
    /**Constructs a new graphics document.
     * This new document does not contain any data. Document data must 
     * be added using the appropriate methods.
     *
     * @param sName The name of the <code>GraphicsDocument</code>.
     * @param sFileExtension the file extension
     * @param sMimeType the MIME type of the document
     */
    public BinaryGraphicsDocument(String name, String sFileExtension, String sMimeType) {
        this.sFileExtension = sFileExtension;
        this.sMimeType = sMimeType;
        sFileName = Misc.trimDocumentName(name, sFileExtension);
    }
        
    /** Set image contents to a byte array
     * 
     * @param data the image data
     */
    public void setData(byte[] data, boolean bIsAcceptedFormat) {
        setData(data,0,data.length,bIsAcceptedFormat);
    }
    
    /** Set image contents to part of a byte array
     * 
     * @param data the image data
     * @param nOff the offset into the byte array
     * @param nLen the number of bytes to use
     * @param bIsAcceptedFormat flag to indicate that the format of the image is acceptable for the converter
     */
    public void setData(byte[] data, int nOff, int nLen, boolean bIsAcceptedFormat) {
        this.blob = data;
        this.nOff = nOff;
        this.nLen = nLen;
        this.bIsAcceptedFormat = bIsAcceptedFormat;
        this.bIsLinked = false;
        this.sURL = null;
    }
    
    /** Set the URL of a linked image
     * 
     * @param sURL the URL
     */
    public void setURL(String sURL) {
        this.blob = null;
        this.nOff = 0;
        this.nLen = 0;
        this.bIsAcceptedFormat = false; // or rather don't know
        this.bIsLinked = true;
    	this.sURL = sURL;
    }
    
    /** Get the URL of a linked image
     * 
     *  @return the URL or null if this is an embedded image
     */
    public String getURL() {
    	return sURL;
    }
    
    /** Does this <code>BinaryGraphicsDocument</code> represent a linked image?
     * 
     * @return true if so
     */
    public boolean isLinked() {
    	return bIsLinked;
    }
    
    /** Is this image in an acceptable format for the converter?
     * 
     * @return true if so (always returns false for linked images)
     */
    public boolean isAcceptedFormat() {
    	return bIsAcceptedFormat;
    }
    
    public byte[] getData() {
    	return blob;
    }
    
    // Implement OutputFile
    
    /** Writes out the content to the specified <code>OutputStream</code>.
     *  Linked images will not write any data.
     *
     * @param  os  <code>OutputStream</code> to write out the  content.
     *
     * @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
    	if (blob!=null) {
    		os.write(blob, nOff, nLen);
    	}
    }
    
    /** Get the file extension
     * 
     *  @return the file extension
     */
    public String getFileExtension() {
    	return sFileExtension;
    }
	
    /** Get the document with file extension.</p>
    *
    * @return  The document with file extension.
    */
   public String getFileName() {
       return sFileName + sFileExtension;
   }
   
    /** Get the MIME type of the document.
     *
     * @return  The MIME type or null if this is unknown
     */
	public String getMIMEType() {
		return sMimeType;
	}
	
    /** Is this document a master document?
     * 
     *  @return false - a graphics file is never a master document
     */
    public boolean isMasterDocument() {
		return false;
	}
}