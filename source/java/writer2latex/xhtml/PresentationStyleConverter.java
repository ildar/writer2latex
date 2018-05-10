/************************************************************************
 *
 *  PresentationStyleConverter.java
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
 *  Version 2.0 (2018-05-01)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;
import java.util.Hashtable;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
//import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;

/**
 * This class converts OpenDocument presentation styles to CSS2 styles.
 * Presentation styles are special frame styles, used to style the standard
 * elements in a presentation (title, subtitle and textbox)
 */
public class PresentationStyleConverter extends FrameStyleConverter {

    // Data about outline styles
    String sCurrentOutlineStyle = null;
    Hashtable<String, String[]> outlineStyles = new Hashtable<String, String[]>();
    ExportNameCollection outlineStyleNames = new ExportNameCollection(true);

    /** Create a new <code>PresentationStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public PresentationStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        // style maps for presentations are currently not supported:
        this.styleMap = new XhtmlStyleMap();
    }

    /** Return a prefix to be used in generated css class names
     *  @return the prefix
     */
    public String getClassNamePrefix() { return "prsnt"; }

    /** Get the family of presentation styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getPresentationStyles();
    }
	
    /** Create default tag name to represent a presentation object
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "div";
    }
	
    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
            StringBuilder buf = new StringBuilder();
            buf.append(super.getStyleDeclarations(sIndent));
            Enumeration<String> names = outlineStyleNames.keys();
            while (names.hasMoreElements()) {
                String sDisplayName = names.nextElement();
                StyleWithProperties style = (StyleWithProperties)
                    getStyles().getStyleByDisplayName(sDisplayName);
                if (!style.isAutomatic()) {
                    // Apply style to paragraphs within a list item with this class
                    CSVList props = new CSVList(";");
                    getFrameSc().cssMargin(StyleWithProperties.GRAPHIC,style,props,true);
                    getParSc().cssPar(style,props,true);
                    getTextSc().cssTextBlock(style,props,true);
                    if (!props.isEmpty()) {
                        buf.append(sIndent)
                           .append("li.outline")
                           .append(styleNames.getExportName(sDisplayName))
                           .append(" p {").append(props.toString()).append("}")
                           .append(config.prettyPrint() ? "\n" : " ");
                    }
                }
            }
            return buf.toString();
        }
        else {
            return "";
        }
    }

	
    public void enterOutline(String sStyleName) {
        sCurrentOutlineStyle = sStyleName;
        if (!outlineStyles.containsKey(sCurrentOutlineStyle)) {
            String[] sNames = new String[10];
            outlineStyles.put(sCurrentOutlineStyle,sNames);
            StyleWithProperties style1 = ofr.getPresentationStyle(sCurrentOutlineStyle);
            if (style1!=null) {
                String sCurrentOutlineStyle1 = sCurrentOutlineStyle;
                if (style1.isAutomatic()) { sCurrentOutlineStyle1 = style1.getParentName(); }
                sNames[1] = sCurrentOutlineStyle1;
                String sBaseName = sCurrentOutlineStyle1.substring(0,sCurrentOutlineStyle1.length()-1);
                for (int i=2; i<10; i++) {
                    String sName = sBaseName + Integer.toString(i);
                    StyleWithProperties style = ofr.getPresentationStyle(sName);
                    if (style!=null && style.getParentName().equals(sNames[i-1])) {
                        sNames[i] = sName;
                    }
                    else {
                        break;
                    }
                }
                sNames[1] = null;
            }
        }
    }
	
    public void exitOutline() {
        sCurrentOutlineStyle = null;
    }
	
    public void applyOutlineStyle(int nLevel, StyleInfo info) {
        if (2<=nLevel && nLevel<=9 && sCurrentOutlineStyle!=null) {
            if (outlineStyles.containsKey(sCurrentOutlineStyle)) {
                info.sClass = "outline"+outlineStyleNames.getExportName(outlineStyles.get(sCurrentOutlineStyle)[nLevel]);
            }
        }
    }
	
    
	
}
