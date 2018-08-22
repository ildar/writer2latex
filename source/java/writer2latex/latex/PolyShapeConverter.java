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
 *  Version 2.0 (2018-08-22)
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;

import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

public class PolyShapeConverter extends ShapeWithViewBoxConverterHelper {

	PolyShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
	void handleShape(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		super.handleShape(shape, dTranslateY, ldp, oc);
		
		parseViewBox(shape);
	
		// Can either handle a closed polygon (draw:polygon) or and open polyline (draw:polyline)
		// The former can have fill formatting, while the latter can have arrow formatting
		boolean bPolygon = XMLString.DRAW_POLYGON.equals(shape.getTagName());
		
		String sPoints = Misc.getAttribute(shape, XMLString.DRAW_POINTS);
		if (sPoints!=null) {
			SimpleInputBuffer in = new SimpleInputBuffer(sPoints);
			StringBuilder tikz = new StringBuilder();
			convertPath(in,tikz);
			if (tikz.length()>0) {
				ldp.append("\\path");
				CSVList options = new CSVList(",","=");
				options.addValues(strokeOptions);
				if (bPolygon) {
					options.addValues(fillOptions);
				}
				else {
					applyArrowStyle(ofr.getFrameStyle(shape.getAttribute(XMLString.DRAW_STYLE_NAME)),options);
				}
				if (!options.isEmpty()) {
					ldp.append("[").append(options.toString()).append("]");
				}
				ldp.append(tikz.toString());
				if (bPolygon) { ldp.append(" -- cycle"); }
				ldp.append(";").nl();
				// Add text node
				convertText(shape,ldp,oc);
			}
		}
	}
	
	private void convertPath(SimpleInputBuffer in, StringBuilder tikz) {
		boolean bFirst = true;
		in.skipSpaces();
		while (!in.atEnd()) {
			double dX=0,dY=0;
			String s = in.getSignedDouble();
			if (s.length()>0) {
				dX = Double.parseDouble(s);
				if (in.peekChar()==',') {
					in.getChar();
				}
				else { // Syntax error in path; no comma after x-coordinate
					return;
				}
				s = in.getSignedDouble();
				if (s.length()>0) {
					dY = Double.parseDouble(s);
					in.skipSpaces();
					if (!bFirst) {
						tikz.append(" -- ");
					}
					tikz.append(transformPoint(dX,dY));
					bFirst=false;
				}
				else { // Syntax error in path; no y-coordinate
					return;
				}
			}
			in.skipSpaces();
		}
	}

}
