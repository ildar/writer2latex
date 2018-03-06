/************************************************************************
 *
 *  BatchHandler.java
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

/** This is a call back interface to handle user interaction during a
 *  batch conversion with a {@link BatchConverter}
 */
public interface BatchHandler {
	
    /** Notification that the conversion is started */
    public void startConversion();
	
    /** Notification that the conversion has finished */
    public void endConversion();
	
    /** Notification that a directory conversion starts
     * 
     *  @param sName the name of the directory to convert
     */
    public void startDirectory(String sName);
	
    /** Notification that a directory conversion has finished
     * 
     *  @param sName the name of the directory
     *  @param bSuccess true if the conversion was successful (this only means
     *  that the index page was created with success, not that the conversion
     *  of files and subdirectories was successful)
     */
    public void endDirectory(String sName, boolean bSuccess);
	
    /** Notification that a file conversion starts
     * 
     *  @param sName the name of the file to convert
     */
    public void startFile(String sName);
	
    /** Notification that a file conversion has finished
     * 
     *  @param sName the name of the file
     *  @param bSuccess true if the conversion of this file was successful
     */
    public void endFile(String sName, boolean bSuccess);
	
    /** Notification that the conversion may be cancelled. The
     *  {@link BatchConverter} fires this event once per document.
     *  Cancelling the conversion does not delete files that was already
     *  converted
     *  
     *  @return true if the handler wants to cancel the conversion
     */
    public boolean cancel();

}
