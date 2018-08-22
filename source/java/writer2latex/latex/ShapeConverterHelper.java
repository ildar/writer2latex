/************************************************************************
 *
 *  ShapeConverterHelper.java
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
 *  Version 2.0 (2018-08-22)
 *
 */

package writer2latex.latex;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;

abstract class ShapeConverterHelper extends ConverterHelper {
	
	// Stroke and fill options for the current shape, derived from the graphic style
	CSVList strokeOptions=null;
	CSVList fillOptions=null;
	
	// Parameters for the current shape derived from the graphic style
	boolean bHasfill=false; // Flag to indicate that a fill style is present
	boolean bHasstroke=false; // Flag to indicate that a stroke style is present

	ShapeConverterHelper(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
	void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
		// Nothing to do, the DrawConverter class does the work
	}
	
	abstract double getMaxY(Element shape);
	
	void handleShape(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// Apply style, which also sets the global parameters bHasfill and bHasstroke
		StyleWithProperties style = ofr.getFrameStyle(shape.getAttribute(XMLString.DRAW_STYLE_NAME));
		fillOptions = new CSVList(",","=");
		applyFillStyle(style, fillOptions);
		strokeOptions = new CSVList(",","=");
		applyStrokeStyle(style, strokeOptions);
	}
	
	void convertText(Element shape, 
		String sTop, String sRight, String sBottom, String sLeft,
		double dAngle,
		LaTeXDocumentPortion ldp, Context oc) {
		if (hasText(shape)) {
			// Get the paragraph style associated with the shape
			StyleWithProperties parStyle1 = ofr.getParStyle(shape.getAttribute(XMLString.DRAW_TEXT_STYLE_NAME));
			// Get the paragraph style associated with the first paragraph
			StyleWithProperties parStyle2 = null;
			Element firstPar = Misc.getChildByTagName(shape, XMLString.TEXT_P);
			if (firstPar!=null) { // actually we already know that
				parStyle2 = ofr.getParStyle(firstPar.getAttribute(XMLString.TEXT_STYLE_NAME));
			}
			Context ic = (Context) oc.clone();
			ic.setInTikZText(true);
			BeforeAfter ba = new BeforeAfter();
			StyleWithProperties style = ofr.getFrameStyle(shape.getAttribute(XMLString.DRAW_STYLE_NAME));
			applyNodeProperties(style,parStyle1,parStyle2,sTop,sRight,sBottom,sLeft,dAngle,ba,ic);
			ldp.append(ba.getBefore());
			traverseText(shape, ldp, ic);
			ldp.append(ba.getAfter()).nl();
		}
	}
	
	private boolean hasText(Element shape) {
		Node child = shape.getFirstChild();
		while (child!=null) {
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				if (child.getNodeName().equals(XMLString.TEXT_P)) {
					if (child.hasChildNodes()) {
						// Only non-empty paragraph counts (LO always exports an empty paragraph)
						return true;
					}
				}
				// TODO: List are currently ignored (LO does not support lists, anyway)
			}
			child = child.getNextSibling();
		}
		return false;
	}
	
	private void traverseText(Element shape, LaTeXDocumentPortion ldp, Context oc) {
    	Node child = shape.getFirstChild();
    	boolean bAfterParagraph = false;
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element elm = (Element)child;
                String nodeName = elm.getTagName();

                if (nodeName.equals(XMLString.TEXT_P)) {
                	Context ic = (Context) oc.clone();
                	StyleWithProperties style = ofr.getParStyle(shape.getAttribute(XMLString.TEXT_STYLE_NAME));
                	BeforeAfter ba = new BeforeAfter();
            		palette.getCharSc().applyFont(style, true, true, ba, ic);
                	ic.updateFormattingFromStyle(style);
                	// TODO: if the text width option is set, \par would be fine
                	// In this case it would also be possible to use separate alignment for each paragraph
                	if (bAfterParagraph) { ldp.append("\\\\"); }
                	ldp.append(ba.getBefore());
                    palette.getInlineCv().traverseInlineText(elm,ldp,ic);
                    bAfterParagraph = true;
                }
                // TODO: Lists are currently ignored (LO does not support them anyway)
                // Note that the text width option is required for lists to work
            }
            child = child.getNextSibling();
        }
    }
	
	// Formatting
	
	// Text node formatting
	private void applyNodeProperties(StyleWithProperties style, StyleWithProperties parStyle1, StyleWithProperties parStyle2,
			String sTop, String sRight, String sBottom, String sLeft, double dAngle,
			BeforeAfter ba, Context oc) {		
		// Create node options
		CSVList options = new CSVList(",","=");

		// Get the padding - the text area must be shrinked slightly by these amounts
		// Note that padding is a length with unit, not a number within the coordinate system defined by the view box
		String sPaddingLeft = getGraphicProperty(style,XMLString.FO_PADDING_LEFT);
		String sPaddingRight = getGraphicProperty(style,XMLString.FO_PADDING_RIGHT);
		String sPaddingTop = getGraphicProperty(style,XMLString.FO_PADDING_TOP);
		String sPaddingBottom = getGraphicProperty(style,XMLString.FO_PADDING_BOTTOM);
		
		// Calculate the anchor point from the style, taking the padding into account
		// ODF offers 9 anchor positions within the text area, and for each of these we should specify the
		// placement of the TikZ node. For example the anchor point (top,left) needs position below right
		String sAnchorX;
		String sAnchorY;
		CSVList placement = new CSVList(" ");
		
		String sVerticalAlign = getGraphicProperty(style, XMLString.DRAW_TEXTAREA_VERTICAL_ALIGN);
		if (sVerticalAlign==null) { sVerticalAlign="top"; }
		switch(sVerticalAlign) {
		case "bottom":
			sAnchorY = sBottom;
			if (sPaddingBottom!=null) { sAnchorY = Calc.add(sAnchorY, sPaddingBottom); }
			placement.addValue("above"); // or south anchor
			break;
		case "middle":
			sAnchorY = Calc.multiply(0.5F, Calc.add(sTop, sBottom));
			if (sPaddingTop!=null && sPaddingBottom!=null) {
				// In case of asymmetric padding, the center should be shifted slightly
				sAnchorY = Calc.sub(sAnchorY,Calc.multiply(0.5F, Calc.sub(sPaddingTop, sPaddingBottom)));
			}
			break;
		case "top":
		case "justify":
		default:
			sAnchorY = sTop;
			if (sPaddingTop!=null) { sAnchorY = Calc.sub(sAnchorY, sPaddingTop); }
			placement.addValue("below"); // or north anchor
		}
		sAnchorY = sAnchorY.substring(0, sAnchorY.length()-2); // strip the unit

		String sHorizontalAlign = getGraphicProperty(style, XMLString.DRAW_TEXTAREA_HORIZONTAL_ALIGN);
		if (sHorizontalAlign==null) { sHorizontalAlign="center"; }
		switch(sHorizontalAlign) {
		case "right":
			sAnchorX = sRight;
			if (sPaddingRight!=null) { sAnchorX = Calc.sub(sAnchorX, sPaddingRight); }
			placement.addValue("left"); // or east anchor
			break;
		case "center":
			sAnchorX = Calc.multiply(0.5F, Calc.add(sLeft, sRight));
			if (sPaddingLeft!=null && sPaddingRight!=null) {
				// In case of asymmetric padding, the center should be shifted slightly
				sAnchorX = Calc.add(sAnchorX,Calc.multiply(0.5F, Calc.sub(sPaddingLeft, sPaddingRight)));
			}
			break;
		case "left":
		case "justify":
		default:
			sAnchorX = sLeft;
			if (sPaddingLeft!=null) { sAnchorX = Calc.add(sAnchorX, sPaddingLeft); }
			placement.addValue("right"); // or west anchor
		}
		sAnchorX = sAnchorX.substring(0, sAnchorX.length()-2); // strip the unit
		
		if (!placement.isEmpty()) { options.addValue(placement.toString()); }

		// Determine the alignment from the provided paragraph styles
		String sAlign = null;
		if (parStyle2!=null) { sAlign=parStyle2.getParProperty(XMLString.FO_TEXT_ALIGN, true); }
		if (sAlign==null && parStyle1!=null) { sAlign=parStyle1.getParProperty(XMLString.FO_TEXT_ALIGN, true); }
		if (sAlign==null) { sAlign="left"; }
		switch (sAlign) {
		case "justify":
		case "center": break;
		case "right":
		case "end": sAlign = "right"; break;
		case "left":
		case "start":
		default: sAlign = "left";
		}
		// TODO: left, right and center could/should be preceded by flush for narrow nodes

		options.addValue("align", sAlign);
		
		// Determine automatic wrapping from the style
		if ("wrap".equals(getGraphicProperty(style,XMLString.FO_WRAP_OPTION))) {
			// In TikZ, automatic wrapping of text is equivalent to setting the width
			String sWidth = Calc.sub(sRight, sLeft);
			// If there is padding, we have to shrink the width slightly
			if (sPaddingLeft!=null) { sWidth = Calc.sub(sWidth,sPaddingLeft); }
			if (sPaddingRight!=null) { sWidth = Calc.sub(sWidth,sPaddingRight); }
			options.addValue("text width", sWidth);
		}
		
		// Determine text formatting from the first paragraph style
		BeforeAfter baTextFormat = new BeforeAfter();
		palette.getCharSc().applyFont(parStyle1, true, true, baTextFormat, oc);
		oc.resetFormattingFromStyle(parStyle1);
		
		// Finally create the node (as a separate path, as the text does not belong to a specific path)
		if (Math.abs(dAngle)<0.01) { // No rotation
			ba.add("\\path ("+sAnchorX+","+sAnchorY+") node["+options.toString()+"] {","};");
		}
		else {
			String sMidX = Calc.multiply(0.5F, Calc.add(sLeft, sRight));
			String sMidY = Calc.multiply(0.5F, Calc.add(sTop, sBottom));
			options.addValue("transform shape");
			ba.add("\\path[rotate around={"+dAngle+":("+sMidX+","+sMidY+")}] ("+sAnchorX+","+sAnchorY+") node["+options.toString()+"] {","};");			
		}
		if (!baTextFormat.getBefore().isEmpty()) {
			ba.addBefore(baTextFormat.getBefore()+" ");
		}
	}
		
	// Set fill color, fill opacity and even odd rule
	private void applyFillStyle(StyleWithProperties style, CSVList props) {
    	String s = getGraphicProperty(style,XMLString.DRAW_FILL);
    	if (s==null || s.equals("solid")) { // solid seems to be default
    		// TODO: Other values are bitmap, gradient, hatch and none
    		s = getGraphicProperty(style,XMLString.DRAW_FILL_COLOR);
    		if (palette.getColorCv().applyNamedColor(s, "fill", props)) {
    			bHasfill = true;
    			props.addValue("even odd rule");
    			s = getGraphicProperty(style,XMLString.DRAW_OPACITY);
    			if (s!=null) {
    				float f = Calc.getFloat(s.substring(0,s.length()-1), 100)/100.0F;
    				props.addValue("fill opacity", format(f));
    			}
    		}
    	}
	}
	
	// Set path style, width, color and opacity
	private void applyStrokeStyle(StyleWithProperties style, CSVList props) {
        String s = getGraphicProperty(style,XMLString.DRAW_STROKE);
        if (s==null || s.equals("solid") || s.equals("dash")) {
        	// solid seems to be the default, last value is "none"
        	bHasstroke = true;
        	// Set the style
        	if ("dash".equals(s)) {
        		// TODO: Convert ODF dash styles to TikZ dash patterns
        		props.addValue("dashed");
        	}
    		// Set the width
        	s = getGraphicProperty(style,XMLString.SVG_STROKE_WIDTH);
        	if (s!=null) {
        		props.addValue("line width", s);
        	}
        	// Set the color
        	s = getGraphicProperty(style,XMLString.SVG_STROKE_COLOR);
    		if (!palette.getColorCv().applyNamedColor(s, "draw", props)) {
    			// No color, use default (black)
    			props.addValue("draw");
    		}
    		// Set the opacity
        	s = getGraphicProperty(style,XMLString.SVG_STROKE_OPACITY);
			if (s!=null) {
				float f = Calc.getFloat(s.substring(0,s.length()-1), 100)/100.0F;
				props.addValue("draw opacity", format(f));
			}
    	}
	}
	
	// Set arrow style (simplistic version not taking the actual arrow style into account)
	void applyArrowStyle(StyleWithProperties style, CSVList props) {
		String sStart = getGraphicProperty(style,XMLString.DRAW_MARKER_START);
		boolean bStart = sStart!=null && !sStart.isEmpty();
		String sEnd = getGraphicProperty(style,XMLString.DRAW_MARKER_END);
		boolean bEnd = sEnd!=null && !sEnd.isEmpty();
		if (bStart && bEnd) {
			props.addValue("<->");
		}
		else if (bStart) {
			props.addValue("<-");
		}
		else if (bEnd) {
			props.addValue("->");			
		}
	}
	
	// Always look in default style if a property is not set
	String getGraphicProperty(StyleWithProperties style, String sProperty) {
		String s = style.getGraphicProperty(sProperty, true);
		return s!=null ? s : ofr.getDefaultFrameStyle().getGraphicProperty(sProperty, false);
	}
	
	// Some helper methods
	
	double getParameter(Element shape, String sXML) {
		String s = Misc.getAttribute(shape, sXML);
		return s!=null ? Calc.length2cm(s) : 0;
	}

	String format(double d) {
		String s = String.format(Locale.ROOT, "%.3f", d);
		if (s.endsWith(".000")) {
			// Special treatment of (near) integers
			s = Integer.toString((int)d);
		}
		return s;
	}
}
