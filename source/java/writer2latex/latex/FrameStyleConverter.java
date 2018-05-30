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
 *  Version 2.0 (2018-05-30)
 *
 */
 
package writer2latex.latex;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;

/** <p>This class converts OpenDocument graphic (frame) styles to LaTeX</p>
 *  <p>Formatting is done using the package <code>longfbox</code> (controlled by the option <code>use_longfbox</code>)</p> 
 *  <p>Other styles have frame properties too, see {@link writer2latex.xhtml.FrameStyleConverter} for details.</p>  
 * 	<p>In addition to frame styles, this class provides methods to convert box properties for text styles and paragraph styles.
 * 	Section styles are not suppported because <code>longfbox</code> does not interact well with <code>multicols</code>,
 * 	<code>longtable</code> and <code>supertabular</code>. Also nested <code>longfbox</code> enviroments cannot break across pages.
 * 	Page layout and tables are handled using other packages.</p>
 */
 class FrameStyleConverter extends ConverterHelper {
	 
	 private boolean bUseLongfbox;
	 private String sBorderRadius;
	 
	 private boolean bNeedLongfbox = false;

	/** Create a new <code>FrameStyleConverter</code>
	 * 
	 * @param ofr The office reader from which styles are read
	 * @param config the LaTeX configuration to use
	 * @param palette the converter palette to provide access to other converter helpers
	 */
	FrameStyleConverter(OfficeReader ofr, LaTeXConfig config,
			ConverterPalette palette) {
		super(ofr, config, palette);
		bUseLongfbox = config.useLongfbox();
		sBorderRadius = config.borderRadius();
	}
	
	void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
		if (bNeedLongfbox) {
			pack.append("\\usepackage{longfbox}").nl();
		}
	}
	
	/** Generate LaTeX code to apply a frame style to an image. The generated code will be a <code>\lfbox</code>
	 *  with appropriate options. No code will be generated if all margins and paddings are zero, all border styles
	 *  are none, background is transparent and the frame is aligned to the baseline. 
	 * 
	 * @param style the office style to apply
	 * @param ba the container for the generated LaTeX code
	 * @param sY the relative vertical position
	 * @param bAsChar true if the images should be anchored as character (this will add additional
	 * properties for vertical alignment)
	 * @return true if the generated code is non-empty
	 */
	boolean applyFrameStyleImage(StyleWithProperties style, BeforeAfter ba, String sY, boolean bAsChar) {
		if (bUseLongfbox) {
			CSVList props = new CSVList(",","=");
			boolean bHasProperties = margin(style, props);
			bHasProperties |= border(style, props);
			bHasProperties |= padding(style, props);
			bHasProperties |= drawBackgroundColor(style, props);
			if (bAsChar) {
				bHasProperties |= verticalAlign(style, props, sY);
			}
			if (bHasProperties) {
				ba.add("\\lfbox["+props.toString()+"]{","}");
				bNeedLongfbox = true;
				return true;
			}
		}
		return false;
	}
	
	/** Generate LaTeX code to apply a frame style to a text frame. The generated code will be a <code>longfbox</code>
	 *  environment with appropriate options, if <code>use_longfbox</code> is true. Otherwise it will be a
	 *  <code>minipage</code> environment. 
	 * 
	 * @param style the office style to apply
	 * @param ba the container for the generated LaTeX code
	 * @param sWidth the width of the text frame
	 * @param sY the relative vertical position
	 * @param bAsChar true if the images should be anchored as character (this will add additional
	 * properties for vertical alignment)
	 */
	void applyFrameStyleText(StyleWithProperties style, BeforeAfter ba, String sWidth, String sY, boolean bAsChar) {
		if (bUseLongfbox) {
			CSVList props = new CSVList(",","=");
			margin(style, props);
			border(style, props);
			padding(style, props);
			drawBackgroundColor(style, props);
			if (bAsChar) {
				verticalAlign(style, props, sY);
			}
			props.addValue("width", sWidth);
			ba.add("\\begin{longfbox}["+props.toString()+"]","\\end{longfbox}");
			bNeedLongfbox = true;
		}
		else {
			ba.add("\\begin{minipage}{"+sWidth+"}","\\end{minipage}");
		}
	}
	
	// Convert individual style properties
	
	// Convert draw background color; returns true if a color is set
	private boolean drawBackgroundColor(StyleWithProperties style, CSVList props) {
    	String s = style.getGraphicProperty(XMLString.DRAW_FILL,true);
    	if (s!=null && s.equals("solid")) {
    		// TODO: Other values are bitmap, gradient, hatch and none
    		return palette.getColorCv().applyLongfboxColor(
    				style.getGraphicProperty(XMLString.DRAW_FILL_COLOR, true),"background-color", props);
    	}
    	// Treat as "none"
    	return false;
	}
	
	// Convert margin; returns true if at least one non-zero margin is applied
    private boolean margin(StyleWithProperties style, CSVList props){
    	return spacing(style,props,
			XMLString.FO_MARGIN,XMLString.FO_MARGIN_TOP,XMLString.FO_MARGIN_RIGHT,XMLString.FO_MARGIN_BOTTOM,XMLString.FO_MARGIN_LEFT,
			"margin","margin-top","margin-right","margin-bottom","margin-left");
    }
    
	// Convert padding; returns true if at least one non-zero padding is applied
    private boolean padding(StyleWithProperties style, CSVList props){
    	return spacing(style,props,
			XMLString.FO_PADDING,XMLString.FO_PADDING_TOP,XMLString.FO_PADDING_RIGHT,XMLString.FO_PADDING_BOTTOM,XMLString.FO_PADDING_LEFT,
			"padding","padding-top","padding-right","padding-bottom","padding-left");
    }
    
    private boolean spacing(StyleWithProperties style, CSVList props,
    		String sXML, String sXMLTop, String sXMLRight, String sXMLBottom, String sXMLLeft,
    		String sLfbox, String sLfboxTop, String sLfboxRight, String sLfboxBottom, String sLfboxLeft){
    	// First try main property to set all four sides
    	if (singleSpacing(style,props,sXML,sLfbox)) {
    		return true;
    	}
    	else {
    		// Then try individual properties for each side
	        boolean bTop = singleSpacing(style,props,sXMLTop,sLfboxTop);
	        boolean bRight = singleSpacing(style,props,sXMLRight,sLfboxRight);
	        boolean bBottom = singleSpacing(style,props,sXMLBottom,sLfboxBottom);
	        boolean bLeft = singleSpacing(style,props,sXMLLeft,sLfboxLeft);
	        // And finally, set the zero spacing as required
            if (!bTop && !bRight && !bBottom && !bLeft) {
                props.addValue(sLfbox,"0mm");
            }
            else {
                if (!bTop) { props.addValue(sLfboxTop,"0mm"); }
                if (!bRight) { props.addValue(sLfboxRight,"0mm"); }
                if (!bBottom) { props.addValue(sLfboxBottom,"0mm"); }
                if (!bLeft) { props.addValue(sLfboxLeft,"0mm"); }
            }
            return bTop || bRight || bBottom || bLeft;
    	}
    }
    
    private boolean singleSpacing(StyleWithProperties style, CSVList props, String sXML, String sLfbox) {
        // Note: Only *absolute* margin and padding values can be used in a lfbox
        String s = style.getAbsoluteProperty(StyleWithProperties.GRAPHIC,sXML);
        if (s!=null && !Calc.isZero(s)) {
        	props.addValue(sLfbox,s);
        	return true;
        }
        return false;
    }

	// Convert border. Return true if at least one border is set    
	private boolean border(StyleWithProperties style, CSVList props) {
		if (borderType(style, props)) {
			// Add border-radius (a percentage of the minimal padding)
			String sPadding = style.getGraphicProperty(XMLString.FO_PADDING,true);
			if (sPadding==null) {
				String sPaddingTop = style.getGraphicProperty(XMLString.FO_PADDING_TOP, true);
				String sPaddingRight = style.getGraphicProperty(XMLString.FO_PADDING_RIGHT, true);
				String sPaddingBottom = style.getGraphicProperty(XMLString.FO_PADDING_BOTTOM, true);
				String sPaddingLeft = style.getGraphicProperty(XMLString.FO_PADDING_LEFT, true);
				sPadding = Calc.min(sPaddingTop, sPaddingRight, sPaddingBottom, sPaddingLeft);
			}
			if (sPadding!=null) {
				String sRadius = Calc.multiply(sBorderRadius, sPadding);
				if (!Calc.isZero(sRadius)) {
					props.addValue("border-radius", sRadius);
				}
			}
			return true;
		}
		return false;
	}
    
	private boolean borderType(StyleWithProperties style, CSVList props) {
    	// First try draw border
        String s = style.getGraphicProperty(XMLString.DRAW_STROKE,true);
        boolean bBorder = false;
        if (s!=null) {
        	switch (s) {
        	case "solid": props.addValue("border-style","solid"); bBorder=true; break; 
        	case "dash": props.addValue("border-style","dashed"); bBorder=true; break;
        	case "none":
        	default:
        	}
    	}
        if (bBorder) {
        	String sWidth = style.getGraphicProperty(XMLString.SVG_STROKE_WIDTH, true);
        	if (sWidth!=null) { props.addValue("border-width",sWidth); }
        	palette.getColorCv().applyLongfboxColor(
        			style.getGraphicProperty(XMLString.SVG_STROKE_COLOR, true), "border-color", props);
        	return true;
        }

		// Then try common text border
		if (singleBorder(style,props,XMLString.FO_BORDER,"border")) {
			return true;
		}
		else { // Finally try individual borders
			boolean bTop = singleBorder(style,props,XMLString.FO_BORDER_TOP,"border-top");
			boolean bRight = singleBorder(style,props,XMLString.FO_BORDER_RIGHT,"border-right");
			boolean bBottom = singleBorder(style,props,XMLString.FO_BORDER_BOTTOM,"border-bottom");
			boolean bLeft = singleBorder(style,props,XMLString.FO_BORDER_LEFT,"border-left");
			// Set missing borders
		    if (!bTop && !bRight && !bBottom && !bLeft) {
		        props.addValue("border-style","none");
		    }
			else {
			    if (!bTop) { props.addValue("border-top-style","none"); }
				if (!bRight) { props.addValue("border-right-style","none"); }
				if (!bBottom) { props.addValue("border-bottom-style","none"); }
				if (!bLeft) { props.addValue("border-left-style","none"); }
			}
		    return bTop || bRight || bBottom || bLeft;
        }
	}
	    
    private boolean singleBorder(StyleWithProperties style, CSVList props, String sXML, String sLfbox) {
        String s = style.getGraphicProperty(sXML,true);
        if (s!=null) {
        	String[] sValues = s.split(" ");
        	int nLen = sValues.length;
        	for (int i=0; i<nLen; i++) {
        		if (sValues[i].length()>0) {
        			if (Character.isDigit(sValues[i].charAt(0))) {
        	            // If it's a number it must be a dimension -> border-width
        				props.addValue(sLfbox+"-width",sValues[i]);
        			}
        			else if (sValues[i].charAt(0)=='#') {
        	            // If it starts with # it must be a color -> border-color
        	        	palette.getColorCv().applyLongfboxColor(sValues[i], sLfbox+"-color", props);
        			}
        			else if (sValues[i].equals("solid")) {
        				props.addValue(sLfbox+"-style", "solid");
        			}
        			else if (sValues[i].equals("dashed") || sValues[i].equals("fine-dashed") || sValues[i].equals("dash-dot")) {
        				props.addValue(sLfbox+"-style", "dashed");
        			}
        			else if (sValues[i].equals("dotted") || sValues[i].equals("dash-dot-dot")) {
        				props.addValue(sLfbox+"-style", "dotted");
        			}
        			else if (sValues[i].equals("double") || sValues[i].equals("double-thin")) {
        				props.addValue(sLfbox+"-style", "double");
        			}
        			else if (sValues[i].equals("none")) {
        				return false;
        			}
        		}
        	}
        	return true;
        }
        return false;
    }
    
    // Vertical align for images anchored as-char
	// Note: According to the ODF spec for style:vertical-pos we have
    // - bottom: the bottom of the frame is aligned with the reference area. 
    // - top: the top of the frame is aligned with the reference area.
    // Unfortunately it seems that LO uses this the other way round if style:vertical-rel is baseline...
    // TODO: Should we use some of the other possible values of vertical-align?
    // Return true if other than default alignment is used
    private boolean verticalAlign(StyleWithProperties style, CSVList props, String sY) {
    	String sPos = style.getProperty(XMLString.STYLE_VERTICAL_POS);
    	if (sPos!=null) {
    		// style_vertical-rel can be baseline, char or line
    		String sRel = style.getProperty(XMLString.STYLE_VERTICAL_REL);
    		boolean bBaseline = sRel==null || "baseline".equals(sRel);
    		switch (sPos) {
    		case "from-top":
    			props.addValue("vertical-align", "top");
    			if (!Calc.isZero(sY)) {
    				props.addValue("raise",Calc.multiply("-100%", sY));
    			}
    			return true;
    		case "middle":
    			props.addValue("vertical-align", "middle");
    			return true;
    		case "bottom":
    			if (bBaseline) { // LO bug as mentioned
	    			props.addValue("vertical-align", "top");
	    			return true;
    			}
    		case "top":
    			if (!bBaseline) { // LO bug as mentioned
	    			props.addValue("vertical-align", "top");
	    			return true;
    			}
    		default:
    			// Otherwise align to the baseline, this is the default - even without longfbox
    		}
    	}
    	return false;
    }
	    
}
