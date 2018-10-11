/************************************************************************
 *
 *  EllipseShapeConverter.java
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
 *  Version 2.0 (2018-10-05)
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
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;

public class EllipseShapeConverter extends ShapeConverterHelper {

	public EllipseShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}

	@Override
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		double dWidth = getParameter(shape,XMLString.SVG_WIDTH);
		double dHeight = getParameter(shape,XMLString.SVG_HEIGHT);
		String sKind = Misc.getAttribute(shape, XMLString.DRAW_KIND);
		double dStartAngle = getDouble(shape,XMLString.DRAW_START_ANGLE);
		double dEndAngle = getDouble(shape,XMLString.DRAW_END_ANGLE);
		
		CSVList options = new CSVList(",","=");
		options.addValue("x radius",format(dWidth/2));
		options.addValue("y radius",format(dHeight/2));
		
		if ("arc".equals(sKind)) {
			startPath(ldp,strokeOptions,arrowOptions);
		}
		else { // All other kinds are filled
			startPath(ldp,strokeOptions,fillOptions);
		}
		
		if ("arc".equals(sKind) || "section".equals(sKind) || "cut".equals(sKind)) {
			// LO actually draws the arc from the end point to the start point(!)
			// (This is evident if the arc has arrow heads)
			// Thus start and end changes meaning from ODF to TikZ here:
			// Also, the arc must be drawn in negative orientation, hence the end angle must be smaller than the start angle
			options.addValue("start angle",format(dEndAngle));
			options.addValue("end angle",format(dStartAngle<dEndAngle ? dStartAngle : dStartAngle-360));
			
			double dStartX = dWidth/2 + dWidth/2*Math.cos(-dEndAngle*Math.PI/180.0);
			double dStartY = dTranslateY-(dHeight/2 + dHeight/2*Math.sin(-dEndAngle*Math.PI/180.0));
			
			ldp.append(point(dStartX,dStartY)).append(" arc[").append(options.toString()).append("]");
			
			if ("section".equals(sKind)) { // Add a line to the center
				ldp.append(" --").append(point(dWidth/2,dTranslateY-dHeight/2));
			}
			if ("section".equals(sKind) || "cut".equals(sKind)) { // Add a line to the start point
				ldp.append(" -- cycle");
			}
		}
		else { // default kind is "full", that is a complete ellipse
			ldp.append(point(dWidth/2,dTranslateY-dHeight/2)).append(" ellipse[").append(options.toString()).append("]");
		}
		
		endPath(ldp);

		// Add text node
		convertText(shape,shape,dTranslateY+"cm",dWidth+"cm",(dTranslateY-dHeight)+"cm","0cm",0,false,ldp,oc);
	}
	
	private double getDouble(Element shape, String sXML) {
		String s = Misc.getAttribute(shape, sXML);
		try {
			return s!=null ? Double.parseDouble(s) : 0;
		}
		catch (NumberFormatException e) {
			return 0;
		}
	}

}
