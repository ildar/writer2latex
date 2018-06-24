/************************************************************************
 *
 *  FrameStyleConverter.java
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
 *  Version 2.0 (2018-05-04)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;

import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;

/** <p>This class converts OpenDocument graphic (frame) styles to CSS styles. Frame styles defines the usual box properties,
 * background, padding, border, shadow and margin.
 * These properties are contained in the graphic-properties element.</p>
 * <p>Frame properties are contained in a number of other style properties too, however:</p>
 * <ul>
 *  <li>graphic-properties, header-footer-properties, page-layout-properties and paragraph-properties supports all box properties</li>
 *  <li>table-properties supports background, shadow and margin</li>
 *  <li>table-row-properties support background</li> 
 *  <li>table-cell-properties supports background, padding, border and shadow</li>
 *  <li>section-properties supports background, left margin and right margin</li>
 *  <li>text-properties support background color</li>
 * </ul>
 * <p>Thus this class provides methods to convert individual properties for other styles.</p>
 * <p>Also, ODF provides two representations for background, border and shadow. One for draw elements and one for text elements.
 * The former is always contained in the graphic-properties element and takes precedence.</p>  
 */
public class FrameStyleConverter extends StyleWithPropertiesConverterHelper {

    /** Create a new <code>FrameStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public FrameStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        this.styleMap = config.getXFrameStyleMap();
        this.bConvertStyles = config.frameFormatting()==XhtmlConfig.CONVERT_ALL || config.frameFormatting()==XhtmlConfig.IGNORE_HARD;
        this.bConvertHard = config.frameFormatting()==XhtmlConfig.CONVERT_ALL || config.frameFormatting()==XhtmlConfig.IGNORE_STYLES;
    }
	
    /** Convert style information for used graphic styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        if (bConvertStyles) {
            StringBuilder buf = new StringBuilder();
            buf.append(super.getStyleDeclarations(sIndent));
            Enumeration<String> names = styleNames.keys();
            while (names.hasMoreElements()) {
                String sDisplayName = names.nextElement();
                StyleWithProperties style = (StyleWithProperties)
                    getStyles().getStyleByDisplayName(sDisplayName);
                if (!style.isAutomatic()) {
                    // Apply style to paragraphs contained in this frame
                    CSVList props = new CSVList(";");
                    cssMargin(StyleWithProperties.GRAPHIC,style,props,true);
                    getParSc().cssPar(style,props,true);
                    getTextSc().cssTextBlock(style,props,true);
                    if (!props.isEmpty()) {
                        buf.append(sIndent)
                           .append(getDefaultTagName(null))
                           .append(".").append(getClassNamePrefix())
                           .append(styleNames.getExportName(sDisplayName))
                           .append(" p {").append(props.toString()).append("}").append(config.prettyPrint() ? "\n" : " ");
                    }
                }
            }
            return buf.toString();
        }
        else {
            return "";
        }
    }

    /** Return a prefix to be used in generated CSS class names
     *  @return the prefix
     */
    public String getClassNamePrefix() { return "frame"; }

    /** Get the family of frame styles
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getFrameStyles();
    }
	
    /** Create default tag name to represent a frame
     *  @param style to use
     *  @return the tag name.
     */
    public String getDefaultTagName(StyleWithProperties style) {
        return "";
    }
	
    /** Convert formatting properties for a specific frame style.
     *  @param style the style to convert
     *  @param props the <code>CSVList</code> object to add information to
     *  @param bInherit true if properties should be inherited from parent style(s)
     */
    public void applyProperties(StyleWithProperties style, CSVList props, boolean bInherit) {
        cssBox(StyleWithProperties.GRAPHIC,style,props,bInherit);
        getTextSc().cssTextBlock(style,props,bInherit); // only in presentations
    }
	
    ////////////////////////////////////////////////////////////////////////////
    // Conversion of individual OpenDocument frame properties

    /** Convert all box properties (background, padding, border, shadow, margins).
     * @param nType the style properties type (a constant from {@link StyleWithProperties}). We need this because box properties
     * unlike most style properties can be contained in several property elements.
     * @param style the style from which to read the properties
     * @param props the property to which CSS properties are added
     * @param bInherit true if properties should be inherited from parent style(s)
     */
    public void cssBox(int nType, StyleWithProperties style, CSVList props, boolean bInherit){
        cssBackground(nType,style,props,bInherit);    	
        cssPadding(nType,style,props,bInherit);
        cssBorder(nType,style,props,bInherit);
        cssShadow(nType,style,props,bInherit);
		cssMargin(nType,style,props,bInherit);
    }
    	
    // Margins and paddings. These are supported by the CSS compatible properties fo:margin, fo:margin-*
    // and fo:padding and fo:padding-* (percentage values have different interpretations in ODF and CSS, though)
    
    public void cssMargin(int nType,StyleWithProperties style, CSVList props, boolean bInherit){
    	cssSpacing(nType,style,props,bInherit,
    			XMLString.FO_MARGIN,XMLString.FO_MARGIN_TOP,XMLString.FO_MARGIN_RIGHT,XMLString.FO_MARGIN_BOTTOM,XMLString.FO_MARGIN_LEFT,
    			"margin","margin-top","margin-right","margin-bottom","margin-left");
    }
    
    public void cssPadding(int nType,StyleWithProperties style, CSVList props, boolean bInherit){
    	cssSpacing(nType,style,props,bInherit,
    			XMLString.FO_PADDING,XMLString.FO_PADDING_TOP,XMLString.FO_PADDING_RIGHT,XMLString.FO_PADDING_BOTTOM,XMLString.FO_PADDING_LEFT,
    			"padding","padding-top","padding-right","padding-bottom","padding-left");
    }
    
    private void cssSpacing(int nType,StyleWithProperties style, CSVList props, boolean bInherit,
    		String sXML, String sXMLTop, String sXMLRight, String sXMLBottom, String sXMLLeft,
    		String sCSS, String sCSSTop, String sCSSRight, String sCSSBottom, String sCSSLeft){
    	// First try main property to set all four sides
    	if (!cssSingleSpacing(nType,style,props,bInherit,sXML,sCSS)) {
    		// Then individual properties for each side
	        boolean bTop = cssSingleSpacing(nType,style,props,bInherit,sXMLTop,sCSSTop);
	        boolean bRight = cssSingleSpacing(nType,style,props,bInherit,sXMLRight,sCSSRight);
	        boolean bBottom = cssSingleSpacing(nType,style,props,bInherit,sXMLBottom,sCSSBottom);
	        boolean bLeft = cssSingleSpacing(nType,style,props,bInherit,sXMLLeft,sCSSLeft);
	        // And finally, if we inherit from parent we always set the spacing
	        if (bInherit) {
	            if (!bTop && !bRight && !bBottom && !bLeft) {
	                props.addValue(sCSS,"0");
	            }
	            else {
	                if (!bTop) { props.addValue(sCSSTop,"0"); }
	                if (!bRight) { props.addValue(sCSSRight,"0"); }
	                if (!bBottom) { props.addValue(sCSSBottom,"0"); }
	                if (!bLeft) { props.addValue(sCSSLeft,"0"); }
	            }
	        }
    	}
    }
    
    private boolean cssSingleSpacing(int nType,StyleWithProperties style, CSVList props, boolean bInherit, String sXML, String sCSS) {
        // Note: Only *absolute* margin and padding values fit with CSS
        if (bInherit || style.getProperty(nType,sXML,false)!=null) {
            String s = style.getAbsoluteProperty(nType,sXML);
            if (s!=null) {
            	props.addValue(sCSS,scale(s));
            	return true;
            }
        }
        return false;
    }
    
    // Borders. There are two ways to set borders in ODF:
    // Text borders can be set using the CSS compatible property fo:border and fo:border-*.
    // (The properties style:border-line-width and style:border-line-width-* cannot be supported by CSS).
    // Draw borders can be set using the draw:stroke, svg:stroke-width and svg:stroke-color,
    // which also converts directly to CSS compatible.
    // Transparency can be set with svg:stroke-opacity (percentage or float between 0.0 and 1.0) 
    
    public void cssBorder(int nType, StyleWithProperties style, CSVList props, boolean bInherit){
    	// First try draw border
    	if (!cssDrawBorder(style,props,bInherit)) {
	    	// The try common border
	        if (!cssSingleBorder(nType,style,props,bInherit,XMLString.FO_BORDER,"border")) {
	        	// Then individual borders
		        boolean bTop = cssSingleBorder(nType,style,props,bInherit,XMLString.FO_BORDER_TOP,"border-top");
		        boolean bRight = cssSingleBorder(nType,style,props,bInherit,XMLString.FO_BORDER_RIGHT,"border-right");
		        boolean bBottom = cssSingleBorder(nType,style,props,bInherit,XMLString.FO_BORDER_BOTTOM,"border-bottom");
		        boolean bLeft = cssSingleBorder(nType,style,props,bInherit,XMLString.FO_BORDER_LEFT,"border-left");
		        // And finally, if we inherit from parent we always set borders
		        if (bInherit) {
		            if (!bTop && !bRight && !bBottom && !bLeft) {
		                props.addValue("border","none");
		            }
		            else {
		                if (!bTop) { props.addValue("border-top","none"); }
		                if (!bRight) { props.addValue("border-right","none"); }
		                if (!bBottom) { props.addValue("border-bottom","none"); }
		                if (!bLeft) { props.addValue("border-left","none"); }
		            }
		        }
	        }
    	}
    }
    
    private boolean cssSingleBorder(int nType, StyleWithProperties style, CSVList props, boolean bInherit, String sXML, String sCSS) {
        if (bInherit || style.getProperty(nType,sXML,false)!=null) {
            String s = style.getProperty(nType,sXML,bInherit);
            if (s!=null) {
            	props.addValue(sCSS,modifyMultiValue(s,style.getGraphicProperty(XMLString.SVG_STROKE_OPACITY, bInherit)));
            	return true;
            }
        }
        return false;
    }
    
    private boolean cssDrawBorder(StyleWithProperties style, CSVList props, boolean bInherit) {
        if (bInherit || style.getGraphicProperty(XMLString.DRAW_STROKE,false)!=null) {
            String s = style.getGraphicProperty(XMLString.DRAW_STROKE,bInherit);
            if (s!=null) {
            	CSVList val = new CSVList(" ");
            	String sWidth = style.getGraphicProperty(XMLString.SVG_STROKE_WIDTH, bInherit);
            	if (sWidth!=null) { val.addValue(scale(sWidth)); }
            	switch (s) {
	            	case "solid": val.addValue("solid"); break; 
	            	case "dash": val.addValue("dashed"); break;
	            	case "none":
	            	default: val.addValue("none");
            	}
            	String sColor = style.getGraphicProperty(XMLString.SVG_STROKE_COLOR, bInherit);
            	if (sColor!=null) {
            		val.addValue(createColor(sColor,style.getGraphicProperty(XMLString.SVG_STROKE_OPACITY, bInherit)));
            	}
            	props.addValue("border", val.toString());
            	return true;
            }
        }
        return false;
    }
    
    // Shadow. A shadow can be set with the CSS compatible property style:shadow.
    // Alternatively it can be set with a combination of the properties draw:shadow,
    // draw:shadow-color, draw:shadow-offset-x and draw:shadow-offset-y, which is also CSS compatible.
    // Transparency can be set with the property draw:shadow-opacity

    public void cssShadow(int nType, StyleWithProperties style, CSVList props, boolean bInherit) {
    	// First check for draw shadow
    	if (!cssDrawShadow(style,props,bInherit)) {
        	// The try text shadow (same as in CSS)
        	String s = style.getProperty(nType,XMLString.STYLE_SHADOW, bInherit);
            if (s!=null) { props.addValue("box-shadow",modifyMultiValue(s,style.getGraphicProperty(XMLString.DRAW_SHADOW_OPACITY, bInherit))); }    		
    	}
    }
    
    private boolean cssDrawShadow(StyleWithProperties style, CSVList props, boolean bInherit) {
        if (bInherit || style.getGraphicProperty(XMLString.DRAW_SHADOW,false)!=null) {
            String s = style.getGraphicProperty(XMLString.DRAW_SHADOW,bInherit);
            if (s!=null) {
            	if (s.equals("visible")) {
            		// All three values are required, so we always try parent style, and next default frame style, and
            		// as a last resort we use a default value
	            	CSVList val = new CSVList(" ");
	            	String sX = style.getGraphicProperty(XMLString.DRAW_SHADOW_OFFSET_X, true);
	            	if (sX==null) { sX = ofr.getDefaultFrameStyle().getGraphicProperty(XMLString.DRAW_SHADOW_OFFSET_X, false); }
	            	if (sX!=null) { val.addValue(scale(sX)); } else { val.addValue("5px"); }
	            	String sY = style.getGraphicProperty(XMLString.DRAW_SHADOW_OFFSET_Y, true);
	            	if (sY==null) { sY = ofr.getDefaultFrameStyle().getGraphicProperty(XMLString.DRAW_SHADOW_OFFSET_Y, false); }
	            	if (sY!=null) { val.addValue(scale(sY)); } else { val.addValue("5px"); }
	            	String sColor = style.getGraphicProperty(XMLString.DRAW_SHADOW_COLOR, true);
	            	if (sColor==null) { sColor = ofr.getDefaultFrameStyle().getGraphicProperty(XMLString.DRAW_SHADOW_COLOR, false); }
	            	if (sColor!=null) {
	            		String sOpacity = style.getGraphicProperty(XMLString.DRAW_SHADOW_OPACITY, true);
	            		if (sOpacity==null) {
	            			sOpacity = ofr.getDefaultFrameStyle().getGraphicProperty(XMLString.DRAW_SHADOW_OPACITY, false);
	            		}
	            		val.addValue(createColor(sColor,sOpacity));
	            	}
	            	else {
	            		val.addValue("black");
	            	}
	            	props.addValue("box-shadow", val.toString());
            	}
            	return true;
            }
        }
        return false;    	
    }
    
    // Background. Background color can be set with the CSS compatible properties fo:background-color
    // or draw:fill-color. Transparency can be set with draw:opacity.
    // TODO: The support for other background types is currently incomplete
    
    public void cssBackground(int nType, StyleWithProperties style, CSVList props, boolean bInherit) {
    	// First we must check for draw background which according to the ODF spec takes precedence
    	if (!cssDrawBackground(style,props,bInherit)) {
	        // Background color: Same as in CSS
	        String s = style.getProperty(nType,XMLString.FO_BACKGROUND_COLOR,bInherit);
	        if (s!=null && !"transparent".equals(s)) {
	        	// Note: draw:opacity is always attached to graphic-properties
	        	props.addValue("background-color",createColor(s,style.getGraphicProperty(XMLString.DRAW_OPACITY, bInherit)));
	        }
	        // Background image:
	        String sUrl = style.getBackgroundImageProperty(XMLString.XLINK_HREF);
	        if (sUrl!=null) { // currently only support for linked image
	            props.addValue("background-image","url("+escapeUrl(sUrl)+")");
	
	            String sRepeat = style.getBackgroundImageProperty(XMLString.STYLE_REPEAT);
	            if ("no-repeat".equals(sRepeat) || "stretch".equals(sRepeat)) {
	                props.addValue("background-repeat","no-repeat");
	            }
	            else {
	                props.addValue("background-repeat","repeat");
	            }
	
	            String sPosition = style.getBackgroundImageProperty(XMLString.STYLE_POSITION);
	            if (sPosition!=null) { props.addValue("background-position",sPosition); }
	        }
    	}
    }
    
    private boolean cssDrawBackground(StyleWithProperties style, CSVList props, boolean bInherit) {
    	// Note that this draw background is always attached to graphic-properties, so we don't use nType
    	// TODO: PageStyleConverter reads draw:fill-color too, should probably use this class..
    	String s = style.getGraphicProperty(XMLString.DRAW_FILL,bInherit);
    	if (s!=null) {
    		if (s.equals("solid")) {
    			// TODO: Other values are bitmap, gradient, hatch and none
    			s = style.getGraphicProperty(XMLString.DRAW_FILL_COLOR, bInherit);
    	        if (s!=null) {
    	        	props.addValue("background-color",createColor(s,style.getGraphicProperty(XMLString.DRAW_OPACITY, bInherit)));
    	        	return true;
    	        }
    		}
    	}
    	return false;
    }
    
    // Helper methods
	
    // Modify the multivalue properties for fo:border and style:shadow.
    // Lengths will be scaled, colors modified with opacity and some border styles will be replaced.
    // LO exports the border styles solid, dotted, dashed, double, fine-dashed, dash-dot, dash-dot-dot and double-thin.
    // The latter four does not exist in CSS and will be replaced with their closest match
    private String modifyMultiValue(String sValue, String sOpacity) {
    	CSVList newValue = new CSVList(" ");
    	String[] sValues = sValue.split(" ");
    	int nLen = sValues.length;
    	for (int i=0; i<nLen; i++) {
    		if (sValues[i].length()>0) {
    			if (Character.isDigit(sValues[i].charAt(0))) {
    	            // If it's a number it must be a unit -> convert it
                    String sDim = scale(sValues[i]);
                    if (Calc.isLessThan(sDim, "1px")) {
                        // Do not output less than 1px - some browsers will render it invisible
                    	newValue.addValue("1px");
                    }
                    else {
                    	newValue.addValue(sDim);
                    }
    			}
    			else if (sValues[i].charAt(0)=='#') {
    	            // If it starts with # it must be a color -> add opacity
    				newValue.addValue(createColor(sValues[i],sOpacity));
    			}
    			else if (sValues[i].equals("fine-dashed") || sValues[i].equals("dash-dot")) {
    				newValue.addValue("dashed");
    			}
    			else if (sValues[i].equals("dash-dot-dot")) {
    				newValue.addValue("dotted");
    			}
    			else if (sValues[i].equals("double-thin")) {
    				newValue.addValue("double");
    			}
    			else {
    				newValue.addValue(sValues[i]);
    			}
    		}
    	}
    	return newValue.toString();
    }
    
    // Convert a combination of color and opacity to CSS
    private String createColor(String sColor, String sOpacity) {
    	if (sOpacity!=null && !sOpacity.equals("100%") && !sOpacity.equals("1.0") && sColor.matches("#[A-Fa-f0-9]{6}")) {
		    int nRed = Integer.parseInt(sColor.substring(1,3), 16);
		    int nGreen = Integer.parseInt(sColor.substring(3,5), 16);
		    int nBlue = Integer.parseInt(sColor.substring(5), 16);
			float fOpacity;
			// Opacity can be either a percentage or a float between 0 and 1
			if (sOpacity.matches("\\d+%")) {
				fOpacity = Calc.getFloat(sOpacity.substring(0,sOpacity.length()-1),1)/100;
			}
			else {
				fOpacity = Calc.getFloat(sOpacity, 1);
			}
			return "rgba("+nRed+","+nGreen+","+nBlue+","+fOpacity+")";
    	}
   		return sColor;
    }
	
    // Must escape certain characters in the URL property	
    private String escapeUrl(String sUrl) {
        StringBuilder buf = new StringBuilder();
        int nLen = sUrl.length();
        for (int i=0; i<nLen; i++) {
            char c = sUrl.charAt(i);
            if (c=='\'' || c=='"' || c=='(' || c==')' || c==',' || c==' ') {
                buf.append("\\");
            }
            buf.append(c);
        }
        return buf.toString();
    }

}
