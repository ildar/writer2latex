/************************************************************************
 *
 *  StyleWithPropertiesConverterHelper.java
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
 *  Version 2.0 (2018-08-23)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;

import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.util.CSVList;

/**
 * <p>This is an abstract class to convert an OpenDocument style family
 * represented by <code>StyleWithProperties</code> to CSS2 styles.</p>
 */
public abstract class StyleWithPropertiesConverterHelper
    extends StyleConverterHelper {

    /** Create a new <code>StyleWithPropertiesConverterHelper</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public StyleWithPropertiesConverterHelper(OfficeReader ofr, XhtmlConfig config,
        Converter converter) {
        super(ofr,config,converter);
    }

    /** Apply a style, either by converting the style or by applying the
     *  style map from the configuarion
     *  @param sStyleName name of the OpenDocument style
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    public void applyStyle(String sStyleName, StyleInfo info) {
        StyleWithProperties style = (StyleWithProperties) getStyles().getStyle(sStyleName);
        info.sTagName = getDefaultTagName(style);
        if (style!=null) {
            if (config.multilingual()) {
            	applyLang(style,info);
            	applyDirection(style,info);
            }
            if (style.isAutomatic()) {
                // Apply parent style + hard formatting
                applyStyle(style.getParentName(),info);
                if (bConvertHard) { applyProperties(style,info.props,false); }
            }
            else {
                String sDisplayName = style.getDisplayName();
                if (styleMap.contains(sDisplayName)) {
                    // Apply attributes as specified in style map from user
                	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
                	if (map.sElement.length()>0) {
                		info.sTagName = map.sElement;
                	}
                    if (!"(none)".equals(map.sCss)) {
                        info.sClass = map.sCss;
                    }
                }
                else {
                    // Generate class name from display name
                    info.sClass = getClassNamePrefix()
                                  + styleNames.getExportName(sDisplayName);
                }
            }
        }
    }
	
    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
            StringBuilder buf = new StringBuilder();
            Enumeration<String> names = styleNames.keys();
            while (names.hasMoreElements()) {
                String sDisplayName = names.nextElement();
                StyleWithProperties style = (StyleWithProperties)
                    getStyles().getStyleByDisplayName(sDisplayName);
                if (!style.isAutomatic()) {
                    CSVList props = new CSVList(";");
                    applyProperties(style,props,true);
                    buf.append(sIndent);
                    buf.append(getDefaultTagName(null));
                    buf.append(".");
                    buf.append(getClassNamePrefix());
                    buf.append(styleNames.getExportName(sDisplayName));
                    buf.append(" {");
                    buf.append(props.toString());
                    buf.append("}");
                    buf.append(config.prettyPrint() ? "\n" : " ");
                    // TODO: Create a method "getStyleDeclarationsInner"
                    // to be used by eg. FrameStyleConverter
                }
            }
            return buf.toString();
        }
        else {
            return "";
        }
    }
	
    /** Return a prefix to be used in generated css class names
     *  @return the prefix
     */
    public String getClassNamePrefix() { return ""; }

    /** Create default tag name to represent a specific style, e.g.
     *  <code>span</code> (text style) or <code>ul</code> (unordered list)
     *  @param style to use
     *  @return the tag name. If the style is null, a default result should be
     *  returned.
     */
    public abstract String getDefaultTagName(StyleWithProperties style);
	
    /** Convert formatting properties for a specific style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public abstract void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit);
	

}
