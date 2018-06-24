/************************************************************************
 *
 *  TableStyleConverter.java
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
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

/**
 * This class converts OpenDocument table styles to CSS styles.
 * Table formatting includes <em>background</em>, <em>shadow</em>, <em>alignment</em>,
 * <em>margins</em>. Table <em>width</em> is handled elsewhere.
 */
public class TableStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>TableStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public TableStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        // Style maps for tables are currently not supported.
        this.styleMap = new XhtmlStyleMap();
        this.bConvertStyles = config.tableFormatting()==XhtmlConfig.CONVERT_ALL || config.tableFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.tableFormatting()==XhtmlConfig.CONVERT_ALL || config.tableFormatting()==XhtmlConfig.IGNORE_STYLES;
    }

    /** Get the family of table styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getTableStyles();
    }
	
    /** Create default tag name to represent a table object
     *  @param style to use
     *  @return the tag name
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "table";
    }
	
    /** Convert formatting properties for a specific table style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
    	// Page break
    	getParSc().cssPageBreak(style, props, bInherit);
        // Apply background and shadow
        getFrameSc().cssBackground(StyleWithProperties.TABLE,style,props,bInherit);
        getFrameSc().cssShadow(StyleWithProperties.TABLE,style,props,bInherit);
        // Table-specific properties (this includes handling of margins)
        cssTable(style,props,bInherit);
    }
	
    private void cssTable(StyleWithProperties style, CSVList props, boolean bInherit){
        // Top and bottom margins
        String sMarginTop = style.getAbsoluteProperty(XMLString.FO_MARGIN_TOP);
        if (sMarginTop!=null) { props.addValue("margin-top",scale(sMarginTop)); }
        else { props.addValue("margin-top","0"); }

        String sMarginBottom = style.getAbsoluteProperty(XMLString.FO_MARGIN_BOTTOM);
	    if (sMarginBottom!=null) { props.addValue("margin-bottom",scale(sMarginBottom)); }
        else { props.addValue("margin-bottom","0"); }

        // Left and right margins and horizontal alignment
        String sAlign = style.getProperty(XMLString.TABLE_ALIGN);
        String sMarginLeft = style.getAbsoluteProperty(XMLString.FO_MARGIN_LEFT);
        if (sMarginLeft!=null) { sMarginLeft = scale(sMarginLeft); }
        String sMarginRight = style.getAbsoluteProperty(XMLString.FO_MARGIN_RIGHT);
        if (sMarginRight!=null) { sMarginRight = scale(sMarginRight); }

        if ("center".equals(sAlign)) {
		    sMarginLeft = "auto"; sMarginRight = "auto";
        }
        else if ("right".equals(sAlign)) {
		    sMarginLeft = "auto";
        }
        else if ("left".equals(sAlign)) {
		    sMarginRight = "auto";
        }

        if (sMarginLeft!=null) { props.addValue("margin-left",sMarginLeft); }		
        if (sMarginRight!=null) { props.addValue("margin-right",sMarginRight); }		
    }
	
}
