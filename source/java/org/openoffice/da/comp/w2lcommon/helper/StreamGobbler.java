/************************************************************************
 *
 *  StreamGobbler.java
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
 *  Version 1.6 (2015-04-05)
 *
 */ 
 
package org.openoffice.da.comp.w2lcommon.helper;

import java.io.*;

public class StreamGobbler extends Thread {
    InputStream is;
    String type;
    
    public StreamGobbler(InputStream is, String type) {
        this.is = is;
        this.type = type;
    }
    
    public void run() {
        try  {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            //String line=null;
            //while ( (line = br.readLine()) != null) {
            while ( br.readLine() != null) {
                // Do nothing...
            }    
        }
        catch (IOException ioe) {
            ioe.printStackTrace();  
        }
    }
}

