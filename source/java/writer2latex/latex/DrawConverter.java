/************************************************************************
 *
 *  DrawConverter.java
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
 *  Version 2.0 (2018-08-19)
 *
 */
 
package writer2latex.latex;

import java.util.LinkedList;
import java.util.Stack;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.base.BinaryGraphicsDocument;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.EmbeddedObject;
import writer2latex.office.EmbeddedXMLObject;
import writer2latex.office.MIMETypes;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;

/**
 *  <p>This class handles draw elements.</p>
 */
public class DrawConverter extends ConverterHelper {
	
	// Do we export custom shapes?
	private boolean bUseTikz;
	private boolean bNeedTikz;

	// Do we include any external graphics?
    private boolean bNeedGraphicx = false;

    // Keep track of floating frames (images, text boxes...)
    private Stack<LinkedList<Element>> floatingFramesStack = new Stack<LinkedList<Element>>();
	
    private Element getFrame(Element onode) {
        return (Element) onode.getParentNode();
    }
	
    public DrawConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
		bUseTikz = config.useTikz();
		bNeedTikz = false;
        floatingFramesStack.push(new LinkedList<Element>());
    }

    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        if (bNeedGraphicx) { 
        	String sOptions = null;
            if (config.backend()==LaTeXConfig.PDFTEX) sOptions = "pdftex";
            else if (config.backend()==LaTeXConfig.DVIPS) sOptions = "dvips";
            pacman.usepackage(sOptions, "graphicx");
        }
		if (bNeedTikz) {
			pacman.usepackage("tikz");
		}
    }
    	
    public void handleCaption(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Floating frames should be positioned *above* the label, hence
        // we use a separate ldp for the paragraphs and add this later
        LaTeXDocumentPortion capLdp = new LaTeXDocumentPortion(true);

        // Convert the caption
        if (oc.isInFigureFloat()) { // float
            capLdp.append("\\caption");
            palette.getCaptionCv().handleCaptionBody(node,capLdp,oc,false);
        }
        else { // nonfloat
            capLdp.append("\\captionof{figure}");
            palette.getCaptionCv().handleCaptionBody(node,capLdp,oc,true);
        }
		
        flushFloatingFrames(ldp,oc);
        ldp.append(capLdp);
    }
	
    // Process the first child of a draw:frame
    public void handleDrawElement(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // node must be an element in the draw namespace
        String sName = node.getTagName();
        if (sName.equals(XMLString.DRAW_OBJECT)) {
            handleDrawObject(node,ldp,oc);
        }		
        else if (sName.equals(XMLString.DRAW_OBJECT_OLE)) {
            handleDrawObject(node,ldp,oc);
        }		
        else if ((!oc.isInHeaderFooter()) && sName.equals(XMLString.DRAW_IMAGE)) {
            handleDrawImage(node,ldp,oc);
        }		
        else if ((!oc.isInHeaderFooter()) && sName.equals(XMLString.DRAW_TEXT_BOX)) {
            handleDrawTextBox(node,ldp,oc);
        }		
        else if (sName.equals(XMLString.DRAW_A)) {
            // we handle this like text:a
            palette.getFieldCv().handleAnchor(node,ldp,oc);
        }
        else if (sName.equals(XMLString.DRAW_FRAME)) {
        	if (!palette.getMathCv().handleTexMathsEquation(node,ldp)) {
        		// OpenDocument: Get the actual draw element in the frame
        		handleDrawElement(Misc.getFirstChildElement(node),ldp,oc);
        	}
        }
        else if (sName.equals(XMLString.DRAW_G)) {
        	if (!palette.getMathCv().handleTexMathsEquation(node,ldp)) {
        		handleGroup(node,ldp,oc);
        	}
        }
        else {
        	ShapeConverterHelper sch = getShapeConverterHelper(sName);
        	if (sch!=null) {
            	if (bUseTikz) {
            		bNeedTikz=true;
    				ldp.append("\\begin{tikzpicture}").nl();
    	        	sch.handleShape(node, sch.getMaxY(node), ldp, oc);
    				ldp.append("\\end{tikzpicture}").nl();
            	}
            	// TODO: Add warning otherwise?
        	}
        	else {
	            // Other drawing objects are currently not supported
	            ldp.append("[Warning: Draw object ignored]");
        	}
        }
    }
	
    //-----------------------------------------------------------------
    // handle draw:object elements (OOo objects such as Chart, Math,...)
    
    private void handleGroup(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	if (bUseTikz) {
    		bNeedTikz=true;
	    	// First get the maximal y coordinate
	    	//  (used to avoid negative y-coordinates, which is not important but looks better)
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
    }
    
    private ShapeConverterHelper getShapeConverterHelper(String sXML) {
    	if (XMLString.DRAW_CUSTOM_SHAPE.equals(sXML)) {
    		return palette.getCustomShapeCv();
    	}
    	else if (XMLString.DRAW_LINE.equals(sXML)) {
    		return palette.getLineShapeCv();
    	}
    	else if (XMLString.DRAW_POLYLINE.equals(sXML)) {
    		return palette.getPolyShapeCv();
    	}
    	else if (XMLString.DRAW_POLYGON.equals(sXML)) {
    		return palette.getPolyShapeCv();
    	}
    	return null;
    }
    
    private void handleDrawObject(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	
        String sHref = Misc.getAttribute(node,XMLString.XLINK_HREF);
		
        if (sHref!=null) { // Embedded object in package or linked object
            if (ofr.isInPackage(sHref)) { // Embedded object in package
                if (sHref.startsWith("#")) { sHref=sHref.substring(1); }
                if (sHref.startsWith("./")) { sHref=sHref.substring(2); }
                EmbeddedObject object = palette.getEmbeddedObject(sHref); 
                if (object!=null) {
                    if (MIMETypes.MATH.equals(object.getType()) || MIMETypes.ODF.equals(object.getType())) { // Formula!
                        try {
                            Document formuladoc = ((EmbeddedXMLObject) object).getContentDOM();
                            Element formula = Misc.getChildByTagName(formuladoc,XMLString.MATH); // Since OOo3.2
                            if (formula==null) {
                            	formula = Misc.getChildByTagName(formuladoc,XMLString.MATH_MATH);
                            }
                            String sLaTeX = palette.getMathCv().convert(formula);
                            if (!" ".equals(sLaTeX)) { // ignore empty formulas
                            	ldp.append(" $")
                            	   .append(sLaTeX)
                            	   .append("$");
                                if (Character.isLetterOrDigit(OfficeReader.getNextChar(node))) { ldp.append(" "); }
                            }
                        }
                        catch (org.xml.sax.SAXException e) {
                            e.printStackTrace();
                        }
                        catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
	                }
                    else { // unsupported object, look for replacement image
                        Element replacementImage = Misc.getChildByTagName(getFrame(node),XMLString.DRAW_IMAGE);
                        if (replacementImage!=null) {
                            handleDrawImage(replacementImage,ldp,oc);
                        }
                        else { 
                            ldp.append("[Warning: object ignored]");
                        }
                    }

                }
            }
        }
        else { // flat xml, object is contained in node
            Element formula = Misc.getChildByTagName(node,XMLString.MATH); // Since OOo 3.2
            if (formula==null) {
            	formula = Misc.getChildByTagName(node,XMLString.MATH_MATH);
            }
            if (formula!=null) {
                ldp.append(" $")
                   .append(palette.getMathCv().convert(formula))
                   .append("$");
                if (Character.isLetterOrDigit(OfficeReader.getNextChar(node))) { ldp.append(" "); }
            }
            else { // unsupported object, look for replacement image
                Element replacementImage = Misc.getChildByTagName(getFrame(node),XMLString.DRAW_IMAGE);
                if (replacementImage!=null) {
                    handleDrawImage(replacementImage,ldp,oc);
                }
                else { 
                    ldp.append("[Warning: object ignored]");
                }
            }

        }
    }
	
    //--------------------------------------------------------------------------
    // Create float environment
    private void applyFigureFloat(BeforeAfter ba, Context oc) {
        // todo: check context...
        if (config.floatFigures() && !oc.isInFrame() && !oc.isInTable()) {
           if (oc.isInMulticols()) {
               ba.add("\\begin{figure*}","\\end{figure*}\n");
           }
           else {
               ba.add("\\begin{figure}","\\end{figure}\n");
           }
           if (config.floatOptions().length()>0) {
               ba.add("["+config.floatOptions()+"]","");
           }
           ba.add("\n","");
           oc.setInFigureFloat(true);
        }
        if (!oc.isInFrame() && config.alignFrames()) {
            // Avoid nesting center environment
        	if (config.floatFigures()) {
        		// Inside floats we don't want the extra glue added by the center environment
        		ba.add("\\centering\n","\n");
        	}
        	else {
        		// Outside a float we certainly want it
                ba.add("\\begin{center}\n","\n\\end{center}\n");
        	}
        }
	
    }
	
    //--------------------------------------------------------------------------
    // Handle draw:image elements
	
    private void handleDrawImage(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Include graphics if allowed by the configuration
        switch (config.imageContent()) {
        case LaTeXConfig.IGNORE:
            // Ignore graphics silently
            return;
        case LaTeXConfig.WARNING:
            System.err.println("Warning: Images are not allowed");
            return;
        case LaTeXConfig.ERROR:
            ldp.append("% Error in document: An image was ignored");
            return;
        }

        Element frame = getFrame(node);
        String sName = frame.getAttribute(XMLString.DRAW_NAME);
        palette.getFieldCv().addTarget(sName,"|graphic",ldp);
        String sAnchor = frame.getAttribute(XMLString.TEXT_ANCHOR_TYPE);
        
        //if (oc.isInFrame() || "as-char".equals(sAnchor)) {
        if ("as-char".equals(sAnchor)) {
            handleDrawImageAsChar(node,ldp,oc);
        }
        else {
            floatingFramesStack.peek().add(node);
        }
    }
	
    private void handleDrawImageAsChar(Element node, LaTeXDocumentPortion ldp, Context oc) {
        ldp.append(" ");
        includeGraphics(node,ldp,oc);
        ldp.append(" ");
    }

    private void handleDrawImageFloat(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        BeforeAfter ba = new BeforeAfter();

        applyFigureFloat(ba,ic);
		
        ldp.append(ba.getBefore());
        includeGraphics(node,ldp,ic);
        ldp.append(ba.getAfter());
    }

    private void includeGraphics(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFileName = null;
        boolean bCommentOut = true;
        
        BinaryGraphicsDocument bgd = palette.getImageCv().getImage(node);
        if (bgd!=null) {
        	if (!bgd.isLinked()) { // embedded image
        		if (!bgd.isRecycled()) { palette.addDocument(bgd); }
                sFileName = bgd.getFileName();
                String sMIME = bgd.getMIMEType();
                bCommentOut = !(
                    config.backend()==LaTeXConfig.UNSPECIFIED ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.JPEG.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.PNG.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.PDF.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.JPEG.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.PNG.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.PDF.equals(sMIME)) ||
                    (config.backend()==LaTeXConfig.DVIPS && MIMETypes.EPS.equals(sMIME)));
        	}
        	else { // linked image
                sFileName = bgd.getFileName();
                String sExt = Misc.getFileExtension(sFileName).toLowerCase();
                // Accept only relative filenames and supported filetypes:
                bCommentOut = sFileName.indexOf(":")>-1 || !(
                    config.backend()==LaTeXConfig.UNSPECIFIED ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.JPEG_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.PNG_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.PDFTEX && MIMETypes.PDF_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.JPEG_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.PNG_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.XETEX && MIMETypes.PDF_EXT.equals(sExt)) ||
                    (config.backend()==LaTeXConfig.DVIPS && MIMETypes.EPS_EXT.equals(sExt)));
        	}
        }
        else {
            ldp.append("[Warning: Image not found]");
            return;
        }
		
        // Now for the actual inclusion:
        bNeedGraphicx = true;
        /* TODO: handle cropping and mirror:
           style:mirror can be none, vertical (lodret), horizontal (vandret),
           horizontal-on-odd, or
           horizontal-on-even (horizontal on odd or even pages).
   		   mirror is handled with scalebox, eg:
        		%\\scalebox{-1}[1]{...}
		   can check for even/odd page first!!
	
          fo:clip="rect(t,r,b,l) svarer til trim
          value can be auto - no clip!
		  cropping is handled with clip and trim:
		  \\includegraphics[clip,trim=l b r t]{...}
		  note the different order from xsl-fo!
         */

        if (bCommentOut) {
            ldp.append(" [Warning: Image ignored] ");
            ldp.append("% Unhandled or unsupported graphics:").nl().append("%");
        }
        
        // Get the style
        Element frame = getFrame(node);
        String sY = Calc.truncateLength(frame.getAttribute(XMLString.SVG_Y));
        StyleWithProperties style = ofr.getFrameStyle(frame.getAttribute(XMLString.DRAW_STYLE_NAME));
        BeforeAfter ba = new BeforeAfter();
        palette.getFrameStyleSc().applyFrameStyleImage(style, ba, sY, true);
        
        ldp.append(ba.getBefore()).append("\\includegraphics");

        CSVList options = new CSVList(',');
        if (!config.originalImageSize()) {
            String sWidth = Calc.truncateLength(frame.getAttribute(XMLString.SVG_WIDTH));
            String sHeight = Calc.truncateLength(frame.getAttribute(XMLString.SVG_HEIGHT));
            if (sWidth!=null) { options.addValue("width="+sWidth); }
            if (sHeight!=null) { options.addValue("height="+sHeight); }
        }
        if (config.imageOptions().length()>0) {
            options.addValue(config.imageOptions()); // TODO: New CSVList...
        }
        if (!options.isEmpty()) {
            ldp.append("[").append(options.toString()).append("]");
        }

        if (config.removeGraphicsExtension()) {
            sFileName = Misc.removeExtension(sFileName);
        }
        ldp.append("{").append(sFileName).append("}").append(ba.getAfter());
        if (bCommentOut) { ldp.nl(); }
    }

    //--------------------------------------------------------------------------
    // handle draw:text-box element
	
    private void handleDrawTextBox(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Element frame = getFrame(node);
        String sName = frame.getAttribute(XMLString.DRAW_NAME);
        palette.getFieldCv().addTarget(sName,"|frame",ldp);
        String sAnchor = frame.getAttribute(XMLString.TEXT_ANCHOR_TYPE);
        //if (oc.isInFrame() || "as-char".equals(sAnchor)) {
        if ("as-char".equals(sAnchor)) {
            makeDrawTextBox(node, ldp, oc);
        }
        else {
            floatingFramesStack.peek().add(node);
        }
    }
	
    private void handleDrawTextBoxFloat(Element node, LaTeXDocumentPortion ldp, Context oc) {
        BeforeAfter ba = new BeforeAfter();
        Context ic = (Context) oc.clone();

        applyFigureFloat(ba,ic);
		
        ldp.append(ba.getBefore());
        makeDrawTextBox(node, ldp, ic);
        ldp.append(ba.getAfter());
    }

    private void makeDrawTextBox(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        ic.setInFrame(true);
        ic.setNoFootnotes(true);
		
        // Check to see, if this is really a container for a figure caption
        boolean bIsCaption = false;
        if (OfficeReader.isSingleParagraph(node)) {
            Element par = Misc.getFirstChildElement(node);
            String sSeqName = ofr.getSequenceName(par);
            if (ofr.isFigureSequenceName(sSeqName)) { bIsCaption = true; }
        }

        Element frame = getFrame(node);
        String sWidth = Calc.truncateLength(frame.getAttribute(XMLString.SVG_WIDTH));
        String sY = Calc.truncateLength(frame.getAttribute(XMLString.SVG_Y));
        StyleWithProperties style = ofr.getFrameStyle(frame.getAttribute(XMLString.DRAW_STYLE_NAME));
        BeforeAfter ba = new BeforeAfter();
        palette.getFrameStyleSc().applyFrameStyleText(style, ba, sWidth, sY, true);
        
        if (!bIsCaption) {
            ldp.append(ba.getBefore()).nl();
        }
        floatingFramesStack.push(new LinkedList<Element>());
        palette.getBlockCv().traverseBlockText(node,ldp,ic);
        flushFloatingFrames(ldp,ic);
        floatingFramesStack.pop();
        if (!bIsCaption) {
        	ldp.append(ba.getAfter());
        }
        if (!oc.isNoFootnotes()) { palette.getNoteCv().flushFootnotes(ldp,oc); }

    }
    
    //-------------------------------------------------------------------------
    //handle any pending floating frames
    
    public void flushFloatingFrames(LaTeXDocumentPortion ldp, Context oc) {
	    // todo: fix language
        LinkedList<Element> floatingFrames = floatingFramesStack.peek();
        int n = floatingFrames.size();
        if (n==0) { return; }
        for (int i=0; i<n; i++) {
            Element node = (Element) floatingFrames.get(i);
            String sName = node.getNodeName();
            if (sName.equals(XMLString.DRAW_IMAGE)) {
                handleDrawImageFloat(node,ldp,oc);
            }
            else if (sName.equals(XMLString.DRAW_TEXT_BOX)) {
                handleDrawTextBoxFloat(node,ldp,oc);
            }
        }
        floatingFrames.clear();
    }
	
}