/************************************************************************
 *
 *  MathmlConverter.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.4 (2014-08-08)
 *
 */

package writer2latex.latex;


import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.TableReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/**
 *  This class converts MathML nodes to LaTeX.
 *  The class name is slightly misleading:
 *  It only converts the StarMath annotation, if available
 *  and it also converts TexMaths formulas
 */
public final class MathmlConverter extends ConverterHelper {
	
	private enum TexMathsStyle {inline, display, latex};
    
    private StarMathConverter smc;
	
    private boolean bContainsFormulas = false;
    private boolean bAddParAfterDisplay = false;
	
    public MathmlConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        smc = new StarMathConverter(palette.getI18n(),config);
        bAddParAfterDisplay = config.formatting()>=LaTeXConfig.CONVERT_MOST;
    }

    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bContainsFormulas) {
            if (config.useOoomath()) {
                pack.append("\\usepackage{ooomath}").nl();
            }
            else {
                smc.appendDeclarations(pack,decl);
            }
        }
    }
	
    public String convert(Node settings, Node formula) {
        // TODO: Use settings to determine display mode/text mode
        // formula must be a math:math node
        // First try to find a StarMath annotation
    	Node semantics = Misc.getChildByTagName(formula,XMLString.SEMANTICS); // Since OOo 3.2
    	if (semantics==null) {
    		semantics = Misc.getChildByTagName(formula,XMLString.MATH_SEMANTICS);
    	}
		if (semantics!=null) {
			Node annotation = Misc.getChildByTagName(semantics,XMLString.ANNOTATION); // Since OOo 3.2
			if (annotation==null) {
				annotation = Misc.getChildByTagName(semantics,XMLString.MATH_ANNOTATION);
			}
            if (annotation!=null) {
                String sStarMath = "";
                if (annotation.hasChildNodes()) {
                    NodeList anl = annotation.getChildNodes();
                    int nLen = anl.getLength();
                    for (int i=0; i<nLen; i++) {
                        if (anl.item(i).getNodeType() == Node.TEXT_NODE) {
                            sStarMath+=anl.item(i).getNodeValue();
                        }
                    }
                    bContainsFormulas = true;      
                    return smc.convert(sStarMath);
                }
            }
        }
        // No annotation was found. In this case we should convert the mathml,
        // but currently we ignore the problem.
        // TODO: Investigate if Vasil I. Yaroshevich's MathML->LaTeX
        // XSL transformation could be used here. (Potential problem:
        // OOo uses MathML 1.01, not MathML 2)
		if (formula.hasChildNodes()) {
			return "\\text{Warning: No StarMath annotation}";
		}
		else { // empty formula
			return " ";
		}
			
    }

    /** Handle an (inline) TexMaths equation
     * 
     * @param node the equation (an svg:desc element containing the formula)
     * @param ldp the LaTeXDocumentPortion to contain the converted equation
     * @param oc the current context
     */
    public void handleTexMathsEquation(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	// LaTeX code is contained in svg:desc
    	// Format is <point size>X<mode>X<TeX code>X<format>X<resolution>X<transparency>
    	// where X is a paragraph sign
    	switch (getTexMathsStyle(node)) {
    	case inline:
       		ldp.append("$").append(getTexMathsEquation(node)).append("$");
       		break;
    	case display:
       		ldp.append("$\\displaystyle ").append(getTexMathsEquation(node)).append("$");
       		break;
    	case latex:    		
       		ldp.append(getTexMathsEquation(node));
    	}
    }
    
    private TexMathsStyle getTexMathsStyle(Element node) {
   		String[] sContent = Misc.getPCDATA(node).split("\u00a7");
   		if (sContent.length>=3) { // we only need 3 items of 6
   			if ("display".equals(sContent[1])) {
   				return TexMathsStyle.display;
   			}
   			else if ("latex".equals(sContent[1])) {
   				return TexMathsStyle.latex;
   			}
   		}
   		return TexMathsStyle.inline;
    }
    
    private String getTexMathsEquation(Element node) {
   		String[] sContent = Misc.getPCDATA(node).split("\u00a7");
   		if (sContent.length>=3) { // we only need 3 items of 6
   			return sContent[2];
   		}
   		else {
   			return "";
   		}
    }
	
    /** Try to convert a table as a display equation:
     *  A 1 row by 2 columns table in which each cell contains exactly one paragraph,
     *  the left cell contains exactly one formula and the right cell contains exactly
     *  one sequence number is treated as a (numbered) display equation.
     *  This happens to coincide with the AutoText provided with OOo Writer :-)
     *  @param table the table reader
     *  @param ldp the LaTeXDocumentPortion to contain the converted equation
     *  @return true if the conversion was successful, false if the table
     * did not represent a display equation
     */
    public boolean handleDisplayEquation(TableReader table, LaTeXDocumentPortion ldp) {
    	if (table.getRowCount()==1 && table.getColCount()==2 &&
    		OfficeReader.isSingleParagraph(table.getCell(0, 0)) && OfficeReader.isSingleParagraph(table.getCell(0, 1)) ) {
    		// Table of the desired form
    		if (palette.parseDisplayEquation(Misc.getFirstChildElement(table.getCell(0, 0))) && palette.getEquation()!=null && palette.getSequence()==null) {
    			// Found equation in first cell
    			Element myEquation = palette.getEquation();
    			if (palette.parseDisplayEquation(Misc.getFirstChildElement(table.getCell(0, 1))) && palette.getEquation()==null && palette.getSequence()!=null) {
    				// Found sequence in second cell
    				handleDisplayEquation(myEquation, palette.getSequence(), ldp);
    				return true;
    			}
    		}
    	}
    	return false;
    }

    /**Try to convert a paragraph as a display equation:
     * A paragraph which contains exactly one formula + at most one sequence
     * number is treated as a display equation. Other content must be brackets
     * or whitespace (possibly with formatting).
     * @param node the paragraph
     * @param ldp the LaTeXDocumentPortion to contain the converted equation
     * @return true if the conversion was successful, false if the paragraph
     * did not contain a display equation
     */
    public boolean handleDisplayEquation(Element node, LaTeXDocumentPortion ldp) {
        if (palette.parseDisplayEquation(node) && palette.getEquation()!=null) {
        	handleDisplayEquation(palette.getEquation(), palette.getSequence(), ldp);
        	return true;
        }
        else {
            return false;
        }
    }
    
    private void handleDisplayEquation(Element equation, Element sequence, LaTeXDocumentPortion ldp) {
    	boolean bTexMaths = equation.getTagName().equals(XMLString.SVG_DESC);
    	TexMathsStyle style = TexMathsStyle.inline;
    	String sLaTeX;
    	if (bTexMaths) {
    		// TeXMaths equation
    		sLaTeX = getTexMathsEquation(equation);
    		style = getTexMathsStyle(equation);
    	}
    	else {
    		// MathML equation
    		sLaTeX = convert(null,equation);
    	}
    	if (!" ".equals(sLaTeX)) { // ignore empty formulas
    		if (!bTexMaths || style!=TexMathsStyle.latex) {
    			if (sequence!=null) {
    				// Numbered equation
    				ldp.append("\\begin{equation}");
    				palette.getFieldCv().handleSequenceLabel(sequence,ldp);
    				if (bTexMaths && style==TexMathsStyle.inline) {
    					ldp.append("\\textstyle ");
    				}
    				ldp.nl()
    				.append(sLaTeX).nl()
    				.append("\\end{equation}").nl();
    			}
    			else {
    				// Unnumbered equation
    				ldp.append("\\begin{equation*}");
    				if (bTexMaths && style==TexMathsStyle.inline) {
    					ldp.append("\\textstyle ");
    				}
    				ldp.nl()
    				.append(sLaTeX).nl()
    				.append("\\end{equation*}").nl();
    			}    	
    		}
    		else {
    			ldp.append(sLaTeX).nl();
    		}
			if (bAddParAfterDisplay) { ldp.nl(); }
    	}
    }

}