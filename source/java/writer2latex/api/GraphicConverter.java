/************************************************************************
 *
 *  GraphicConverter.java
 *
 *  Copyright: 2002-2008 by Henrik Just
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
 *  Version 1.0 (2008-11-23) 
 *
 */
 
package writer2latex.api;

/** A simple interface for a graphic converter which converts between various
 *  graphics formats 
 */
public interface GraphicConverter {
    
    /** Check whether a certain conversion is supported by the converter
     * 
     *  @param sSourceMime a string containing the source Mime type
     *  @param sTargetMime a string containing the target Mime type
     *  @param bCrop true if the target graphic should be cropped
     *  @param bResize true if the target graphic should be resized
     *  (the last two parameters are for future use)
     *  @return true if the conversion is supported 
     */
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize);
	
    /** Convert a graphics file from one format to another
     * 
     *  @param source a byte array containing the source graphic
     *  @param sSourceMime a string containing the Mime type of the source
     *  @param sTargetMime a string containing the desired Mime type of the target
     *  @return a byte array containing the converted graphic. Returns null
     *  if the conversion failed. 
     */
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime);

}



