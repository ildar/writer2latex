/************************************************************************
 *
 *  ShapeWithViewBoxConverterHelper.java
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
 *  Version 2.0 (2018-09-23)
 *
 */
package writer2latex.latex.tikz;

import org.w3c.dom.Element;

import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/** This is a base class for TikZ shape converters for draw elements providing a view box
 */
abstract class ShapeWithViewBoxConverterHelper extends ShapeConverterHelper {
	
	// Translation for final y coordinate
	protected double dTranslateY;
	
	// Parameters for the current shape derived from the draw:custom-shape element
	// private double dOffsetX=0; // horizontal position in cm (from svg:x)
	// private double dOffsetY=0; // vertical position in cm (from svg:y)
	protected double dWidth=0; // Width in cm (from svg:width)
	protected double dHeight=0; // Height in cm (from svg:height)
	// Parameters for the current shape
	protected double dViewBoxMinX=0;
	protected double dViewBoxMinY=0;
	protected double dViewBoxWidth=0;
	protected double dViewBoxHeight=0;
	protected double dTextAreaLeft=0;
	protected double dTextAreaTop=0;
	protected double dTextAreaRight=0;
	protected double dTextAreaBottom=0;	
	
	
	ShapeWithViewBoxConverterHelper(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
    double getMaxY(Element shape) {
		return super.getMaxY(shape) + getParameter(shape,XMLString.SVG_HEIGHT);
    }
	
	// The subclass should extend this
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		this.dTranslateY = dTranslateY;

		// Set up global parameters
		//dOffsetX = getParameter(shape,XMLString.SVG_X);
		//dOffsetY = getParameter(shape,XMLString.SVG_Y);
		dWidth = getParameter(shape,XMLString.SVG_WIDTH);
		dHeight = getParameter(shape,XMLString.SVG_HEIGHT);
	}
	
	void convertText(Element shape, LaTeXDocumentPortion ldp, Context oc) {
		convertText(shape,shape,
			format(transformY(dTextAreaTop))+"cm",format(transformX(dTextAreaRight))+"cm",
			format(transformY(dTextAreaBottom))+"cm",format(transformX(dTextAreaLeft))+"cm",
			0.0,false,ldp,oc);
	}

	// Parse the svg:viewBox attribute for the current sjape
	// svg:viewBox, following the SVG spec, is a set of four numbers separated by whitespace and/or a comma.
	// The order of the numbers is <min-x>, <min-y>, <width> and <height>.
	// Numbers in SVG seems to indicate floating point numbers, but the ODF spec says integers?
	void parseViewBox(Element geometry) {
		// LO sometimes exports without a view box. This means trouble, but it seems that LO always use these values?
		dViewBoxMinX=0;
		dViewBoxMinY=0;
		dViewBoxWidth=21600;
		dViewBoxHeight=21600;
		String sViewBox = Misc.getAttribute(geometry, XMLString.SVG_VIEWBOX);
		if (sViewBox!=null) {
			SimpleInputBuffer in = new SimpleInputBuffer(sViewBox);
			in.skipSpaces();
			dViewBoxMinX=parseViewBoxItem(in);
			dViewBoxMinY=parseViewBoxItem(in);
			dViewBoxWidth=parseViewBoxItem(in);
			dViewBoxHeight=parseViewBoxItem(in);
		}
		// The view box is also provides default values for the text area
		dTextAreaLeft = dViewBoxMinX;
		dTextAreaTop = dViewBoxMinY;
		dTextAreaRight = dViewBoxMinX+dViewBoxWidth;
		dTextAreaBottom = dViewBoxMinY+dViewBoxHeight;
	}
	
	private double parseViewBoxItem(SimpleInputBuffer in) {
		String sValue = in.getSignedDouble();
		if (sValue.length()>0) {
			in.skipSpaces();
			if (in.peekChar()==',') in.getChar();
			in.skipSpaces();
			return Double.parseDouble(sValue);
		}
		// Something's wrong
		return 0;
	}
	
	// Transform a horizontal dimension
	double transformLengthX(double dX) {
		return dX*dWidth/dViewBoxWidth;
	}
	
	// Transform a vertical dimension
	double transformLengthY(double dY) {
		return dY*dHeight/dViewBoxHeight;
	}
	
	// Transform x coordinate (just an alias for transformLengthX)
	double transformX(double dX) {
		return transformLengthX(dX); // +dOffsetX;
	}
	
	// Transform y coordinate
	// y coordinate is reversed because we go from an upside down y-axis to a standard y-axis
	double transformY(double dY) {
		return transformLengthY(dViewBoxHeight-dY)-dHeight+dTranslateY; // -dOffsetY
	}
	
	// Transform a point from the coordinate system given by svg:viewBox to the actual coordinate system given
	// by svg:width and svg:height. Return a textual representation of the point in TikZ syntax 
	String transformPoint(double dX, double dY) {
		return "(" + format(transformX(dX)) + "," + format(transformY(dY)) + ")";
	}
		
	// Transform an angle from the coordinate system given by svg:viewBox to the actual coordinate system given
	// by svg:width and svg:height. Return a textual representation of the angle.
	String transformAngle(double dV) {
		double dNewV = Math.atan2(
				transformLengthY(Math.sin(dV*Math.PI/180.0)),
				transformLengthX(Math.cos(dV*Math.PI/180.0))
				)*180.0/Math.PI;
		// If the old angle is outside (-180;180], so must the new angle
		if (dV>180) dNewV+=360;
		if (dV<-180) dNewV-=360;
		// Change sign because we go from an upside down y-axis to a standard y-axis
		return format(-dNewV);
	}
	

}
