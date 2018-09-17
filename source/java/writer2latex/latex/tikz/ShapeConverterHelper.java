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
 *  Version 2.0 (2018-09-17)
 *
 */

package writer2latex.latex.tikz;

import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.ConverterHelper;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.LaTeXPacman;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/** This is a base class for all TikZ shape converters. It handles stroke and fill formatting,
 *  transformations and text nodes.
 */
abstract class ShapeConverterHelper extends ConverterHelper {
	
	// Fill, stroke and arrow options for the current shape, derived from the graphic style
	CSVList fillOptions=null;
	CSVList strokeOptions=null;
	CSVList arrowOptions=null;
	
	// Parameters for the current shape derived from the graphic style
	boolean bHasfill=false; // Flag to indicate that a fill style is present
	boolean bHasstroke=false; // Flag to indicate that a stroke style is present

	ShapeConverterHelper(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
	public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
		// Nothing to do, the TikZConverter class does the work
	}
	
	/** Each shape converter must be able to determine the maximal y-coordinate. Because ODF uses an upside down
	 *  y axis we have to change sign on all y-coordinates. To avoid negative coordinates (which would be OK, but
	 *  looks rather unusual) we translate the y-axis such that the orgin is in the lower left corner.
	 *  This method is used to calculate the necessary translation.
	 *  
	 *  @return the maximal y-coordinate in cm
	 */
	abstract double getMaxY(Element shape);
	
	/** This method converts a shape. The subclass must convert the actual path, and use the members and
	 *  methods of this class to apply the stroke and fill style and to convert text nodes.
	 * 
	 * @param shape the ODF draw shape to convert
	 * @param dTranslateY the translation of the y-axis to apply
	 * @param ldp the LaTeXDocumentPortion to which code should be added
	 * @param oc the current context
	 */
	void handleShape(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// Apply style, which also sets the members bHasfill and bHasstroke
		// The subclass must use the members fillOptions and strokeOptions when constructing the path options
		StyleWithProperties style = ofr.getFrameStyle(shape.getAttribute(XMLString.DRAW_STYLE_NAME));
		fillOptions = new CSVList(",","=");
		applyFillStyle(style, fillOptions);
		strokeOptions = new CSVList(",","=");
		applyStrokeStyle(style, strokeOptions);
		arrowOptions = new CSVList(",","=");
		applyArrowOptions(style, arrowOptions);

		// Apply transformation
		CSVList transformOptions = new CSVList(";","=");
		try {
			applyTransformOptions(shape, dTranslateY, transformOptions);
		}
		catch (Exception e) {
			System.err.println("Error in draw:transform "+e.getMessage());
		}
		if (!transformOptions.isEmpty()) {
			// TikZ reads the transformations in reverse order (unlike ODF)
			// TODO: Extend CSVList to avoid this less elegant method...
			CSVList reversed = new CSVList(",");
			String[] s = transformOptions.toString().split(";");
			int n = s.length;
			for (int i=n-1; i>=0; i--) {
				reversed.addValue(s[i]);
			}
			ldp.append("\\begin{scope}[").append(reversed.toString()).append("]").nl();
		}
		
		handleShapeInner(shape,dTranslateY, ldp, oc);
		
		if (!transformOptions.isEmpty()) {
			ldp.append("\\end{scope}").nl();
		}
	}
	
	/** The subclass must implement this method to convert a draw shape to TikZ paths
	 * 
	 * @param shape the ODF draw shape to convert
	 * @param dTranslateY the translation of the y-axis to apply
	 * @param ldp the LaTeXDocumentPortion to which code should be added
	 * @param oc the current context
	 */
	abstract void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc);
	
	/** Get a length attribute from a draw shape in cm
	 * 
	 * @param shape the ODF draw shape
	 * @param sXML the name of the attribute
	 * @return the length in cm
	 */
	protected double getParameter(Element shape, String sXML) {
		String s = Misc.getAttribute(shape, sXML);
		return s!=null ? Calc.length2cm(s) : 0;
	}

	/** Format a double to 3 decimal points (or none if the are all zero). This method should be used on all
	 *  numbers in the TikZ path.
	 * 
	 * @param d the double to format
	 * @return a String with the required format
	 */
	String format(double d) {
		String s = String.format(Locale.ROOT, "%.3f", d);
		if (s.endsWith(".000")) {
			// Special treatment of (near) integers
			s = s.substring(0,s.length()-4);
		}
		return s;
	}
	
	/** This method creates a text node for a draw element. The subclass should use this in and appropriate place in 
	 *  the <code>handleShapeInner</code> method.
	 * 
	 * @param shape the ODF draw element to convert
	 * @param sTop the top edge of the text area as a length with unit
	 * @param sRight the right edge of the text area 
	 * @param sBottom the bottom edge of the text area
	 * @param sLeft the left edge of the text are
	 * @param dAngle the angle (in degress) by which the text node should be rotated
	 * @param ldp the LaTeXDocumentPortion to which code should be added
	 * @param oc the current context
	 */
	void convertText(Element shape, 
		String sTop, String sRight, String sBottom, String sLeft,
		double dAngle,boolean bForceWrap,
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
			applyNodeProperties(style,parStyle1,parStyle2,sTop,sRight,sBottom,sLeft,dAngle,bForceWrap,ba,ic);
			ldp.append(ba.getBefore());
			traverseText(shape, ldp, ic);
			ldp.append(ba.getAfter()).nl();
		}
	}
	
	// Remaining methods are private
	
	// Test whether a draw element has a text node
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
	
	// Convert the text content of a draw element
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
	
	// Convert text node formatting
	private void applyNodeProperties(StyleWithProperties style, StyleWithProperties parStyle1, StyleWithProperties parStyle2,
			String sTop, String sRight, String sBottom, String sLeft, double dAngle, boolean bForceWrap,
			BeforeAfter ba, Context oc) {
		
		// TODO: Check for vertical text (adjust angle with -90 degrees)
		// boolean bVertical = "tb-rl".equals(style.getParProperty(XMLString.STYLE_WRITING_MODE, true));
	
		// Get the padding
		String sPaddingLeft = getGraphicProperty(style,XMLString.FO_PADDING_LEFT);
		String sPaddingRight = getGraphicProperty(style,XMLString.FO_PADDING_RIGHT);
		String sPaddingTop = getGraphicProperty(style,XMLString.FO_PADDING_TOP);
		String sPaddingBottom = getGraphicProperty(style,XMLString.FO_PADDING_BOTTOM);

		// Create node options
		CSVList options = new CSVList(",","=");
		
		// Get the padding - the text area must be shrinked slightly by these amounts
		// Note that padding is a length with unit, not a number within the coordinate system defined by the view box
		
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
		if (bForceWrap || "wrap".equals(getGraphicProperty(style,XMLString.FO_WRAP_OPTION))) {
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
		
		// Nodes should always be transformed
		options.addValue("transform shape");
		
		// Finally create the node (as a separate path, as the text does not belong to a specific path)
		// Vertical writing implies a further rotation of 90 degrees
		double dAnchorX = Calc.length2cm(sAnchorX);
		double dAnchorY = Calc.length2cm(sAnchorY);
		
		if (Math.abs(dAngle)<0.01) { // No rotation
			ba.add("\\path ("+format(dAnchorX)+","+format(dAnchorY)+") node["+options.toString()+"] {","};");
		}
		else {
			String sMidX = Calc.multiply(0.5F, Calc.add(sLeft, sRight));
			String sMidY = Calc.multiply(0.5F, Calc.add(sTop, sBottom));
			ba.add("\\path[rotate around={"+format(dAngle)+":("+sMidX+","+sMidY+")}] ("
					+format(dAnchorX)+","+format(dAnchorY)+") node["+options.toString()+"] {","};");			
		}
		if (!baTextFormat.getBefore().isEmpty()) {
			ba.addBefore(baTextFormat.getBefore()+" ");
		}
	}
		
	// Convert fill color, fill opacity and even odd rule
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
	
	// Convert path style, width, color and opacity
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
	
	// Convert arrow style (simplistic version not taking the actual arrow style into account)
	private void applyArrowOptions(StyleWithProperties style, CSVList props) {
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
	
	// Get a graphic style property, using the default graphic style if a property is not set
	private String getGraphicProperty(StyleWithProperties style, String sProperty) {
		String s = style.getGraphicProperty(sProperty, true);
		return s!=null ? s : ofr.getDefaultFrameStyle().getGraphicProperty(sProperty, false);
	}
	
	// Apply the transformations associated with a shape
	private void applyTransformOptions(Element shape, double dTranslateY, CSVList props) {
		String sTransform = Misc.getAttribute(shape, XMLString.DRAW_TRANSFORM);
		if (sTransform!=null) {
			SimpleInputBuffer in = new SimpleInputBuffer(sTransform);
			in.skipSpaces();
			while (!in.atEnd()) {
				if (Character.isLetter(in.peekChar())) {
					String s = in.getIdentifier();
					switch (s) {
					case "rotate": applyRotate(in, dTranslateY, props); break;
					case "translate": applyTranslate(in, props); break;
					case "skewX": applySkewX(in, props); break;
					case "skewY":
					case "scale":
					case "matrix": ignoreCommand(in); break;
					default: // Unhandled transformation, give up
						throw new IllegalArgumentException("Syntax error in draw:transform");
					}
				}
				else { // Syntax error, give up
					throw new IllegalArgumentException("Syntax error in draw:transform");
				}
				in.skipSpaces();
			}
		}
	}
	
	// Apply rotation
	// Syntax is rotate(<double>)
	private void applyRotate(SimpleInputBuffer in, double dTranslateY, CSVList props) {
		in.skipSpaces();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpaces();
			double dAngle = getDouble(in);
			in.skipSpaces();
			if (in.peekChar()==')') {
				in.getChar();
				// We must rotate around the upper left corner (which is (0,0) in ODF)
				props.addValue("rotate around", "{"+format(dAngle*180.0/Math.PI)+":(0,"+format(dTranslateY)+")}");
				return;
			}
			throw new IllegalArgumentException("Syntax error in draw:transform, expected ')'");
		}
		throw new IllegalArgumentException("Syntax error in draw:transform, expected '('");
	}
	
	// Apply translation
	// Syntax is translate(<double><unit> [<double><unit>])
	private void applyTranslate(SimpleInputBuffer in, CSVList props) {
		in.skipSpaces();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpaces();
			double dTranslateX = getLength(in);
			double dTranslateY = 0;
			in.skipSpaces();
			if (in.peekChar()!=')') { // Must have a y coordinate too
				dTranslateY = getLength(in);
				in.skipSpaces();
			}
			if (in.peekChar()==')') {
				in.getChar();
				// Change sign because of upside down y-axis in ODF
				String s = format(-dTranslateY);
				if (!s.equals("0")) {
					props.addValue("yshift", s+"cm");					
				}
				props.addValue("xshift", format(dTranslateX)+"cm");
				return;
			}
			throw new IllegalArgumentException("Syntax error in draw:transform, expected ')'");
		}
		throw new IllegalArgumentException("Syntax error in draw:transform, expected '('");		
	}
	
	// Apply skewing along the x-axis
	// Syntax is skewX(<double>)
	private void applySkewX(SimpleInputBuffer in, CSVList props) {
		in.skipSpaces();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpaces();
			double dAngle = getDouble(in);
			in.skipSpaces();
			if (in.peekChar()==')') {
				in.getChar();
				// TikZ uses a slant factor, x is replaced with x+k*y, which implies tan(v)=k
				props.addValue("xslant",format(Math.tan(dAngle)));
				return;
			}
			throw new IllegalArgumentException("Syntax error in draw:transform, expected ')'");
		}
		throw new IllegalArgumentException("Syntax error in draw:transform, expected '('");		
	}
	
	// Skip an unsupported transformation command
	private void ignoreCommand(SimpleInputBuffer in) {
		in.skipSpaces();
		if (in.peekChar()=='(') {
			in.getChar();
			while (!in.atEnd() && in.peekChar()!=')') {
				in.getChar();
			}
			if (!in.atEnd() && in.peekChar()==')') {
				in.getChar();
			}
		}
	}
	
	private double getLength(SimpleInputBuffer in) {
		double dValue = getDouble(in);
		String sUnit = in.getIdentifier();
		if (!sUnit.isEmpty()) {
			return Calc.length2cm(dValue+sUnit);
		}
		throw new IllegalArgumentException("Syntax error in draw:transform, expected length");
	}
	
	private double getDouble(SimpleInputBuffer in) {
		String s = in.getSignedDouble();
		if (s.length()>0) {
			return Double.parseDouble(s);
		}
		throw new IllegalArgumentException("Syntax error in draw:transform, expected double");
	}
	
}
