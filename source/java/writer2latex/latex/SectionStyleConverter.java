/************************************************************************
 *
 *  SectionConverter.java
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
 *  Version 2.0 (2018-06-25)
 *
 */

package writer2latex.latex;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Calc;

/** This class convert ODF section styles to <code>multicols</code> environments. The export depends on the options
 *  <code>use_multicol</code> (nothing is exported if this is false) and <code>multicols_format</code> (if this is
 *  false, only the number of columns is exported, otherwise also column distance, separator line and unbalancing
 *  of columns is exported).
 */
public class SectionStyleConverter extends ConverterHelper {

	// Configuration options
	private boolean bUseMulticol;
	private boolean bMulticolsFormat;
	// Flag to indicate if we actually need multicols.sty
    private boolean bNeedMulticol;
    
    public SectionStyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
		bUseMulticol = config.useMulticol();
		bMulticolsFormat = config.multicolsFormat();
		bNeedMulticol = false;
	}

	@Override
	void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        if (bNeedMulticol) { pacman.usepackage("multicol"); }
	}
	
	/** Apply a section style using <code>multicols.sty</code>. This will generate a <code>multicols</code> environment
	 *  if there is at least two columns, depending on the options.
	 * 
	 * @param style the office section style to apply
	 * @param ba the <code>BeforeAfter</code> to hold the code
	 * @param context the current context
	 */
	void applySectionStyle(StyleWithProperties style, BeforeAfter ba, Context context) {
        // Don't nest multicols and require at least 2 columns
        if (bUseMulticol && !context.isInMulticols() && style!=null && style.getColCount()>1) {
	        int nCols = style.getColCount();
	        bNeedMulticol = true;
	        context.setInMulticols(true);
	        if (bMulticolsFormat) {
	        	// Calculate the column separation: Each column has a left and right margin. We sum these up and
	        	// use the average (excluding the first left and last right margin).
	        	String sColumnSep = "0cm";
	        	String s;
	        	for (int i=0; i<nCols; i++) {
	        		s = style.getColumnProperty(i, XMLString.FO_START_INDENT);
	        		if (i>0 && s!=null) { // Exclude first
	        			sColumnSep = Calc.add(s, sColumnSep);
	        		}
	        		s = style.getColumnProperty(i, XMLString.FO_END_INDENT);
	        		if (i<nCols-1 && s!=null) { // Exclude last
	        			sColumnSep = Calc.add(s, sColumnSep);
	        		}
	        	}
	        	if (!Calc.isZero(sColumnSep)) { // If the spacing is zero, we prefer the default distance
	        		ba.addBefore("\\setlength{\\columnsep}{" + Calc.multiply((100.0/(nCols-1))+"%",sColumnSep) + "}\n");
	        	}
	        	// Set the width of the separator line
	        	String sSepWidth = style.getColumnSepProperty(XMLString.STYLE_WIDTH);
	        	boolean bHasSeparator = false; // the default is no separator
	        	if (sSepWidth!=null && !"none".equals(XMLString.STYLE_STYLE) && !Calc.isZero(sSepWidth)) {
	        		ba.addBefore("\\setlength{\\columnseprule}{"+sSepWidth+"}\n");
	        		bHasSeparator = true;
	        	}
	        	// Set the color of the separator line
	        	if (bHasSeparator) {
		        	String sColor = style.getColumnSepProperty(XMLString.STYLE_COLOR);
		        	BeforeAfter baColor = new BeforeAfter();
		        	palette.getColorCv().applyColor(sColor, true, baColor, context);
		        	if (!baColor.isEmpty()) {
		        		ba.addBefore("\\renewcommand\\columnseprulecolor{");
		        		ba.add(baColor);
		        		ba.addBefore("}\n");
		        	}
	        	}
	        }
	        if (!ba.isEmpty()) {
		        // Group the contents in order not to change default values of parameters
		        ba.enclose("{","}\n");
	        }
	        else {
	        	ba.enclose("", "\n");
	        }
	        if (bMulticolsFormat && "true".equals(style.getProperty(XMLString.TEXT_DONT_BALANCE_TEXT_COLUMNS))) {
		        ba.add("\\begin{multicols*}", "\\end{multicols*}");	        	
	        }
	        else {
		        ba.add("\\begin{multicols}", "\\end{multicols}");

	        }
	        ba.addBefore("{"+(nCols>10 ? 10 : nCols)+"}\n");
        }
	}

}
