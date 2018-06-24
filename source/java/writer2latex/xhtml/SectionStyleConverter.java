/************************************************************************
 *
 *  SectionStyleConverter.java
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
 *  Version 2.0 (2018-05-02)
 *
 */

package writer2latex.xhtml;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
//import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

/**
 * This class converts OpenDocument section styles to CSS2 styles.
 * Sections are formatted using (a subset of) box properties and with columns.
 * The latter would require css3 to be converted (column-count property)
 */
public class SectionStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>SectionStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public SectionStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        // Style maps for sections are currently not supported.
        // (Section styles are not supported by OOo yet) 
        this.styleMap = new XhtmlStyleMap();
        this.bConvertStyles = config.sectionFormatting()==XhtmlConfig.CONVERT_ALL
            || config.sectionFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.sectionFormatting()==XhtmlConfig.CONVERT_ALL
            || config.sectionFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Get the family of section styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getSectionStyles();
    }
	
    /** <p>Create default tag name to represent a section object</p>
     *  @param style to use
     *  @return the tag name. If the style is null, a default result should be
     *  returned.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "div";
    }
	
    /** <p>Convert formatting properties for a specific section style.</p>
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        getFrameSc().cssMargin(StyleWithProperties.SECTION,style,props,bInherit);
        getFrameSc().cssBackground(StyleWithProperties.SECTION,style,props,bInherit);
    }

}
