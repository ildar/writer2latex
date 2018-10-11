/************************************************************************
 *
 *  MeasureShapeConverter.java
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
 *  Version 2.0 (2018-10-09)
 *  
 */
package writer2latex.latex.tikz;

import org.w3c.dom.Element;

import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Calc;

public class MeasureShapeConverter extends ShapeConverterHelper {

	public MeasureShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
    double getMaxY(Element shape) {
		return Math.max(getParameter(shape,XMLString.SVG_Y1), getParameter(shape,XMLString.SVG_Y2));
    }
	
	@Override
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// Get start reference point as (dX1,dY1) and end reference point as (dX2,dY2)
		double dX1 = getParameter(shape,XMLString.SVG_X1);
		double dY1 = getParameter(shape,XMLString.SVG_Y1);
		double dX2 = getParameter(shape,XMLString.SVG_X2);
		double dY2 = getParameter(shape,XMLString.SVG_Y2);
		
		// Get style parameters
		// String sPlacing="above";
		// The default values for all the distances are guesswork based on LO rendering
		double dLineDistance = 1.5;
		double dGuideDistance = 0.3;
		double dGuideOverhang = 0.3;
		double dStartGuide = 0;
		double dEndGuide = 0;
		
		StyleWithProperties style = ofr.getFrameStyle(shape.getAttribute(XMLString.DRAW_STYLE_NAME));
		if (style!=null) {
			// Placing can be "above" or "below"
			// sPlacing = getGraphicProperty(style,XMLString.DRAW_PLACING);	
			// The distance between the measure line and the line between the reference points
			dLineDistance = getStyleAttribute(style,XMLString.DRAW_LINE_DISTANCE,dLineDistance);
			// The distance between the reference points and the start point of extension lines
			dGuideDistance = getStyleAttribute(style,XMLString.DRAW_GUIDE_DISTANCE,dGuideDistance);
			// The length of extension lines after their intersection with a dimension line
			dGuideOverhang = getStyleAttribute(style,XMLString.DRAW_GUIDE_OVERHANG,dGuideOverhang);
			// A length that is added to the length of a start extension line.
			// The extension line is extended by this length towards the start reference point 
			dStartGuide = getStyleAttribute(style,XMLString.DRAW_START_GUIDE,dStartGuide);
			// A length that is added to the length of the end extension line.
			// The extension line is extended by this length towards the end reference point
			dEndGuide = getStyleAttribute(style,XMLString.DRAW_END_GUIDE,dEndGuide);
		}
			
		// Calculate a unit vector in the direction from the reference line to the measure line
		double dLength = Math.sqrt((dX2-dX1)*(dX2-dX1)+(dY2-dY1)*(dY2-dY1));
		double dVx = (dY1-dY2)/dLength;
		double dVy = (dX2-dX1)/dLength;
		
		// Calculate the start and end points of the measure line
		// Whether to add or subtract should depend on draw:placing, but LO seems not to export this?
		// This seems to work, anyway...
		double dMx1 = dX1-dLineDistance*dVx;
		double dMy1 = dY1-dLineDistance*dVy;
		double dMx2 = dX2-dLineDistance*dVx;
		double dMy2 = dY2-dLineDistance*dVy;
		// Draw the measure line
		startPath(ldp,strokeOptions,arrowOptions);
		ldp.append(point(dMx1,dTranslateY-dMy1)).append(" --").append(point(dMx2,dTranslateY-dMy2));
		endPath(ldp);
		
		// Calculate the start and end points of the extension lines (same principles as for the measure line)
		double dE1x1 = dX1-(dGuideDistance-dStartGuide)*dVx;
		double dE1y1 = dY1-(dGuideDistance-dStartGuide)*dVy;
		double dE1x2 = dX1-(dLineDistance+dGuideOverhang)*dVx;
		double dE1y2 = dY1-(dLineDistance+dGuideOverhang)*dVy;
		double dE2x1 = dX2-(dGuideDistance-dEndGuide)*dVx;
		double dE2y1 = dY2-(dGuideDistance-dEndGuide)*dVy;
		double dE2x2 = dX2-(dLineDistance+dGuideOverhang)*dVx;
		double dE2y2 = dY2-(dLineDistance+dGuideOverhang)*dVy;
		// Draw extension lines
		startPath(ldp,strokeOptions);
		ldp.append(point(dE1x1,dTranslateY-dE1y1)).append(" --").append(point(dE1x2,dTranslateY-dE1y2));
		ldp.append(point(dE2x1,dTranslateY-dE2y1)).append(" --").append(point(dE2x2,dTranslateY-dE2y2));
		endPath(ldp);
		
		// Add text node
		// Eg. \path[rotate around={-30:(2.04,1.158)}] (2.04,1.158) node[transform shape,align=center] {Text!};
		double dTx1 = dX1-(dLineDistance-0.3)*dVx;
		double dTy1 = dY1-(dLineDistance-0.3)*dVy;
		double dTx2 = dX2-(dLineDistance-0.3)*dVx;
		double dTy2 = dY2-(dLineDistance-0.3)*dVy;

		String sMidX = ((dTx1+dTx2)/2)+"cm";
		String sMidY = ((2*dTranslateY-dTy1-dTy2)/2)+"cm";
		String sLength = Math.sqrt((dTx2-dTx1)*(dTx2-dTx1)+(dTy2-dTy1)*(dTy2-dTy1))+"cm";
		double dAngle = 180-Math.atan2(dTy2-dTy1, dTx2-dTx1)*180.0/Math.PI;
		convertText(shape,shape,
				Calc.add(sMidY, "0.1cm"),
				Calc.add(sMidX, Calc.multiply(0.5F, sLength)),
				Calc.sub(sMidY, "0.1cm"),
				Calc.sub(sMidX, Calc.multiply(0.5F, sLength)),
				dAngle,false,
				ldp,oc);

	}
	
	private double getStyleAttribute(StyleWithProperties style, String sXML, double dDefault) {
		String s = getGraphicProperty(style,sXML);
		return s!=null ? Calc.length2cm(s) : dDefault;
	}

}
