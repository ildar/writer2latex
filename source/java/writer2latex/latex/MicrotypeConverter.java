/************************************************************************
 *
 *  MicrotypeConverter.java
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
 *  Version 2.0 (2018-05-31)
 *
 */

package writer2latex.latex;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Calc;

/** This class handles micro typography and letter spacing using the packages <code>microtype.sty</code>
 *  and/or <code>letterspace.sty</code>. The latter is a stripped down version of the former, providing
 *  support for letter spacing only. Usage depends on the options <code>use_microtype</code> and
 *  <code>use_letterspace</code>.
 *  This is only supported if the backend is PdfTeX or XeTeX, and letter spacing is only supported by PdfTeX.
 *  Caveat: Letter spacing requires scalable fonts. Thus the cm-super package needs to be installed if using
 *  letter spacing with Computer Modern, CM Bright or Concrete fonts.
 */
public class MicrotypeConverter extends ConverterHelper {
	// TODO: Check the section with hints and caveats in the microtype manual
	
	private boolean bUseLetterspace = true;
	private boolean bNeedLetterspace = false;

	public MicrotypeConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
		bUseLetterspace = config.useLetterspace() && config.getBackend()==LaTeXConfig.PDFTEX;
	}

	@Override
	void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
		if (config.useMicrotype() && (config.getBackend()==LaTeXConfig.PDFTEX || config.getBackend()==LaTeXConfig.XETEX)) {
			pack.append("\\usepackage{microtype}").nl();
		}
		else if (bNeedLetterspace) {
			pack.append("\\usepackage{letterspace}").nl();
		}
	}
	
	/** Apply letter spacing from a style (declaration form is not supported)
	 * 
	 * @param style the office style to use
	 * @param bInherit true if the property should be inherited from parent styles
	 * @param ba the BeforeAfter to which code should be added
	 */
    void applyLetterspace(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
    	if (bUseLetterspace) {
    	    String s = style.getTextProperty(XMLString.FO_LETTER_SPACING,bInherit);
    	    if (s!=null) {
    	    	// \textls expresses spacing in 1/1000 em, so we need the font size
    	    	String sFontSize = style.getAbsoluteFontSize();
    	    	if (sFontSize==null) { // Bummer, use the default font size and hope for the best
    	    		StyleWithProperties parStyle = ofr.getDefaultParStyle();
    	    		if (parStyle!=null) {
    	    			sFontSize = parStyle.getAbsoluteFontSize();
    	    		}
    	    		if (sFontSize==null) { // Last resort...
    	    			sFontSize = "12pt";
    	    		}
    	    	}
	    		// The starred form (like LO) does not add extra kerning before/after the text
				ba.add("\\textls*[", "");
    	    	if (s.startsWith("-")) { // Calc.divide cannot handle negative results currently
	    	    	String sAmount = Calc.divide(Calc.multiply("1000%", s.substring(1)), sFontSize);
	    	    	ba.add("-"+sAmount.substring(0, sAmount.length()-1),"");
    	    	}
    	    	else {
	    	    	String sAmount = Calc.divide(Calc.multiply("1000%", s), sFontSize);
	    	    	ba.add(sAmount.substring(0, sAmount.length()-1),"");    	    		
    	    	}
				ba.add("]{", "}");
	    		bNeedLetterspace = true;
    	    }
    	}
    }

}
