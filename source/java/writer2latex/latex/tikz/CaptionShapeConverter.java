/************************************************************************
 *
 *  CaptionShapeConverter.java
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

public class CaptionShapeConverter extends ShapeConverterHelper {

	public CaptionShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
    double getMaxY(Element shape) {
		return super.getMaxY(shape) + getParameter(shape,XMLString.SVG_HEIGHT);
    }
	
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// A caption has two parts. The first is a rectangle, which may be filled and contains the text
		double dWidth = getParameter(shape,XMLString.SVG_WIDTH);
		double dHeight = getParameter(shape,XMLString.SVG_HEIGHT);
		
		startPath(ldp,strokeOptions,fillOptions);
		ldp.append(point(0,dTranslateY)).append(" rectangle").append(point(dWidth,dTranslateY-dHeight));
		endPath(ldp);
	
		// The other part of the caption is a line, which may have an arrow
		double dX = getParameter(shape,XMLString.DRAW_CAPTION_POINT_X);
		double dY = getParameter(shape,XMLString.DRAW_CAPTION_POINT_Y);
		double dX2 = dX<dWidth/2.0 ? 0 : dWidth;
		double dY2 = dHeight/2;

		startPath(ldp,strokeOptions,arrowOptions);
		ldp.append(point(dX,dTranslateY-dY)).append(" --")
			.append(point((dX+dX2)/2,dTranslateY-dY2)).append(" --")
			.append(point(dX2,dTranslateY-dY2));
		endPath(ldp);
		
		// Add text node (In LO a caption always wraps text despite fo:wrap-option is not set, hence true for bForceWrap)
		convertText(shape,shape,dTranslateY+"cm",dWidth+"cm",(dTranslateY-dHeight)+"cm","0cm",0,true,ldp,oc);
	}

}
