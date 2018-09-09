/************************************************************************
 *
 *  LineShapeConverter.java
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
 *  Version 2.0 (2018-09-09)
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

class LineShapeConverter extends ShapeConverterHelper {

	LineShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
    double getMaxY(Element shape) {
		return Math.max(getParameter(shape,XMLString.SVG_Y1), getParameter(shape,XMLString.SVG_Y2));
    }
	
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		ldp.append("\\path");

		// Apply style properties
		CSVList options = new CSVList(",","=");
		options.addValues(strokeOptions);
		options.addValues(arrowOptions);
		if (!options.isEmpty()) {
			ldp.append("[").append(options.toString()).append("]");
		}

		// Convert path
		double dX1 = getParameter(shape,XMLString.SVG_X1);
		double dY1 = getParameter(shape,XMLString.SVG_Y1);
		double dX2 = getParameter(shape,XMLString.SVG_X2);
		double dY2 = getParameter(shape,XMLString.SVG_Y2);
		ldp.append("(").append(format(dX1)).append(",").append(format(dTranslateY-dY1)).append(")--(")
			.append(format(dX2)).append(",").append(format(dTranslateY-dY2)).append(");").nl();
		// Add text node
		// Eg. \path[rotate around={-30:(2.04,1.158)}] (2.04,1.158) node[transform shape,align=center] {Text!};
		String sMidX = ((dX1+dX2)/2)+"cm";
		String sMidY = ((2*dTranslateY-dY1-dY2)/2)+"cm";
		String sLength = Math.sqrt((dX2-dX1)*(dX2-dX1)+(dY2-dY1)*(dY2-dY1))+"cm";
		double dAngle = -Math.atan2(dY2-dY1, dX2-dX1)*180.0/Math.PI;
		convertText(shape,
				Calc.add(sMidY, "0.1cm"),
				Calc.add(sMidX, Calc.multiply(0.5F, sLength)),
				Calc.sub(sMidY, "0.1cm"),
				Calc.sub(sMidX, Calc.multiply(0.5F, sLength)),
				dAngle,
				ldp,oc);
	}
		
}
