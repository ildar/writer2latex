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
 *  Version 2.0 (2018-09-23)
 *  
 */
package writer2latex.latex.tikz;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;

public class FrameConverter extends ShapeConverterHelper {

	public FrameConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
    double getMaxY(Element shape) {
		return super.getMaxY(shape) + getParameter(shape,XMLString.SVG_HEIGHT);
    }
	
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		// A frame is a rectangle, so we will start with that
		ldp.append("\\path");

		CSVList options = new CSVList(",","=");
		options.addValues(strokeOptions);
		options.addValues(fillOptions);
		if (!options.isEmpty()) {
			ldp.append("[").append(options.toString()).append("]");
		}

		double dWidth = getParameter(shape,XMLString.SVG_WIDTH);
		double dHeight = getParameter(shape,XMLString.SVG_HEIGHT);
		ldp.append(" (0,").append(format(dTranslateY)).append(") rectangle (")
			.append(format(dWidth)).append(",").append(format(dTranslateY-dHeight)).append(");").nl();
		
		// Next determine the frame type
		Node child = shape.getFirstChild();
		if (child.getNodeType()==Node.ELEMENT_NODE) {
			if (child.getNodeName().equals(XMLString.DRAW_TEXT_BOX)) {
				// Add text node (always wrap, hence true for bForceWrap)
				convertText(shape,(Element)child,dTranslateY+"cm",dWidth+"cm",(dTranslateY-dHeight)+"cm","0cm",0,true,ldp,oc);
			}
			else if(child.getNodeName().equals(XMLString.DRAW_IMAGE)) {
				// Create a node with the image
				ldp.append("\\path (").append(format(0.5*dWidth)).append(",").append(format(dTranslateY-0.5*dHeight))
					.append(") node[inner sep=0pt] {");
				palette.getDrawCv().includeGraphics((Element)child, ldp, oc);
				ldp.append("};").nl();
			}
		}
	}
	
}
