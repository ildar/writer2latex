/************************************************************************
 *
 *  CellStyleConverter.java
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
 *  Version 2.0 (2018-03-08)
 *
 */

package writer2latex.xhtml;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

/**
 * This class converts OpenDocument cell styles to CSS2 styles.
 * Cells are formatted using box properties and alignment.
 */
public class CellStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>CellStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public CellStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        // Style maps for Cells are currently not supported.
        // (In OOo, cell styles are only supported by Calc) 
        this.styleMap = new XhtmlStyleMap();
        this.bConvertStyles = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlTableFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlTableFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Get the family of cell styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getCellStyles();
    }
	
    /** Create default tag name to represent a Cell object
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "td";
    }
	
    /** Convert formatting properties for a specific Cell style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        // Apply "inner" box properties (no margins)
        getFrameSc().cssBorder(style,props,bInherit);
        getFrameSc().cssPadding(style,props,bInherit);
        getFrameSc().cssBackground(style,props,bInherit);
        // only relevant for spreadsheets
        getParSc().cssPar(style,props,bInherit); 
        getTextSc().cssTextCommon(style,props,bInherit);
        // Cell-specific properties (vertical alignment)
        cssCell(style,props,bInherit);
    }
	
    private void cssCell(StyleWithProperties style, CSVList props, boolean bInherit){
        // Vertical align: Some values fit with css
        String s = ofr.isOpenDocument() ? 
            style.getProperty(XMLString.STYLE_VERTICAL_ALIGN,bInherit) :
            style.getProperty(XMLString.FO_VERTICAL_ALIGN,bInherit);
        if ("middle".equals(s)) { props.addValue("vertical-align","middle"); }
        else if ("bottom".equals(s)) { props.addValue("vertical-align","bottom"); }
        else if ("top".equals(s)) { props.addValue("vertical-align","top"); }
        else {
            // No value or "automatic" means, according to the spec,
            //"The application decide how to align the text."
            // We treat this case like OOo does:
            props.addValue("vertical-align", ofr.isSpreadsheet() ? "bottom" : "top");
        }
    }

}
