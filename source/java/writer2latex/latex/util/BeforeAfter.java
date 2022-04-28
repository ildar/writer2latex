/************************************************************************
 *
 *  BeforeAfter.java
 *
 *  Copyright: 2002-2022 by Henrik Just
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
 *  Version 2.0 (2022-04-25)
 *
 */

package writer2latex.latex.util;

/** Utility class to hold LaTeX code to put before/after other LaTeX code 
 */
public class BeforeAfter {
    private String sBefore="";
    private String sAfter="";
    
    /** Constructor to initialize the object with a pair of strings
     *  @param sBefore1 LaTeX code to put before
     *  @param sAfter1 LaTeX code to put after  
     */
    public BeforeAfter(String sBefore1, String sAfter1) {
        sBefore=sBefore1; sAfter=sAfter1;
    }
    
    /** Default constructor: Create with empty strings
     */
    public BeforeAfter() { }

    /** <p>Add data to the <code>BeforeAfter</code></p>
     *  <p>The new data will be be added "inside", thus for example</p>
     *  <ul><li><code>add("\textsf{","}");</code>
     *  <li><code>add("\textit{","}");</code></ul>
     *  <p>will create the pair <code>\textsf{\textit{</code>, <code>}}</code></p>
     *
     *  @param sBefore1 LaTeX code to put before (null is allowed and ignored)
     *  @param sAfter1 LaTeX code to put after (null is allowed and ignored)
     */
    public void add(String sBefore1, String sAfter1) {
    	if (sBefore1!=null) { sBefore+=sBefore1; }
    	if (sAfter1!=null) { sAfter=sAfter1+sAfter; }
    }
    
    /** Add data to the <code>BeforeAfter</code>. The new data will be added "inside", and no data
     *  will be added to the "after" part
     * 
     * @param sBefore1 LaTeX code to put before (null is allowed and ignored)
     */
    public void addBefore(String sBefore1) {
        if (sBefore1!=null) { sBefore+=sBefore1; }    	
    }
    
    /** Add data to the <code>BeforeAfter</code>. The new data will be added "inside", and no data
     *  will be added to the "before" part
     * 
     * @param sAfter1 LaTeX code to put after (null is allowed and ignored)
     */
    public void addAfter(String sAfter1) {
        if (sAfter1!=null) { sAfter=sAfter1+sAfter; }
    }
    
    /** <p>Add data to the <code>BeforeAfter</code></p>
     *  <p>The new data will be be added "outside", thus for example</p>
     *  <ul><li><code>enclose("\textsf{","}");</code>
     *  <li><code>enclose("\textit{","}");</code></ul>
     *  <p>will create the pair <code>\textit{\textsf{</code>, <code>}}</code></p>
     *
     *  @param sBefore1 LaTeX code to put before (null is allowed and ignored)
     *  @param sAfter1 LaTeX code to put after   (null is allowed and ignored)
     */
    public void enclose(String sBefore1, String sAfter1) {
    	if (sBefore1!=null) { sBefore=sBefore1+sBefore; }
    	if (sAfter1!=null) { sAfter+=sAfter1; }
    }
    
    /** <p>Add the content of another <code>BeforeAfter</code> to this <code>BeforeAfter</code></p>
     *  <p>The new data will be be added "inside"</p>
     *
     *  @param ba the code to add
     */
    public void add(BeforeAfter ba) {
    	add(ba.getBefore(), ba.getAfter());
    }
	
    /** Get LaTeX code to put before
     *  @return then LaTeX code
     */
    public String getBefore() { return sBefore; }

    /** Get LaTeX code to put after
     *  @return then LaTeX code
     */
    public String getAfter() { return sAfter; }
	
    /** Check if this <code>BeforeAfter</code> contains any data
     *  @return true if there is data in at least one part
     */
    public boolean isEmpty() { return sBefore.length()==0 && sAfter.length()==0; }
   	
}