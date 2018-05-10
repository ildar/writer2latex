/************************************************************************
 *
 *  StyleConverterHelper.java
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
 *  Version 2.0 (2018-04-02)
 *
 */

package writer2latex.xhtml;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.ExportNameCollection;

/**
 * <p>This is an abstract base class to convert an OpenDocument style family to
 * CSS2 styles.</p>
 */
public abstract class StyleConverterHelper extends ConverterHelper {

    // Translation of OpenDocument style names to CSS class names
    protected ExportNameCollection styleNames = new ExportNameCollection(true);
	
    // Style map to use
    protected XhtmlStyleMap styleMap;

    // Should we convert styles resp. hard formatting?
    protected boolean bConvertStyles = true;
    protected boolean bConvertHard = true;
	
    /** Create a new <code>StyleConverterHelper</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public StyleConverterHelper(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
    }

    /** Apply the writing direction (ltr or rtl) attribute from a style
     *  @param style the OpenDocument style to use
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    protected static void applyDirection(StyleWithProperties style, StyleInfo info) {
        String sDir = style.getProperty(XMLString.STYLE_WRITING_MODE);
        if ("lr-tb".equals(sDir)) { info.sDir="ltr"; }
        else if ("rl-tb".equals(sDir)) { info.sDir="rtl"; }
    }

    /** Apply language+country from a style
     *  @param style the OpenDocument style to use
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    protected static void applyLang(StyleWithProperties style, StyleInfo info) {
        // Language, country and script are defined by the following attributes
        // For western languages: fo:language, fo:country, fo:script and style:rfc-language-tag
        // For asian languages: style:language-asian, style:country-asian, style:script-asian, style:rfc-language-tag-asian
        // Finally for compex languages: style:language-complex, style:country-complex, style:script-complex, style:script-type and
        // style:rfc-language-tag-complex
        // Currently we handle only fo:language and fo:country and 
        String sLang = style.getTextProperty(XMLString.FO_LANGUAGE,true);
        String sCountry = style.getTextProperty(XMLString.FO_COUNTRY,true);
        if (sLang!=null) {
            if (sCountry==null || sCountry.equals("none")) { info.sLang = sLang; }
            else { info.sLang = sLang+"-"+sCountry; }
        }
    }

    /** Get the OpenDocument style family associated with this
     *  StyleConverterHelper
     *  @return the style family
     */
    public abstract OfficeStyleFamily getStyles();

    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public abstract String getStyleDeclarations(String sIndent);	

}
