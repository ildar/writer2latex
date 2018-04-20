/************************************************************************
 *
 *  FontDeclaration.java
 *
 *  Copyright: 2002-2018 by Henrik Just
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
 *  Version 2.0 (2018-04-18)
 *
 */

package writer2latex.office;

import org.w3c.dom.Node;

/** This class parses and represents a font face declaration in ODF.
 *  the <code>style:font-face</code> element supports a large number
 *  of attributes, but we only use <code>svg:font-family</code>,
 *  <code>style:font-family-generic</code> and <code>style:font-pitch</code> 
  */
public class FontDeclaration extends OfficeStyle {
    private String sFontFamily = null;
    private String sFirstFontFamily = null;
    private String sFontFamilyGeneric = null;
    private String sFontPitch = null;

    /** Load a font face declaration from a node, either a 
     *  <code>style:font-face</code> element or a
     *  <code>style:text-properties</code> element
     * 
     *  @param node the ODF node
     */
    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        PropertySet properties = new PropertySet();
        properties.loadFromDOM(node);
        sFontFamily = properties.getProperty(XMLString.SVG_FONT_FAMILY);
        sFontFamilyGeneric = properties.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC);
        sFontPitch = properties.getProperty(XMLString.STYLE_FONT_PITCH);
        // Get the first font family from the list
        if (sFontFamily!=null) {
        	if (sFontFamily.startsWith("'")) {
        		sFirstFontFamily = sFontFamily.substring(1, sFontFamily.indexOf('\'',1));
        	}
        	else if (sFontFamily.indexOf(',')>-1) {
        		sFirstFontFamily = sFontFamily.substring(0, sFontFamily.indexOf(','));
        	}
        	else {
        		sFirstFontFamily = sFontFamily;
        	}
        }
    }
	
    /** Get the <code>svg:font-family</code> property, which is a comma
     *  separated list of font family names
     * 
     * @return the property value, or null if the property is not set
     */
    public String getFontFamily() { return sFontFamily; }

    /** Get the first font specified in the <code>svg:font-family</code>
     *  property
     * 
     * @return the property value, or null if the property is not set
     */
    public String getFirstFontFamily() { return sFirstFontFamily; }

    /** Get the <code>style:font-family-generic</code> property
     * 
     * @return the property value, or null if the property is not set
     */
    public String getFontFamilyGeneric() { return sFontFamilyGeneric; }

    /** Get the <code>style:font-pitch</code> property
     * 
     * @return the property value, or null if the property is not set
     */
    public String getFontPitch() { return sFontPitch; }

}
