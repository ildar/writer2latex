/************************************************************************
 *
 *  TextStyleConverter.java
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
 *  Version 2.0 (2018-05-03)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;
import java.util.Hashtable;

import writer2latex.office.FontDeclaration;
import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.ExportNameCollection;

/**
 * This class converts OpenDocument text styles to CSS2 styles.
 * This includes conversion of text properties in other styles
 * (paragraph, cell, graphic and presentation styles).
 * <ul><li>TODO: Support CJK and CTL</li> 
 * <li>TODO: Support style:use-window-font-color ("automatic color")</li>
 * <li>TODO: Support style:font-charset (other encoding)</li>
 * <li>TODO: Support style:font-size-rel</li>
 * <li>TODO: Support text:display and text:condition</li>
 * 

 */
public class TextStyleConverter extends StyleWithPropertiesConverterHelper {

    // OpenDocument does *not* define the style for links without style name,
    // but OOo uses these styles, and so do we if they are available
    // (Caveat: OOo does not export "Visited Internet Link" until a link is actually clicked)
    private static final String DEFAULT_LINK_STYLE = "Internet link"; // Not "Link"!
    private static final String DEFAULT_VISITED_LINK_STYLE = "Visited Internet Link";

    // Bookkeeping for anchors
    private ExportNameCollection anchorStyleNames = new ExportNameCollection(true);
    private ExportNameCollection anchorVisitedStyleNames = new ExportNameCollection(true);
    private Hashtable<String, String> anchorCombinedStyleNames = new Hashtable<String, String>();
    private Hashtable<String, String> orgAnchorStyleNames = new Hashtable<String, String>();
    private Hashtable<String, String> orgAnchorVisitedStyleNames = new Hashtable<String, String>();
    
    // Use default font?
    private boolean bConvertFont = false;

    /** Create a new <code>TextStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public TextStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        this.styleMap = config.getXTextStyleMap();
        this.bConvertStyles = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.xhtmlFormatting()==XhtmlConfig.CONVERT_ALL || config.xhtmlFormatting()==XhtmlConfig.IGNORE_STYLES;
        /*StyleWithProperties defaultStyle = ofr.getDefaultParStyle();
        if (defaultStyle!=null) {
        	String sFontSize = defaultStyle.getProperty(XMLString.FO_FONT_SIZE,false);
        	if (sFontSize!=null) {
        		sBaseFontSize = sFontSize;
        	}
        }*/
        this.bConvertFont = !config.useDefaultFont();
    }

    /** Apply a link style, using a combination of two text styles
     *  @param sStyleName name of the OpenDocument style
     *  @param sVisitedStyleName name of the OpenDocument style for visited links
     *  @param info the <code>StyleInfo</code> object to add information to
     */
    public void applyAnchorStyle(String sStyleName, String sVisitedStyleName,
        StyleInfo info) {
        if (sStyleName==null || sVisitedStyleName==null) { return; }
        if (sStyleName.length()==0 || sVisitedStyleName.length()==0) { return; }
        // Look for a style map
        String sDisplayName = ofr.getTextStyles().getDisplayName(sStyleName);
        if (styleMap.contains(sDisplayName)) { // class name from config
        	XhtmlStyleMapItem map = styleMap.get(sDisplayName);
            if (!"(none)".equals(map.sCss)) {
                info.sClass = map.sCss;
            }
            return;
        }

        String sName = sStyleName+sVisitedStyleName;
        if (!anchorCombinedStyleNames.containsKey(sName)) {
            String sExportName;
            // This combination is not seen before, but the base style may be known
            // In that case, use the visited style name as well
            if (anchorStyleNames.containsName(sStyleName)) {
                sExportName = anchorStyleNames.getExportName(sStyleName)
                              +anchorVisitedStyleNames.getExportName(sVisitedStyleName);
            }
            else {
                sExportName = anchorStyleNames.getExportName(sStyleName);
            }
            anchorCombinedStyleNames.put(sName,sExportName);
            orgAnchorStyleNames.put(sExportName,sStyleName);
            orgAnchorVisitedStyleNames.put(sExportName,sVisitedStyleName);
        }
        info.sClass = anchorCombinedStyleNames.get(sName);
    }
	
    /** <p>Convert style information for used styles</p>
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuilder buf = new StringBuilder();
        buf.append(super.getStyleDeclarations(sIndent));
        if (bConvertStyles) {
            // Export anchor styles
            // Default is always the styles "Internet link" and "Visited Internet Link"(?) 
            StyleWithProperties defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:link {").append(props.toString()).append("}\n");
            }
		
            defaultLinkStyle = (StyleWithProperties)
                getStyles().getStyleByDisplayName(DEFAULT_VISITED_LINK_STYLE);
            if (defaultLinkStyle!=null) {
                CSVList props = new CSVList(";");
                cssText(defaultLinkStyle,props,true);
                cssHyperlink(defaultLinkStyle,props);
                buf.append(sIndent)
                   .append("a:visited {").append(props.toString()).append("}\n");
            }

            // Remaining link styles...
            Enumeration<String> enumer = anchorCombinedStyleNames.elements();
            while (enumer.hasMoreElements()) {
                String sExportName = enumer.nextElement();
                String sStyleName = orgAnchorStyleNames.get(sExportName);
                String sVisitedStyleName = orgAnchorVisitedStyleNames.get(sExportName);

                StyleWithProperties style = ofr.getTextStyle(sStyleName);

                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":link {").append(props.toString()).append("}\n");
                }
			
                style = ofr.getTextStyle(sVisitedStyleName);
                if (style!=null) {
                    CSVList props = new CSVList(";");
                    cssText(style,props,true);
                    cssHyperlink(style,props);
                    buf.append(sIndent).append("a.").append(sExportName)
                       .append(":visited {").append(props.toString()).append("}\n");
                }
            }
        }
        return buf.toString();

    }

    /** Get the family of text (character) styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getTextStyles();
    }
	
    /** Create default tag name to represent a text
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "span";
    }
	
    /** Convert formatting properties for a specific text style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssText(style,props,bInherit);
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // OpenDocument text properties
    // Text properties can be applied to text, paragraph, cell, graphic and
    // presentation styles.
    // Language and country attributes are handled elsewhere
    // The following attributes are currently not supported:
    //   - style:use-window-font-color ("automatic color")
    //   - style:font-charset (other encoding)
    //   - style:font-size-rel
    //   - text:display
    //   - text:condition
    //   - style:text-blinking
    // Also all attributes for CJK and CTL text are currently ignored:
    //   style:font-name-*, style:font-family-*, style:font-family-generic-*,
    //   style:font-style-name-*, style:font-pitch-*, style:font-charset-*,
    //   style:font-size-*, style:font-size-rel-*, style:script-type 
    // The following attributes cannot be supported using CSS2:
    //   - style:text-outline
    //   - style:font-relief
    //   - style:letter-kerning 
    //   - style:text-combine-*
    //   - style:text-emphasis
    //   - style:text-scale
    //   - style:text-rotation-*
    //   - fo:hyphenate
    //   - fo:hyphenation-*
    //   

    private void cssText(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssTextCommon(style,props,bInherit);
        cssTextBackground(style,props,bInherit);
        getFrameSc().cssPadding(StyleWithProperties.TEXT, style, props, bInherit);
        getFrameSc().cssBorder(StyleWithProperties.TEXT, style, props, bInherit);
        getFrameSc().cssShadow(StyleWithProperties.TEXT, style, props, bInherit);
    }
	
    public void cssTextCommon(StyleWithProperties style, CSVList props, boolean bInherit) {
        String s=null,s2=null,s3=null,s4=null;
        CSVList val;
		
        // Font family
        if (bConvertFont && (bInherit || style.getProperty(XMLString.STYLE_FONT_NAME,false)!=null)) {
            val = new CSVList(","); // multivalue property!
            // Get font family information from font declaration or from style
            s = style.getProperty(XMLString.STYLE_FONT_NAME);
            if (s!=null) {
                FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(s);
                if (fd!=null) {
                    s = fd.getFontFamily();
                    s2 = fd.getFontFamilyGeneric();
                    s3 = fd.getFontPitch();
                }
            }
            else {            
                s = style.getProperty(XMLString.FO_FONT_FAMILY);
                s2 = style.getProperty(XMLString.STYLE_FONT_FAMILY_GENERIC);
                s3 = style.getProperty(XMLString.STYLE_FONT_PITCH);
            }
   		
            // Add the western font family (CJK and CTL is more complicated)
            if (s!=null) { val.addValue(s); }
            // Add generic font family
            if ("fixed".equals(s3)) { val.addValue("monospace"); }
            else if ("roman".equals(s2)) { val.addValue("serif"); }
            else if ("swiss".equals(s2)) { val.addValue("sans-serif"); }
            else if ("modern".equals(s2)) { val.addValue("monospace"); }
            else if ("decorative".equals(s2)) { val.addValue("fantasy"); }
            else if ("script".equals(s2)) { val.addValue("cursive"); }
            else if ("system".equals(s2)) { val.addValue("serif"); } // System default font
            if (!val.isEmpty()) { props.addValue("font-family",val.toString()); }
        }
		
        // Font style (italics): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_STYLE,bInherit);
	    if (s!=null) { props.addValue("font-style",s); }
	  
        // Font variant (small caps): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_VARIANT,bInherit);
        if (s!=null) { props.addValue("font-variant",s); }
	    
        // Font weight (bold): This property fit with css2
        s = style.getProperty(XMLString.FO_FONT_WEIGHT,bInherit);
        if (s!=null) { props.addValue("font-weight",s); }
 
        // Font size: Absolute values of this property fit with css2
        // this is handled together with sub- and superscripts (style:text-position)
        // First value: sub, super or percentage (raise/lower relative to font height)
        // Second value (optional): percentage (relative size);
        if (bInherit || style.getProperty(XMLString.FO_FONT_SIZE,false)!=null
                     || style.getProperty(XMLString.STYLE_TEXT_POSITION,false)!=null) {
            s = style.getAbsoluteProperty(XMLString.FO_FONT_SIZE);
            s2 = style.getProperty(XMLString.STYLE_TEXT_POSITION);
	        if (s2!=null) {
                s2 = s2.trim();
                int i = s2.indexOf(" ");
                if (i>0) { // two values
                    s3 = s2.substring(0,i);
                    s4 = s2.substring(i+1);
                } 		
                else { // one value
                    s3 = s2; s4="100%";
                }
                if (s!=null) {
               		props.addValue("font-size",Calc.multiply(s4,scale(s)));
                }
                else {
                	props.addValue("font-size",s4);
                }
                if (!"0%".equals(s3)) {
                	props.addValue("vertical-align",s3);
                }
            }
            else if (s!=null) {
           		props.addValue("font-size",scale(s));
            }
        }

        // Color: This attribute fit with css2
        s = style.getProperty(XMLString.FO_COLOR,bInherit);
	    if (s!=null) { props.addValue("color",s); }
	  
        // Shadow: This attribute fit with css2
        // (Currently OOo has only one shadow style, which is saved as 1pt 1pt)
        s = style.getProperty(XMLString.FO_TEXT_SHADOW,bInherit);
        if (s!=null) { props.addValue("text-shadow",s); }
	  
        cssTextDecoration(style, props, bInherit);
        
        // Letter spacing: This property fit with css
        s = style.getProperty(XMLString.FO_LETTER_SPACING,bInherit);
	    if (s!=null) { props.addValue("letter-spacing",scale(s)); }
  
        // Capitalization: This property fit with css
        s = style.getProperty(XMLString.FO_TEXT_TRANSFORM,bInherit);
	    if (s!=null) { props.addValue("text-transform",s); }
    }
    
    // Convert text decoration.
    // In this case three different properties all maps to a single CSS property
    // This implies that style and color cannot be set independently (without creating
    // an additional inline element, which we don't want to do)
    // Also in CSS text-decoration cannot be turned of on child elements
    // We cannot support style:text-*-width, style:text-line-through-text and
    // style:text-line-through-text-style in CSS.
    // Also only a limited number of ODF line styles are supported in CSS
    public void cssTextDecoration(StyleWithProperties style, CSVList props, boolean bInherit) {
    	String sThrough = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,bInherit);
    	String sUnder = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,bInherit);
    	String sOver = style.getProperty(XMLString.STYLE_TEXT_OVERLINE_STYLE,bInherit);
        if (active(sThrough) || active(sUnder) || active(sOver)) {
        	// At least one decoration is active (not none)
            CSVList val = new CSVList(" ");

            // Select color from one of the decorations 
            String sColor = null;
            if (active(sThrough)) {
            	sColor = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_COLOR, bInherit);
            }
            if (sColor==null && active(sUnder)) {
            	sColor = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_COLOR, bInherit);
            }
            if (sColor==null && active(sOver)) {
                sColor = style.getProperty(XMLString.STYLE_TEXT_OVERLINE_COLOR, bInherit);            		
            }
            if (sColor!=null && !"font-color".equals(sColor)) {
            	val.addValue(sColor);
            }
            
            // Select style from one of the decorations
            String sStyle = null;
            String sType = null;
            if (active(sThrough)) {
            	sStyle = sThrough;
            	sType = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_TYPE, bInherit);
            }
            else if (active(sUnder)) {
            	sStyle = sUnder;
            	sType = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_TYPE, bInherit);
            }
            else if (active(sOver)) {
            	sStyle = sOver;
                sType = style.getProperty(XMLString.STYLE_TEXT_OVERLINE_TYPE, bInherit);            		
            }
            if (sStyle!=null) {
            	switch (sStyle) {
            	case "wave":
            		val.addValue("wavy"); break;
            	case "dash":
            	case "long-dash":
            	case "dot-dash":
            		val.addValue("dashed"); break;
            	case "dot-dot-dash":
            	case "dotted":
            		val.addValue("dotted"); break;
            	case "solid":
            	default:
            		if ("double".equals(sType)) {
            			val.addValue("double");
            		}
            		else {
            			val.addValue("solid");
            		}
            	}
            }
            
            // Select the required decorations  
            if (active(sThrough)) { val.addValue("line-through"); }
            if (active(sUnder)) { val.addValue("underline"); }
            if (active(sOver)) { val.addValue("overline"); }
            if (!val.isEmpty()) { props.addValue("text-decoration",val.toString()); }

            // Select mode
            String sMode = null;
            if (active(sThrough)) {
            	sMode = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_MODE, bInherit);
            }
            if (sMode==null && active(sUnder)) {
            	sMode = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_MODE, bInherit);
            }
            if (sMode==null && active(sOver)) {
                	sMode = style.getProperty(XMLString.STYLE_TEXT_OVERLINE_MODE, bInherit);            		
            }
            if (sMode!=null) {
            	if (sMode.equals("skip-white-space")) {
            		props.addValue("text-decoration-skip", "spaces");
            	}
            	else if (sMode.equals("continuous")) {
            		props.addValue("text-decoration-skip", "none");            		
            	}
            }
        }
        else if (sThrough!=null || sUnder!=null || sOver!=null) {
        	// At least one decoration set to none
        	props.addValue("text-decoration", "none");
        }
    }
    
    // Test whether a given decoration is active (set and not none)
    private boolean active(String sDecorationStyle) {
    	return sDecorationStyle!=null && !"none".equals(sDecorationStyle);
    }
	
    public void cssTextBackground(StyleWithProperties style, CSVList props, boolean bInherit) {
        // Background color: This attribute fit with css when applied to inline text
        String s = style.getTextProperty(XMLString.FO_BACKGROUND_COLOR,bInherit);
	    if (s!=null) { props.addValue("background-color",s); }
    }
	
    private void cssHyperlink(StyleWithProperties style, CSVList props) {
        // For hyperlinks, export text-decoration:none even if nothing is defined in source
        String s1 = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,true);
        String s2 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,true);
        String s3 = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,true);
        if (s1==null && s2==null && s3==null) {
            props.addValue("text-decoration","none");
        }
    }

}
