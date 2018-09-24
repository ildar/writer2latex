/************************************************************************
 *
 *  TikZConverter.java
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
 *  Version 2.0 (2018-09-24)
 *
 */
 
package writer2latex.latex.tikz;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.ConverterHelper;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.LaTeXPacman;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;

/** This class converts ODF drawing shapes to TikZ pictures.
 */
public class TikZConverter extends ConverterHelper {
	
	// Do we export drawings?
	private boolean bUseTikZ;
	private boolean bNeedTikZ;
	
	// Converter helpers for the individual shape categories
	private CustomShapeConverter customShapeCv;
    private LineShapeConverter lineShapeCv;
    private PolyShapeConverter polyShapeCv;
    private CaptionShapeConverter captionShapeCv;
    private PathShapeConverter pathShapeCv;
    private FrameConverter frameCv;

	public TikZConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
        customShapeCv = new CustomShapeConverter(ofr,config,palette);
        lineShapeCv = new LineShapeConverter(ofr,config,palette);
        polyShapeCv = new PolyShapeConverter(ofr,config,palette);
        captionShapeCv = new CaptionShapeConverter(ofr,config,palette);
        pathShapeCv = new PathShapeConverter(ofr,config,palette);
        frameCv = new FrameConverter(ofr,config,palette);
		bUseTikZ = config.useTikz();
		bNeedTikZ = false;
	}

	@Override
	public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
		if (bNeedTikZ) {
			pacman.usepackage("tikz");
		}
	}
		
	/** Convert an ODF element to a TikZ picture. Supported elements are draw:g,
	 *  draw:custom-shape, draw:line, draw:polyline, draw:polygon and draw:path
	 * 
	 * @param node the ODF element to handle
	 * @param ldp the LaTeXDocumentPortion to which code should be added
	 * @param oc the current context
	 * @return true if this element was converted to a TikZ picture
	 */
	public boolean handleDrawing(Element node, LaTeXDocumentPortion ldp, Context oc) {
		if (bUseTikZ) {
	        String sName = node.getTagName();
	        if (sName.equals(XMLString.DRAW_G)) {
	    		handleGroup(node,ldp,oc);
	    		return true;
	        }
	        else {
	        	ShapeConverterHelper sch = getShapeConverterHelper(sName);
	        	if (sch!=null) {
            		bNeedTikZ=true;
    				ldp.append("\\begin{tikzpicture}").nl();
    	        	sch.handleShape(node, sch.getMaxY(node), ldp, oc);
    				ldp.append("\\end{tikzpicture}").nl();
    				return true;
	        	}
	        }
		}
        return false;
	}
	
	// Handle a draw:g element as a single TikZ picture
	private void handleGroup(Element node, LaTeXDocumentPortion ldp, Context oc) {
		bNeedTikZ=true;
    	// First get the maximal y coordinate
    	double dMaxY = 0.0;
    	Node child = node.getFirstChild();
    	while (child!=null) {
    		if (child.getNodeType()==Node.ELEMENT_NODE) {
            	ShapeConverterHelper sch = getShapeConverterHelper(child.getNodeName());
            	if (sch!=null) {
            		dMaxY = Math.max(dMaxY, sch.getMaxY((Element)child));
            	}
    		}
    		child = child.getNextSibling();
    	}
    	// Next do the actual export
		ldp.append("\\begin{tikzpicture}").nl();
    	child = node.getFirstChild();
    	while (child!=null) {
    		if (child.getNodeType()==Node.ELEMENT_NODE) {
            	ShapeConverterHelper sch = getShapeConverterHelper(child.getNodeName());
            	if (sch!=null) {
            		sch.handleShape((Element)child, dMaxY, ldp, oc);
            	}
    		}
    		child = child.getNextSibling();
    	}
		ldp.append("\\end{tikzpicture}").nl();
    }
    
	// Get the ConverterHelper associated with the given ODF element name
    private ShapeConverterHelper getShapeConverterHelper(String sXML) {
    	if (XMLString.DRAW_CUSTOM_SHAPE.equals(sXML)) {
    		return customShapeCv;
    	}
    	else if (XMLString.DRAW_LINE.equals(sXML)) {
    		return lineShapeCv;
    	}
    	else if (XMLString.DRAW_POLYLINE.equals(sXML)) {
    		return polyShapeCv;
    	}
    	else if (XMLString.DRAW_POLYGON.equals(sXML)) {
    		return polyShapeCv;
    	}
    	else if (XMLString.DRAW_CAPTION.endsWith(sXML)) {
    		return captionShapeCv;
    	}
    	else if (XMLString.DRAW_PATH.equals(sXML)) {
    		return pathShapeCv;
    	}
    	/*else if (XMLString.DRAW_CONNECTOR.equals(sXML)) {
    		return pathShapeCv;
    	}*/
    	else if (XMLString.DRAW_FRAME.equals(sXML)) {
    		return frameCv;
    	}
    	return null;
    }

}
