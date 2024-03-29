/************************************************************************
 *
 *  LoftReader.java
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
 *  Version 1.0 (2008-11-22)
 *
 */

package writer2latex.office;

//import java.util.Hashtable;
//import java.util.Set;

import org.w3c.dom.Element;
//import org.w3c.dom.Node;

import writer2latex.util.Misc;

/**
 *  <p>The class reads a <code>text:illustration-index</code> or
 *  <code>text:table-index</code> element.</p>
 */
public class LoftReader {

    private Element loftSource = null;
    private Element indexBody = null;
	
    private String sName = null;                 // (section) name for this lof/lot
    private String sStyleName = null;            // section style name

    private boolean bUseCaption = true;
    private String sCaptionSequenceName = null;
    private boolean bIsByChapter = false;        // default is document
	
    private Element indexTitleTemplate = null;
    private Element loftEntryTemplate = null;

    private boolean bIsTableIndex;
	

    /** <p>Initialize the LoftReader with a illustration/table index node
     *  @param onode a <code>text:*-index</code>
     */
    public LoftReader(Element onode) {
        bIsTableIndex = onode.getTagName().equals(XMLString.TEXT_TABLE_INDEX);

        sName = Misc.getAttribute(onode,XMLString.TEXT_NAME);
        sStyleName = Misc.getAttribute(onode,XMLString.TEXT_STYLE_NAME);

        loftSource = bIsTableIndex ?
            Misc.getChildByTagName(onode,XMLString.TEXT_TABLE_INDEX_SOURCE) :
            Misc.getChildByTagName(onode,XMLString.TEXT_ILLUSTRATION_INDEX_SOURCE);

        indexBody = Misc.getChildByTagName(onode,XMLString.TEXT_INDEX_BODY);

        if (loftSource!=null) {
            bUseCaption = !"false".equals(loftSource.getAttribute(XMLString.TEXT_USE_CAPTION));
            sCaptionSequenceName = loftSource.getAttribute(XMLString.TEXT_CAPTION_SEQUENCE_NAME);
            bIsByChapter = "chapter".equals(loftSource.getAttribute(XMLString.TEXT_INDEX_SCOPE));
            
            indexTitleTemplate = Misc.getChildByTagName(loftSource,XMLString.TEXT_INDEX_TITLE_TEMPLATE);
            loftEntryTemplate = bIsTableIndex ?
                Misc.getChildByTagName(loftSource,XMLString.TEXT_TABLE_INDEX_ENTRY_TEMPLATE) :
                Misc.getChildByTagName(loftSource,XMLString.TEXT_ILLUSTRATION_INDEX_ENTRY_TEMPLATE);
        }
    }
	
    /** <p>Get the (section) name for this loft </p>
     *  @return the name of the loft
     */
    public String getName() { return sName; }
	
    /** <p>Get the (section) style name for this loft </p>
     *  @return name of the section style to use for this loft
     */
    public String getStyleName() { return sStyleName; }
	
    /** <p>Is this a table index or a figure index? </p>
     *  @return true if it's a table index
     */
    public boolean isTableIndex() { return bIsTableIndex; }
	
    /** <p>Is this loft by chapter? </p>
     *  @return true if the scope is a chapter only
     */
    public boolean isByChapter() { return bIsByChapter; }
	
    /** <p>Is this loft generated by captions? (otherwise: by object names)</p>
     *  @return true if we use captions
     */
    public boolean useCaption() { return bUseCaption; }
	
    /** <p>Get the sequence name to use for the caption</p>
     *  @return the name of the caption
     */
    public String getCaptionSequenceName() { return sCaptionSequenceName; }
	
    /** <p>Get the index title template for this loft</p>
     *  @return the <code>text:index-title-template</code> element, or null
     */
    public Element getIndexTitleTemplate() { return indexTitleTemplate; }
	
    /** <p>Get the entry template for this loft at a specific level</p>
     *  @param nLevel the outline level
     *  @return the <code>text:table-of-content-entry-template</code> element, or null
     */
    public Element getLoftEntryTemplate(int nLevel) {
        return loftEntryTemplate;
    }
	
    /** <p>Return the generated content of this loft, if available</p>
     *  @return the <code>text:index-body</code> element
     */
    public Element getIndexBody() { return indexBody; }

	
}