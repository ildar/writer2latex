/************************************************************************
 *
 *  RectShapeConverter.java
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
 *  Version 2.0 (2018-10-03)
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

public class RectShapeConverter extends ShapeConverterHelper {

	RectShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}

	@Override
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// Corner radius is a special rectangle feature
		CSVList cornerOptions = new CSVList(",","=");
		applyCornerRadius(shape,cornerOptions);

		// Get dimensions of the rectangle
		double dWidth = getParameter(shape,XMLString.SVG_WIDTH);
		double dHeight = getParameter(shape,XMLString.SVG_HEIGHT);
		
		// Convert the path
		startPath(ldp,strokeOptions,fillOptions,cornerOptions);
		ldp.append(point(0,dTranslateY)).append(" rectangle").append(point(dWidth,dTranslateY-dHeight));
		endPath(ldp);
		
		// Add text node
		convertText(shape,shape,dTranslateY+"cm",dWidth+"cm",(dTranslateY-dHeight)+"cm","0cm",0,false,ldp,oc);
	}
	
	static void applyCornerRadius(Element shape,CSVList options) {
		double dCornerRadius = getParameter(shape,XMLString.DRAW_CORNER_RADIUS);
		if (Math.abs(dCornerRadius)>0.001) {
			options.addValue("rounded corners", format(dCornerRadius)+"cm");
		}
		
	}

}
