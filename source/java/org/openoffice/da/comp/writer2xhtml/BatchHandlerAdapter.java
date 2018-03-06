/************************************************************************
 *
 *  BatchHandlerAdapter.java
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
 *  Version 1.0 (2008-10-05) 
 *
 */
 
package org.openoffice.da.comp.writer2xhtml;

import writer2latex.api.BatchHandler;
import org.openoffice.da.writer2xhtml.XBatchHandler;

/** The uno interface provides an XBatchHandler implementation, the java
 *  interface requires a BatchHandler implementation. This simple class
 *  implements the latter using an instance of the former.
 */
public class BatchHandlerAdapter implements BatchHandler {

    private XBatchHandler unoHandler;

    public BatchHandlerAdapter(XBatchHandler unoHandler) {
        this.unoHandler = unoHandler;
    }
    
    public void startConversion() {
        unoHandler.startConversion();
    }
	
    public void endConversion() {
        unoHandler.endConversion();
    }
	
    public void startDirectory(String sName) {
        unoHandler.startDirectory(sName);
    }
	
    public void endDirectory(String sName, boolean bSuccess) {
        unoHandler.endDirectory(sName, bSuccess);
    }
	
    public void startFile(String sName) {
        unoHandler.startFile(sName);
    }
	
    public void endFile(String sName, boolean bSuccess) {
        unoHandler.endFile(sName, bSuccess);
    }
	
    public boolean cancel() {
        return unoHandler.cancel();
    }

}
