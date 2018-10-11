/************************************************************************
 *
 *  CustomShapeConverter.java
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
 *  Version 2.0 (2018-10-10)
 *
 */

package writer2latex.latex.tikz;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/** Convert draw:custom-shape elements to TikZ pictures
 */
class CustomShapeConverter extends ShapeWithViewBoxConverterHelper {
	
	// In LO some specific shapes have subpaths which are in a darker or brighter shade
	// This is *not* part of ODF, but we emulate the LO behavior for these shapes
	private Map<String,Short[]> subpathShades = new HashMap<>();
	
	// Parameters for the current shape derived from the draw:enhanced-geometry element
	private double dStretchX=0;
	private double dStretchY=0;
	private Vector<Double> modifiers = new Vector<>();
	private Map<String,String> equations = new HashMap<>();
	private Map<String,Double> equationResults = new HashMap<>();
	
	// State while parsing the current path
	private double dCurrentX; // x-coordinate of current point
	private double dCurrentY; // y-coordinate of current point
	private boolean bNostroke; // This set of subpaths have specified the nostroke command S
	private boolean bNofill; // This set of subpaths have specified the nofill command F
	
	private boolean bStretchX;
	private boolean bStretchY;
	private double dUnstretchedWidth;
	private double dUnstretchedHeight;

	CustomShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
		// Define LO shades for specific shades (other shapes are not shaded)
		// The shades are ordered by sub path
		// - Zero means original color
		// - Positive means shade with white, i.e. color!number
		// - Negative means shade with black, i.e. color!(-number)!black
		// The actual numbers were found by probing a screenshot from LO
		subpathShades.put("can", new Short[]{0, 60}); // UI name: ?
		subpathShades.put("cube", new Short[]{0, 80, -80}); // UI name: Cube
		subpathShades.put("paper", new Short[]{0, -80}); // UI name: Folded Corner
		subpathShades.put("smiley", new Short[]{0, -80, -80}); // UI name: Smile Face
		subpathShades.put("quad-bevel", new Short[]{0, 80, -60, -80, 60}); // UI name: Square Bevel
		subpathShades.put("col-502ad400", new Short[]{0, 80, -70, 60, 60}); // UI name: Diamond Bevel
		subpathShades.put("col-60da8460", new Short[]{0, -70, -20, 40, 40, 40}); // UI name: Octagon Bevel
		subpathShades.put("vertical-scroll", new Short[]{0, -80, -80}); // UI name: Vertical Scroll
		subpathShades.put("horizontal-scroll", new Short[]{0, -80, -80}); // UI name: Horizontal Scroll
	}

	@Override
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		super.handleShapeInner(shape, dTranslateY, ldp, oc);
		
		Short[] nShades = null;

		// Parse the path parameters and formulas
		Element geometry = Misc.getChildByTagName(shape, XMLString.DRAW_ENHANCED_GEOMETRY);
		if (geometry!=null) {
			try {
				parseViewBox(geometry);
				parseStretchPoints(geometry);
				parseModifiers(geometry);
				parseEquations(geometry);
				parseTextAreas(geometry);
				// Get the LO specific shades, if any
				String sType = Misc.getAttribute(geometry, XMLString.DRAW_TYPE);
				if (sType!=null && subpathShades.containsKey(sType)) {
					nShades = subpathShades.get(sType);
				}
			}
			catch (Exception e) {
				System.err.println("Error "+e.getMessage());
				return;
			}
		}

		// Determine stretching. If the shape should be stretched, we expand the view box accordingly.
		bStretchX=false;
		bStretchY=false;
		dUnstretchedWidth = dWidth;
		dUnstretchedHeight = dHeight;
		double dViewBoxAspect = dViewBoxWidth/dViewBoxHeight;
		double dShapeAspect = dWidth/dHeight;
		if (dStretchX>0.1 && (dViewBoxAspect < dShapeAspect)) {
			bStretchX=true;
			dUnstretchedWidth = dViewBoxAspect/dShapeAspect*dWidth;
		}
		else if (dStretchY>0.1 && (dViewBoxAspect > dShapeAspect)) {
			bStretchY=true;
			dUnstretchedHeight = dShapeAspect/dViewBoxAspect*dHeight;
		}
		
		// Finally convert the path
		String sPath = Misc.getAttribute(geometry, XMLString.DRAW_ENHANCED_PATH);
		if (sPath!=null) {
			// Remember the original fill color
			String sFill = fillOptions.getValue("fill");
			// We need to keep track of the sub paths in order to apply shading
			int nSubPath = 0;
			// Parse the path
			SimpleInputBuffer in = new SimpleInputBuffer(sPath);
			in.skipSpaces();
			while (!in.atEnd()) {
				StringBuilder tikz = new StringBuilder();
				try {
					convertSubPath(in, tikz);
					CSVList options = new CSVList(",","=");
					// Use shade or original fill color
					if (sFill!=null) {
						fillOptions.addValue("fill",sFill);
						if (nShades!=null && nSubPath<nShades.length) {
							if (nShades[nSubPath]>0) { // Shade with white
								fillOptions.addValue("fill",sFill+"!"+nShades[nSubPath]);
							}
							else if (nShades[nSubPath]<0) { // Shade with black
								fillOptions.addValue("fill",sFill+"!"+(-nShades[nSubPath])+"!black");
							}
						}
					}
					if (!bNostroke) options.addValues(strokeOptions);
					if (!bNofill) options.addValues(fillOptions);
					startPath(ldp,options);
					ldp.append(tikz.toString());
					endPath(ldp);
					nSubPath++;
				}
				catch (Exception e) {
					System.err.println("Error "+e.getMessage());
					break;
				}
			}
			// Add text node
			convertText(shape,ldp,oc);
		}
	}
	
	// Parse the draw:text-areas attribute for the current enhanced geometry
	// It is a space separated string of four values representing left, top, right and bottom in that order
	// (Another set for LTR may follow the first four values, this is currently ignored)
	private void parseTextAreas(Element geometry) {
		// Get and parse the property
		String sTextAreas = Misc.getAttribute(geometry, XMLString.DRAW_TEXT_AREAS);
		if (sTextAreas!=null) {
			SimpleInputBuffer in = new SimpleInputBuffer(sTextAreas);
			in.skipSpaces();
			dTextAreaLeft = getParameter(in);
			dTextAreaTop = getParameter(in);
			dTextAreaRight = getParameter(in);
			dTextAreaBottom = getParameter(in);
		}
	}

	
	// Convert draw:enhanced-path to a TikZ path
	private void convertSubPath(SimpleInputBuffer in, StringBuilder tikz) {
		dCurrentX = 0;
		dCurrentY = 0;
		bNofill=false;
		bNostroke=false;
		while (!in.atEnd()) {
			if (Character.isLetter(in.peekChar())) {
				char cCommand = in.getChar();
				skipSpacing(in);
				switch (cCommand) {
				case 'A': convertArc(in, tikz, false, false); break; // arcto: Counter-clockwise, no moveto
				case 'B': convertArc(in, tikz, false, true); break; // arc: Counter-clockwise, with moveto
				case 'C': convertCurveto(in, tikz); break;
				case 'F': bNofill=true; break; 
				case 'L': convertLineto(in, tikz); break; 
				case 'M': convertMoveto(in, tikz); break;
				case 'N': return;
				case 'Q': convertQuadraticcurveto(in, tikz); break;
				case 'S': bNostroke=true; break; 
				case 'T': convertAngleellipse(in, tikz, false); break; // ellipseto: No moveto
				case 'U': convertAngleellipse(in, tikz, true); break; // ellipse: With moveto
				case 'V': convertArc(in, tikz, true, true); break;// clocwisearc: Clockwise, with moveto 
				case 'W': convertArc(in, tikz, true, false); break; // clocwisearcto: Clockwise, no moveto
				case 'X': convertEllipticalquadrant(in, tikz, true); break; // ellipticalquadrantx: Tangential to x-axis at first start 
				case 'Y': convertEllipticalquadrant(in, tikz, false); break; // ellipticalquadranty: Tangential to y-axis at first start
				case 'Z': convertClosepath(in, tikz); break; 
				default: throw new IllegalArgumentException("Unknown path command "+cCommand); 
				}
			}
			else {
				throw new IllegalArgumentException("Command expected at "+in.peekChar());
			}
		}
	}
	
	// Path command M (moveto)
	private void convertMoveto(SimpleInputBuffer in, StringBuilder tikz) {
		double dX=getParameter(in);
		double dY=getParameter(in);
		tikz.append(" ").append(transformPoint(dX,dY));
		dCurrentX = dX; dCurrentY = dY;
		convertLineto(in, tikz);
	}
	
	// Path command L (lineto)
	private void convertLineto(SimpleInputBuffer in, StringBuilder tikz) {
		// Accept empty parameters for easy reuse in convertMoveto...
		while (isParameterStart(in.peekChar())) {
			double dX=getParameter(in);
			double dY=getParameter(in);
			tikz.append(" -- ").append(transformPoint(dX,dY));
			dCurrentX = dX; dCurrentY = dY;
		}
	}
	
	// Path commands A (arcto), B (arc), W (clockwisearcto) and V (clockwisearc)
	private void convertArc(SimpleInputBuffer in, StringBuilder tikz, boolean bClockwise, boolean bMoveto) {
		do {
			double dX1=getParameter(in); // (x1,y1) is first point in bounding box for ellipse
			double dY1=getParameter(in);
			double dX2=getParameter(in); // (x2,y2) is second point in bounding box for ellipse
			double dY2=getParameter(in);
			double dX3=getParameter(in); // (x3,y3) is a vector defining the start angle the spec says
			double dY3=getParameter(in); // reality says it is the start point
			double dX4=getParameter(in); // (x4,y4) is a vector defining the end angle the spec says
			double dY4=getParameter(in); // reality says it is the end point
			// Translate to parameters needed by TikZ
			double dCenterX = (dX1+dX2)/2.0;
			double dCenterY = (dY1+dY2)/2.0;
			double dRadiusX = Math.abs(dX1-dX2)/2.0;
			double dRadiusY = Math.abs(dY1-dY2)/2.0;
			double dStartAngle = Math.atan2(dY3-dCenterY, dX3-dCenterX)*180/Math.PI;
			double dEndAngle = Math.atan2(dY4-dCenterY, dX4-dCenterX)*180/Math.PI;
			// In TikZ, orientation of the path depends on the angles
			// If start angle<end angle, counter clockwise orientation is used
			// If start angle>end angle, clockwise orientation is used
			// We need to do it the other way round here because of the upside down y axis in ODF
			if (!bClockwise && (dStartAngle<dEndAngle)) {
				dEndAngle-=360;
			}
			else if (bClockwise && (dStartAngle>dEndAngle)) {
				dStartAngle-=360;
			}
			if (bMoveto) {
				tikz.append(" ").append(transformPoint(dX3,dY3));
			}
			// Create options list for the arc command
			CSVList options = new CSVList(",","=");
			options.addValue("x radius",format(transformLengthX(dRadiusX)));
			options.addValue("y radius",format(transformLengthY(dRadiusY)));
			options.addValue("start angle",transformAngle(dStartAngle));
			options.addValue("end angle",transformAngle(dEndAngle));
			tikz.append(" arc [").append(options.toString()).append("]");
			dCurrentX = dX4; dCurrentY = dY4;
		} while (isParameterStart(in.peekChar()));
	}
	
	// Path commands T (ellipseto) and U (ellipse)
	private void convertAngleellipse(SimpleInputBuffer in, StringBuilder tikz, boolean bMoveto) {
		do {
			double dCenterX=getParameter(in); // The center of the ellipse
			double dCenterY=getParameter(in);
			double dRadiusX=getParameter(in); // The x-radius of the ellipse
			double dRadiusY=getParameter(in); // The y-radius of the ellipse
			double dStartAngle=getParameter(in); // The start angle of the arc
			double dEndAngle=getParameter(in); // The end angle of the arc
			// In TikZ, orientation of the path depends on the angles
			// If start angle>end angle, clockwise orientation is used
			// We need to do it the other way round here because of the upside down y axis in ODF
			// Note that we prefer negative angles to get positive angles after transformation
			if (Math.abs(dEndAngle-dStartAngle)>360) {
				// LO sometimes exports this (seen in cloud callout shape) - replaces with sensible values
				dStartAngle = 0; dEndAngle = -360;
			}
			else {
				dStartAngle-=360;
				if (dStartAngle<dEndAngle) {
					dEndAngle-=360;
				}
			}
			if (bMoveto) {
				tikz.append(" ").append(transformPoint(
					dCenterX+dRadiusX*Math.cos(dStartAngle*Math.PI/180.0),
					dCenterY+dRadiusY*Math.sin(dStartAngle*Math.PI/180.0)));
			}
			// Create options list for the arc command
			CSVList options = new CSVList(",","=");
			options.addValue("x radius",format(transformLengthX(dRadiusX)));
			options.addValue("y radius",format(transformLengthY(dRadiusY)));
			options.addValue("start angle",transformAngle(dStartAngle));
			options.addValue("end angle",transformAngle(dEndAngle));
			tikz.append(" arc [").append(options.toString()).append("]");
			dCurrentX = dCenterX+dRadiusX*Math.cos(dEndAngle*Math.PI/180.0);
			dCurrentY = dCenterY+dRadiusY*Math.sin(dEndAngle*Math.PI/180.0);

		} while (isParameterStart(in.peekChar()));
	}	
	
	// Path command C (curveto)
	private void convertCurveto(SimpleInputBuffer in, StringBuilder tikz) {
		do {
			double dX1=getParameter(in);
			double dY1=getParameter(in);
			double dX2=getParameter(in);
			double dY2=getParameter(in);
			double dX=getParameter(in);
			double dY=getParameter(in);
			// Syntax for bezier curves is ".. controls (x1,y1) and (x2,y2) .. (x,y)"
			tikz.append(" .. controls ").append(transformPoint(dX1,dY1))
			    .append(" and ").append(transformPoint(dX2,dY2))
			    .append(" .. ").append(transformPoint(dX,dY));
			dCurrentX = dX; dCurrentY = dY;
		} while (isParameterStart(in.peekChar()));
	}
	
	// Path command Q (quadraticcurveto)
	private void convertQuadraticcurveto(SimpleInputBuffer in, StringBuilder tikz) {
		do {
			double dX1=getParameter(in);
			double dY1=getParameter(in);
			double dX=getParameter(in);
			double dY=getParameter(in);
			// Syntax for quadratic bezier curves is ".. controls (x1,y1) .. (x,y)"
			tikz.append(" .. controls ").append(transformPoint(dX1,dY1))
			    .append(" .. ").append(transformPoint(dX,dY));
			dCurrentX = dX; dCurrentY = dY;
		} while (isParameterStart(in.peekChar()));
	}
	
	// Path commands X (ellipticalquadrantx) and Y (ellipticalquadranty)
	private void convertEllipticalquadrant(SimpleInputBuffer in, StringBuilder tikz, boolean bTangentX) {
		int nCount = bTangentX ? 0 : 1;
		do {
			double dX=getParameter(in);
			double dY=getParameter(in);
			double dDeltaX = dX-dCurrentX;
			double dDeltaY = dY-dCurrentY;
			double dStart=0,dEnd=0;
			if ((nCount++)%2==0) { // x axis is tangent at starting point
				if (dDeltaX>0 && dDeltaY>0) {
					dStart=90; dEnd=0;
				}
				else if (dDeltaX>0 && dDeltaY<0) {
					dStart=270; dEnd=360;					
				}
				else if (dDeltaX<0 && dDeltaY>0) {
					dStart=90; dEnd=180;					
				}
				else if (dDeltaX<0 && dDeltaY<0) {
					dStart=270; dEnd=180;					
				}
			}
			else { // y axis is tangent at starting point
				if (dDeltaX>0 && dDeltaY>0) {
					dStart=180; dEnd=270;
				}
				else if (dDeltaX>0 && dDeltaY<0) {
					dStart=180; dEnd=90;					
				}
				else if (dDeltaX<0 && dDeltaY>0) {
					dStart=360; dEnd=270;					
				}
				else if (dDeltaX<0 && dDeltaY<0) {
					dStart=0; dEnd=90;				
				}				
			}
			CSVList options = new CSVList(",","=");
			options.addValue("x radius", format(transformLengthX(Math.abs(dDeltaX))));
			options.addValue("y radius", format(transformLengthY(Math.abs(dDeltaY))));
			options.addValue("start angle",format(dStart));
			options.addValue("end angle",format(dEnd));
			tikz.append(" arc[").append(options.toString()).append("]");
			dCurrentX = dX; dCurrentY = dY;
		} while (isParameterStart(in.peekChar()));
	}
	
	// Path command Z (closepath)
	private void convertClosepath(SimpleInputBuffer in, StringBuilder tikz) {
		tikz.append(" -- cycle");
		// No need to set current point as this would be the end of the subpath
	}
	
	private double getParameter(SimpleInputBuffer in) {
		if (in.peekChar()=='?') {
			double dValue = functionReference(in);
			skipSpacing(in);
			return dValue;
		}
		else if (in.peekChar()=='$') {
			double dValue = modifierReference(in);
			skipSpacing(in);
			return dValue;
		}
		else {
			String sNumber = in.getSignedDouble();
			if (sNumber.length()>0) {
				double dValue = Double.parseDouble(sNumber);
				skipSpacing(in);
				return dValue;
			}
		}
		throw new IllegalArgumentException("Syntax error in path, expected parameter");
	}
	
	private boolean isParameterStart(char c) {
		return (c>='0' && c<='9') || c=='+' || c=='-' || c=='.' || c=='?' || c=='$';
	}
	
	private void skipSpacing(SimpleInputBuffer in) {
		in.skipSpaces();
		if (in.peekChar()==',') in.getChar();
		in.skipSpaces();		
	}
	
	// Parse the draw:path-stretchpoint-* attributes, which are double values
	private void parseStretchPoints(Element geometry) {
		dStretchX=0;
		String sStretchX = Misc.getAttribute(geometry, XMLString.DRAW_PATH_STRETCHPOINT_X);
		if (sStretchX!=null) {
			dStretchX = Double.parseDouble(sStretchX);
		}
		dStretchY=0;
		String sStretchY = Misc.getAttribute(geometry, XMLString.DRAW_PATH_STRETCHPOINT_Y);
		if (sStretchY!=null) {
			dStretchY = Double.parseDouble(sStretchY);
		}
	}
	
	// Parse the draw:modifiers attribute for the current enhanced geometry
	// draw:modifiers is a list of space separated floating point values
	// We follow the grammar from the ODF spec, except that we accept more spaces
	private void parseModifiers(Element geometry) {
		modifiers.clear();
		String sModifiers = Misc.getAttribute(geometry, XMLString.DRAW_MODIFIERS);
		if (sModifiers!=null) {
			SimpleInputBuffer in = new SimpleInputBuffer(sModifiers);
			in.skipSpaces();
			while (!in.atEnd()) {
				String sValue = in.getSignedDouble();
				if (sValue.length()>0) {
					modifiers.add(Double.parseDouble(sValue));
				}
				else { // Something's wrong, don't read any further
					return;
				}
				in.skipSpaces();
			}
		}
	}
	
	// Parse the draw:equation elements in the current enhanced geometry

	private void parseEquations(Element geometry) {
		equations.clear();
		equationResults.clear();
		// Read the equations
		Node child = geometry.getFirstChild();
		while (child!=null) {
			if (Misc.isElement(child, XMLString.DRAW_EQUATION)) {
				String sName = Misc.getAttribute(child, XMLString.DRAW_NAME);
				String sFormula = Misc.getAttribute(child, XMLString.DRAW_FORMULA);
				if (sName!=null && sFormula!=null) {
					equations.put(sName, sFormula);
				}
			}
			child=child.getNextSibling();
		}
		// Note that we defer calculation; we may not need the result and LO sometimes
		// export (unused) formulas referring to non-existent parameters
	}
	
	private double calculateEquation(String sName) {
		if (!equationResults.containsKey(sName)) {
			SimpleInputBuffer in = new SimpleInputBuffer(equations.get(sName));
			equationResults.put(sName, additiveExpression(in));
			if (!in.atEnd()) {
				throw new IllegalArgumentException("Syntax error: Unexpected character '"+in.peekChar()+"' at index "+in.getIndex());
			}
		}
		return equationResults.get(sName);
	}
	
	// Parse and calculate formulas based on the grammer in the ODF spec
	
	private double additiveExpression(SimpleInputBuffer in) {
		double dSum = multiplicativeExpression(in);
		in.skipSpacesAndTabs();
		while (in.peekChar()=='+' || in.peekChar()=='-') {
			if (in.peekChar()=='+') {
				in.getChar();
				in.skipSpacesAndTabs();
				dSum+=multiplicativeExpression(in);
			}
			else if (in.peekChar()=='-') {
				in.getChar();
				in.skipSpacesAndTabs();
				dSum-=multiplicativeExpression(in);				
			}
			in.skipSpacesAndTabs();
		}
		return dSum;
	}
	
	private double multiplicativeExpression(SimpleInputBuffer in) {
		double dFactor = unaryExpression(in);
		in.skipSpacesAndTabs();
		while (in.peekChar()=='*' || in.peekChar()=='/') {
			if (in.peekChar()=='*') {
				in.getChar();
				in.skipSpacesAndTabs();
				dFactor*=unaryExpression(in);
			}
			else if (in.peekChar()=='/') {
				in.getChar();
				in.skipSpacesAndTabs();
				dFactor/=unaryExpression(in);				
			}
			in.skipSpacesAndTabs();
		}
		return dFactor;
	}
	
	private double unaryExpression(SimpleInputBuffer in) {
		if (in.peekChar()=='-') {
			in.getChar();
			in.skipSpaces();
			return -basicExpression(in);
		}
		else {
			return basicExpression(in);
		}
	}
	
	private double basicExpression(SimpleInputBuffer in) {
		if (Character.isLetter(in.peekChar())) {
			String sName = in.getIdentifier();
			switch (sName) {
		    // identifier
			case "pi": return Math.PI;
			case "left": return dViewBoxMinX;
			case "top": return dViewBoxMinY;
			case "right": return dViewBoxMinX+dViewBoxWidth;
			case "bottom": return dViewBoxMinY+dViewBoxHeight;
			case "xstretch": return dStretchX;
			case "ystretch": return dStretchY;
			case "hasstroke": return bHasstroke?1:0;
			case "hasfill": return bHasfill?1:0;
			case "width": return dViewBoxWidth;
			case "height": return dViewBoxHeight;
			// The interpretation of logheight and logwidth is unclear from the ODF spec
			// This report indicates that it might be as follows:
			// https://issues.oasis-open.org/browse/OFFICE-3778
			case "logwidth": return dWidth*10000;
			case "logheight": return dHeight*10000;
			// unary function
			case "abs":
			case "sqrt":
			case "sin":
			case "cos":
			case "tan":
			case "atan": return unaryFunction(sName, in);
			// binary function
			case "min":
			case "max":
			case "atan2": return binaryFunction(sName, in);
			// ternary function
			case "if": return ternaryFunction(sName, in);
			// Other values indicates an error
			default: throw new IllegalArgumentException("Syntax error: Unexpected identifier '"+sName+"'");
			}
		}
		else if (in.peekChar()=='?') {
			return functionReference(in);
		}
		else if (in.peekChar()=='$') {
			return modifierReference(in);
		}
		else if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpacesAndTabs();
			double term = additiveExpression(in);
			in.skipSpacesAndTabs();
			if (in.peekChar()==')') {
				in.getChar();
				return term;
			}
			else {
				throw new IllegalArgumentException("Syntax error: ')' expected");
			}
		}
		else { // Must be a number
			String sNumber = in.getSignedDouble();
			if (sNumber.length()>0) {
				return Double.parseDouble(sNumber);
			}
			throw new IllegalArgumentException("Syntax error: Unexpected character '"+in.peekChar()+"' at index "+in.getIndex());
		}
	}
	
	// According to the ODF spec, trigonometric functions use degrees.
	// This is an error according to
	// https://issues.oasis-open.org/browse/OFFICE-3823
	private double unaryFunction(String sName, SimpleInputBuffer in) {
		in.skipSpacesAndTabs();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpacesAndTabs();
			double dArgument = additiveExpression(in);
			in.skipSpacesAndTabs();
			if (in.peekChar()==')') {
				in.getChar();
				switch(sName) {
				case "abs": return Math.abs(dArgument);
				case "sqrt": return Math.sqrt(dArgument);
				case "sin": return Math.sin(dArgument);
				case "cos": return Math.cos(dArgument);
				case "tan": return Math.tan(dArgument);
				case "atan": return Math.atan(dArgument);
				}
			}
		}
		throw new IllegalArgumentException("Syntax error: Bad argument to '"+sName+"'");
	}
	
	// According to the ODF spec, atan2 uses the argument order atan2(x,y) and not the more common atan2(y,x)
	// This is an arror according to
	// https://issues.oasis-open.org/browse/OFFICE-3822
	private double binaryFunction(String sName, SimpleInputBuffer in) {
		in.skipSpacesAndTabs();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpacesAndTabs();
			double dArgument1 = additiveExpression(in);
			in.skipSpacesAndTabs();
			if (in.peekChar()==',') {
				in.getChar();
				in.skipSpacesAndTabs();
				double dArgument2 = additiveExpression(in);
				in.skipSpacesAndTabs();
				if (in.peekChar()==')') {
					in.getChar();
					switch(sName) {
					case "min": return Math.min(dArgument1, dArgument2);
					case "max": return Math.max(dArgument1, dArgument2);
					case "atan2": return Math.atan2(dArgument1, dArgument2);
					}
				}
			}
		}
		throw new IllegalArgumentException("Syntax error: Bad argument to '"+sName+"'");
	}
	
	private double ternaryFunction(String sName, SimpleInputBuffer in) {
		in.skipSpacesAndTabs();
		if (in.peekChar()=='(') {
			in.getChar();
			in.skipSpacesAndTabs();
			double dArgument1 = additiveExpression(in);
			in.skipSpacesAndTabs();
			if (in.peekChar()==',') {
				in.getChar();
				in.skipSpacesAndTabs();
				double dArgument2 = additiveExpression(in);
				in.skipSpacesAndTabs();
				if (in.peekChar()==',') {
					in.getChar();
					in.skipSpacesAndTabs();
					double dArgument3 = additiveExpression(in);
					in.skipSpacesAndTabs();
					if (in.peekChar()==')') {
						in.getChar();
						// The only ternary function is if
						return dArgument1>0 ? dArgument2 : dArgument3;
					}
				}
			}
		}
		throw new IllegalArgumentException("Syntax error: Bad argument to '"+sName+"'");
	}
	
	private double functionReference(SimpleInputBuffer in) {
		in.getChar();
		// The name of a string is supposed to be a sequence of arbitrary characters except spaces and tabs
		// In reality it is an ordinary identifier
		/*String sName = "";
		while (!in.atEnd() && in.peekChar()!=' ' && in.peekChar()!='\u0009') {
			sName+=in.getChar();
		}*/
		String sName = in.getIdentifier();
		if (equations.containsKey(sName)) {
			return calculateEquation(sName);
		}
		throw new IllegalArgumentException("Unknown equation: "+sName);
	}
	
	private double modifierReference(SimpleInputBuffer in) {
		in.getChar();
		if (in.peekChar()>='0' && in.peekChar()<='9') {
			int nIndex = Integer.parseInt(in.getInteger());
			if (nIndex<modifiers.size()) {
				return modifiers.get(nIndex);
			}
		}
		throw new IllegalArgumentException("Uknown modifier index");
	}
	
	// Override coordinate transformations to handle the special stretch feature of custom shapes
	
	// As described in the ODF spec, we first convert to a (fake) shape with the same aspect ratio as the view box
	double transformLengthX(double dX) {
		return dX*dUnstretchedWidth/dViewBoxWidth;
	}
	
	double transformLengthY(double dY) {
		return dY*dUnstretchedHeight/dViewBoxHeight;
	}
	
	// Coordinates in the upper portion of the axis are then translated to the fill the real shape size
	// Strange things will happen if the two halves are connected with anything but straight lines
	// (LO always connects them with vertical and horizontal lines, so we are fine)
	double transformX(double dX) {
		if (bStretchX && dX>dStretchX) {
			return transformLengthX(dX)+dWidth-dUnstretchedWidth;
		}
		else {
			return transformLengthX(dX);
		}
	}
	
	double transformY(double dY) {
		if (bStretchY && dY<dStretchY) {
			return transformLengthY(dViewBoxHeight-dY)-dUnstretchedHeight+dTranslateY;
		}
		else {
			return transformLengthY(dViewBoxHeight-dY)-dHeight+dTranslateY;
		}
	}
	
}
