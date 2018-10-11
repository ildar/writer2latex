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
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/** Convert draw:path and draw:connector shapes
 * 
 */
public class PathShapeConverter extends ShapeWithViewBoxConverterHelper {
	
	// State while parsing the current path
	private double dFirstX; // x-coordinate of first point on path
	private double dFirstY; // y-coordinate of first point on path
	private double dCurrentX; // x-coordinate of current point
	private double dCurrentY; // y-coordinate of current point
	private boolean bClosed; // at least one sub-path is closed (by the Z or z command)
	private char cPreviousCommand; // the previous applied command
	private double dControlX; // x-coordinate of last control point
	private double dControlY; // y-coordinate of last control point
	private boolean bControlIsRelative; // flag to indicate relative coordinates for control point
	
	public PathShapeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}
	
	@Override
	void handleShapeInner(Element shape, double dTranslateY, LaTeXDocumentPortion ldp, Context oc) {
		super.handleShapeInner(shape, dTranslateY, ldp, oc);
		parseViewBox(shape);
		
		// Some data only used by connectors
		CSVList placementOptions = new CSVList(",","=");
		double dX1=0,dX2=0,dY1=0,dY2=0;
		
		String sPath = Misc.getAttribute(shape, XMLString.SVG_D);		
		if (sPath!=null) {
			if (shape.getNodeName().equals(XMLString.DRAW_CONNECTOR)) {
				// A connector needs some special treatment: It does not specify the svg:width, svg:height, svg:x and svg:y.
				// Hence we must reconstruct these from the available data, which is the start and end point of the path
				
				// Get the start point (dX1,dY1) and the end point (dX2,dY2)
				dX1 = getParameter(shape,XMLString.SVG_X1);
				dY1 = getParameter(shape,XMLString.SVG_Y1);
				dX2 = getParameter(shape,XMLString.SVG_X2);
				dY2 = getParameter(shape,XMLString.SVG_Y2);
				
				// Special treatment of horizontal and vertical lines: These are exported as lines
				if (Math.abs(dX1-dX2)<0.0001 || Math.abs(dY1-dY2)<0.0001) {
					startPath(ldp,strokeOptions,arrowOptions);
					ldp.append(point(dX1,dTranslateY-dY1)).append(" --").append(point(dX2,dTranslateY-dY2));
					endPath(ldp);
					// Add text node
					if (Math.abs(dX1-dX2)<0.001) { // Vertical line
						convertText(shape, shape, 
								Math.max(dTranslateY-dY1, dTranslateY-dY2)+"cm", (dX1+0.1)+"cm",
								Math.min(dTranslateY-dY1, dTranslateY-dY2)+"cm", (dX1-0.1)+"cm",
								0.0, false,
								ldp, oc);						
					}
					else { // Horizontal line
						convertText(shape, shape, 
								(dTranslateY-dY1+0.1)+"cm", Math.max(dX1, dX2)+"cm",
								(dTranslateY-dY1-0.1)+"cm", Math.min(dX1, dX2)+"cm",
								0.0, false,
								ldp, oc);						
					}
					return;
				}
				
				// Determine coordinates of (dX1,dX2) and (dY1,dY2) relative to view box
				// This involves parsing the path with a fake width and height
				dWidth=dViewBoxWidth;
				dHeight=dViewBoxHeight;
				SimpleInputBuffer in = new SimpleInputBuffer(sPath);
				wsp(in);
				StringBuilder tikz = new StringBuilder();
				try {
					path(in, tikz);
				}
				catch (Exception e) {
					System.err.println("Error "+e.getMessage());
					return;
				}
				// We are now able to calculate the width and height using (dFirstX,dFirstY) and (dCurrentX,dCurrentY),
				// which the coordinates mentioned above
				dWidth = Math.abs(dX2-dX1)/Math.abs(dCurrentX-dFirstX)*dViewBoxWidth;
				dHeight = Math.abs(dY2-dY1)/Math.abs(dCurrentY-dFirstY)*dViewBoxHeight;
				// Calculate the upper right corner of the view box
				double dX = dX1-(dFirstX-dViewBoxMinX)/dViewBoxWidth*dWidth;
				double dY = dY1-(dFirstY-dViewBoxMinY)/dViewBoxHeight*dHeight;
				// Create options for placement scope
				String s = format(dX);
				if (!s.equals("0")) { placementOptions.addValue("xshift", s+"cm"); }		
				s = format(-dY); // As always: Upside down axis in ODF
				if (!s.equals("0")) { placementOptions.addValue("yshift", s+"cm"); }
			}
			
			// We now have a suitable width and height in all cases an can convert the path 
			SimpleInputBuffer in = new SimpleInputBuffer(sPath);
			StringBuilder tikz = new StringBuilder();
			try {
				path(in, tikz);
				if (!placementOptions.isEmpty()) { // Connectors may need additional placement
					ldp.append("\\begin{scope}[").append(placementOptions.toString()).append("]").nl();			
				}
				// SVG and TikZ always applies fill options if they are present (by implicitly closing the path)
				// ODF (or at least LO) on the other hand only fills closed path, hence the flag bClosed
				// This may be too simplistic if the path contains several sub-paths which are not all closed
				if (bClosed) { startPath(ldp,strokeOptions,fillOptions); }
				else { startPath(ldp,strokeOptions,arrowOptions); }
				ldp.append(tikz.toString());
				endPath(ldp);
				if (!placementOptions.isEmpty()) {
					ldp.append("\\end{scope}").nl();
				}
				// Add text node
				if (shape.getNodeName().equals(XMLString.DRAW_CONNECTOR)) {
					// Placement of text nodes on connectors is somewhat strange in LO, this 
					// interpretation seems to be close to the behavior in LO
					convertText(shape, shape, 
						Math.max(dTranslateY-dY1, dTranslateY-dY2)+"cm", Math.max(dX1, dX2)+"cm",
						Math.min(dTranslateY-dY1, dTranslateY-dY2)+"cm", Math.min(dX1, dX2)+"cm",
						0.0, false,
						ldp, oc);
				}
				else {
					// For ordinary paths we can use the view box
					convertText(shape,ldp,oc);
				}
			}
			catch (Exception e) {
				System.err.println("Error "+e.getMessage());
			}
		}		
	}
	
	// SVG path grammar

	// svg-path: wsp* moveto-drawto-command-groups? wsp*
	// moveto-drawto-command-groups: moveto-drawto-command-group | moveto-drawto-command-group wsp* moveto-drawto-command-groups
	// moveto-drawto-command-group: moveto wsp* drawto-commands?
	private void path(SimpleInputBuffer in, StringBuilder tikz) {
		cPreviousCommand='\u0000';
		dFirstX=0;
		dFirstY=0;
		dCurrentX=0;
		dCurrentY=0;
		dControlX=0;
		dControlY=0;
		bControlIsRelative=false;
		bClosed=false;
		wsp(in);
		while (!in.atEnd()) {
			moveto(in,tikz);
			wsp(in);
			drawtoCommands(in,tikz);
			wsp(in);
		}
	}
	
	// drawto-commands: drawto-command | drawto-command wsp* drawto-commands
	private void drawtoCommands(SimpleInputBuffer in, StringBuilder tikz) {
		while (!in.atEnd() && "ZzLlHhVvCcSsQqTtAa".indexOf(in.peekChar())>=0) { // which really means not M or m
			drawtoCommand(in,tikz);
			wsp(in);
		}
	}
	
	// drawto-command: closepath | lineto | horizontal-lineto | vertical-lineto | curveto | smooth-curveto |
	// quadratic-bezier-curveto  | smooth-quadratic-bezier-curveto | elliptical-arc
	private void drawtoCommand(SimpleInputBuffer in, StringBuilder tikz) {
		char c = in.peekChar();
		switch (c) {
		case 'Z':
		case 'z': closepath(in,tikz); break;
		case 'L': lineto(in,tikz,false); break;
		case 'l': lineto(in,tikz,true); break;
		case 'H': horizontalLineto(in,tikz,false); break;
		case 'h': horizontalLineto(in,tikz,true); break;
		case 'V': verticalLineto(in,tikz,false); break;
		case 'v': verticalLineto(in,tikz,true); break;
		case 'C': curveto(in,tikz,false); break; 
		case 'c': curveto(in,tikz,true); break;
		case 'S': smoothCurveto(in,tikz,false); break;
		case 's': smoothCurveto(in,tikz,true); break;
		case 'Q': qbcurveto(in,tikz,false); break;
		case 'q': qbcurveto(in,tikz,true); break;
		case 'T': smoothQbcurveto(in,tikz,false); break;
		case 't': smoothQbcurveto(in,tikz,true); break;
		case 'A': ellipticalArc(in,tikz,false);
		case 'a': ellipticalArc(in,tikz,true);
		default:
			throw new IllegalArgumentException("Syntax error in path: Expected command, but got '"+in.peekChar()+"'");
		}
		cPreviousCommand = c;
	}

	// moveto: ( "M" | "m" ) wsp* moveto-argument-sequence
	// moveto-argument-sequence: coordinate-pair | coordinate-pair comma-wsp? lineto-argument-sequence
	private void moveto(SimpleInputBuffer in, StringBuilder tikz) {
		if (in.peekChar()=='M' || in.peekChar()=='m') {
			boolean bRelative = in.getChar()=='m';
			wsp(in);
			double dX = number(in);
			commawsp(in);
			double dY = number(in);
			commawsp(in);
			insertPoint(dX,dY,bRelative,tikz);
			if (cPreviousCommand=='\u0000') {
				// This is the first point on the path
				dFirstX = dCurrentX; dFirstY = dCurrentY;
			}
			linetoArgumentSequence(in,tikz,bRelative);
		}
	}
	
	// closepath: ("Z" | "z")
	private void closepath(SimpleInputBuffer in, StringBuilder tikz) {
		in.getChar();
		tikz.append(" -- cycle");
		bClosed=true;
	}
	
	// lineto: ( "L" | "l" ) wsp* lineto-argument-sequence
	private void lineto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		linetoArgumentSequence(in,tikz,bRelative);
	}
	
	// lineto-argument-sequence: coordinate-pair | coordinate-pair comma-wsp? lineto-argument-sequence
	private void linetoArgumentSequence(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			double dX = number(in);
			commawsp(in);
			double dY = number(in);
			commawsp(in);
			tikz.append(" --");
			insertPoint(dX,dY,bRelative,tikz);
		}
	}
	
	// horizontal-lineto: ( "H" | "h" ) wsp* horizontal-lineto-argument-sequence
	// horizontal-lineto-argument-sequence: coordinate | coordinate comma-wsp? horizontal-lineto-argument-sequence
	private void horizontalLineto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			double dX = number(in);
			commawsp(in);
			tikz.append(" -- ");
			insertPoint(dX,bRelative?0:dCurrentY,bRelative,tikz);
		}		
	}

	// vertical-lineto: ( "V" | "v" ) wsp* vertical-lineto-argument-sequence
	// vertical-lineto-argument-sequence: coordinate | coordinate comma-wsp? vertical-lineto-argument-sequence
	private void verticalLineto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			double dY = number(in);
			commawsp(in);
			tikz.append(" --");
			insertPoint(bRelative?0:dCurrentX,dY,bRelative,tikz);
		}		
	}
	
	// curveto: ( "C" | "c" ) wsp* curveto-argument-sequence
	// curveto-argument-sequence: curveto-argument | curveto-argument comma-wsp? curveto-argument-sequence
	private void curveto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			curvetoArgument(in,tikz,bRelative);
			commawsp(in);
		}
	}
	
	// curveto-argument: coordinate-pair comma-wsp? coordinate-pair comma-wsp? coordinate-pair
	private void curvetoArgument(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		double dX1 = number(in);
		commawsp(in);
		double dY1 = number(in);
		commawsp(in);
		double dX2 = number(in);
		commawsp(in);
		double dY2 = number(in);
		commawsp(in);
		double dX = number(in);
		commawsp(in);
		double dY = number(in);
		commawsp(in);
		// Syntax for bezier curves is ".. controls (x1,y1) and (x2,y2) .. (x,y)"
		if (bRelative) {
			// In TikZ, the second control point is relative to the *end* of the curve
			// Hence we must recalculate in this case
			dX2-=dX;
			dY2-=dY;
		}
		tikz.append(" .. controls");
		insertControlPoint(dX1,dY1,bRelative,tikz);
	    tikz.append(" and");
		insertControlPoint(dX2,dY2,bRelative,tikz);
	    tikz.append(" ..");
		insertPoint(dX,dY,bRelative,tikz);
	}
	
	// smooth-curveto: ( "S" | "s" ) wsp* smooth-curveto-argument-sequence
	// smooth-curveto-argument-sequence: smooth-curveto-argument | smooth-curveto-argument comma-wsp? smooth-curveto-argument-sequence
	private void smoothCurveto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			smoothCurvetoArgument(in,tikz,bRelative);
			commawsp(in);
		}
	}
	
	// smooth-curveto-argument: coordinate-pair comma-wsp? coordinate-pair
	private void smoothCurvetoArgument(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		double dX1 = number(in);
		commawsp(in);
		double dY1 = number(in);
		commawsp(in);
		double dX = number(in);
		commawsp(in);
		double dY = number(in);
		commawsp(in);
		// Syntax for bezier curves is ".. controls (x1,y1) and (x2,y2) .. (x,y)"
		tikz.append(" .. controls");
		insertAutomaticControlPoint('C','S',tikz);
	    tikz.append(" and");
		insertControlPoint(dX1,dY1,bRelative,tikz);
	    tikz.append(" ..");
		insertPoint(dX,dY,bRelative,tikz);
	}
		
    // quadratic-bezier-curveto: ( "Q" | "q" ) wsp* quadratic-bezier-curveto-argument-sequence
	// quadratic-bezier-curveto-argument-sequence: quadratic-bezier-curveto-argument
	//   | quadratic-bezier-curveto-argument comma-wsp? quadratic-bezier-curveto-argument-sequence
	private void qbcurveto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			qbcurvetoArgument(in,tikz,bRelative);
			commawsp(in);
		}
	}

	// quadratic-bezier-curveto-argument: coordinate-pair comma-wsp? coordinate-pair
	private void qbcurvetoArgument(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		double dX1 = number(in);
		commawsp(in);
		double dY1 = number(in);
		commawsp(in);
		double dX = number(in);
		commawsp(in);
		double dY = number(in);
		commawsp(in);
		// Syntax for bezier curves is ".. controls (x1,y1) .. (x,y)"
		tikz.append(" .. controls");
		insertControlPoint(dX1,dY1,bRelative,tikz);
	    tikz.append(" ..");
		insertPoint(dX,dY,bRelative,tikz);
	}
	
	// smooth-quadratic-bezier-curveto: ( "T" | "t" ) wsp* smooth-quadratic-bezier-curveto-argument-sequence
	// smooth-quadratic-bezier-curveto-argument-sequence: coordinate-pair
	//     | coordinate-pair comma-wsp? smooth-quadratic-bezier-curveto-argument-sequence
	private void smoothQbcurveto(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			smoothQbcurvetoArgument(in,tikz,bRelative);		
		}
	}
	
	private void smoothQbcurvetoArgument(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		double dX = number(in);
		commawsp(in);
		double dY = number(in);
		commawsp(in);
		// Syntax for bezier curves is ".. controls (x1,y1) .. (x,y)"
		tikz.append(" .. controls ");
		insertAutomaticControlPoint('Q','T',tikz);
	    tikz.append(" ..");
		insertPoint(dX,dY,bRelative,tikz);
	}

	// elliptical-arc: ( "A" | "a" ) wsp* elliptical-arc-argument-sequence
	// elliptical-arc-argument-sequence: elliptical-arc-argument
	//     | elliptical-arc-argument comma-wsp? elliptical-arc-argument-sequence
	private void ellipticalArc(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		in.getChar();
		wsp(in);
		while (!in.atEnd() && !Character.isLetter(in.peekChar())) {
			ellipticalArcArgument(in,tikz,bRelative);
			commawsp(in);
		}
	}
	
	// elliptical-arc-argument: nonnegative-number comma-wsp? nonnegative-number comma-wsp? 
	//     number comma-wsp flag comma-wsp flag comma-wsp coordinate-pair
	private void ellipticalArcArgument(SimpleInputBuffer in, StringBuilder tikz, boolean bRelative) {
		number(in); // rx
		commawsp(in);
		number(in); // ry
		commawsp(in);
		number(in); // x-axis rotation
		commawsp(in);
		flag(in); // large arc flag
		commawsp(in);
		flag(in); // sweep flag
		commawsp(in);
		double dX = number(in);
		commawsp(in);
		double dY = number(in);
		commawsp(in);
		// Currently we don't support elliptical arcs, using a simple lineto as replacement
		tikz.append(" --");
		insertPoint(dX,dY,bRelative,tikz);		
	}
	
	private double number(SimpleInputBuffer in) {
		String s = in.getSignedDouble();
		if (s!=null) {
			return Double.parseDouble(s);
		}
		throw new IllegalArgumentException("Syntax error in path: Expected number");
	}
	
	private int flag(SimpleInputBuffer in) {
		char c = in.getChar();
		if (c=='0') {
			return 0;
		}
		else if (c=='1') {
			return 1;
		}
		throw new IllegalArgumentException("Syntax error in path: Expected 0 or 1");
	}
	
	private void commawsp(SimpleInputBuffer in) {
		wsp(in);
		if (in.peekChar()==',') { in.getChar(); }
		wsp(in);
	}
	
	private void wsp(SimpleInputBuffer in) {
		while (!in.atEnd() && (in.peekChar()==' ' || in.peekChar()=='\u0009' || in.peekChar()=='\n' || in.peekChar()=='\r')) {
			in.getChar();
		}
	}

	private void insertAutomaticControlPoint(char cCommand1, char cCommand2, StringBuilder tikz) {
		double dX2,dY2;
		if (cPreviousCommand==cCommand1 || cPreviousCommand==Character.toLowerCase(cCommand1)
			|| cPreviousCommand==cCommand2 || cPreviousCommand==Character.toLowerCase(cCommand2)) {
			// Smooth continuitation of the previous curve.
			// The control point is the reflection of the last control point relative to the current point
			if (bControlIsRelative) {
				dX2=-dControlX;
				dY2=-dControlY;
			}
			else {
				dX2=2*dCurrentX-dControlX;
				dY2=2*dCurrentY-dControlY;
			}
			insertControlPoint(dX2,dY2,bControlIsRelative,tikz);
		}
		else {
			// If the previous command was not a curve of the same type, use the current point
			dX2=dCurrentX;
			dY2=dCurrentY;
			insertControlPoint(dX2,dY2,false,tikz);
		}
	}

	
	private void insertControlPoint(double dX, double dY, boolean bRelative, StringBuilder tikz) {
		dControlX=dX;
		dControlY=dY;
		bControlIsRelative = bRelative;
		if (bRelative) {
			tikz.append(point("+",transformLengthX(dX),-transformLengthY(dY)));
		}
		else {
			tikz.append(transformPoint(dX,dY));		
		}		
	}
	
	private void insertPoint(double dX, double dY, boolean bRelative, StringBuilder tikz) {
		if (bRelative) {
			dCurrentX+=dX;
			dCurrentY+=dY;
			tikz.append(point("++",transformLengthX(dX),-transformLengthY(dY)));
		}
		else {
			dCurrentX=dX;
			dCurrentY=dY;
			tikz.append(transformPoint(dX,dY));		
		}
	}
	
}
