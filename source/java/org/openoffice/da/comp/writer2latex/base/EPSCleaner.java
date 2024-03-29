/************************************************************************
 *
 *  EPSCleaner.java
 *
 *  Copyright: 2002-2009 by Henrik Just
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
 *  Version 1.0 (2009-03-09)
 */
 
package org.openoffice.da.comp.writer2latex.base;

/** This class removes redundant binary information from EPS files created by OOo.
 *  See the issue http://qa.openoffice.org/issues/show_bug.cgi?id=25256
 *  According to this message http://markmail.org/message/dc6rprmtktxuq35v
 *  on dev@openoffice.org the binary data is an EPSI preview in TIFF format
 *  TODO: Is it possible to avoid this export? 
 */
public class EPSCleaner {

    // Signatures for start and end in eps
    private byte[] psStart;
    private byte[] psEnd;
    
    public EPSCleaner() {
        try {
            psStart = "%!PS-Adobe".getBytes("US-ASCII");
            psEnd = "%%EOF".getBytes("US-ASCII");
        }
        catch (java.io.UnsupportedEncodingException ex) {
            // US-ASCII *is* supported :-)
        }
            
    }
	
    // 
    public byte[] cleanEps(byte[] blob) {
        int n = blob.length;

        int nStart = 0;
        for (int i=0; i<n; i++) {
            if (match(blob,psStart,i)) {
                nStart=i;
                break;
            }
        }

        int nEnd = n;
        for (int i=nStart; i<n; i++) {
            if (match(blob,psEnd,i)) {
                nEnd=i+psEnd.length;
                break;
            }
        }
		
        byte[] newBlob = new byte[nEnd-nStart];
        System.arraycopy(blob,nStart,newBlob,0,nEnd-nStart);
        return newBlob;        
    }
	
    private boolean match(byte[] blob, byte[] sig, int nStart) {
        int n = sig.length;
        if (nStart+n>=blob.length) { return false; }
        for (int i=0; i<n; i++) {
            if (blob[nStart+i]!=sig[i]) { return false; }
        }
        return true;
    }


}

