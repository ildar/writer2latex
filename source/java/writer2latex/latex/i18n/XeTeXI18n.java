/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Version 2.0 (2018-03-27)
 * 
 */

package writer2latex.latex.i18n;

import java.text.Bidi;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This class takes care of i18n in XeLaTeX
 */
public class XeTeXI18n extends I18n {

    private Polyglossia polyglossia;
    private boolean bCTL;
    private boolean bUseXepersian;
    private String sLTRCommand=null;
    private String sRTLCommand=null;

    /** Construct a new XeTeXI18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public XeTeXI18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
    	super(ofr,config,palette);
    	
    	// TODO: Add a use_xepersian option, using polyglossia if false
    	// For CTL languages currently only monolingual documents are supported
    	// For LCG languages, multilingual documents are supported using polyglossia
    	polyglossia = new Polyglossia();
    	
    	// Identify script type and set default language
    	bCTL = polyglossia.isCTL(sDefaultCTLLanguage, sDefaultCTLCountry);
    	if (bCTL) {
        	polyglossia.applyLanguage(sDefaultCTLLanguage, sDefaultCTLCountry);
        	// For farsi, we load xepersian.sty
        	bUseXepersian = "fa".equals(sDefaultCTLLanguage);
        	if (bUseXepersian) {
        		sLTRCommand = "\\lr";
        		sRTLCommand = "\\rl";
        	}
    	}
    	else {
    		polyglossia.applyLanguage(sDefaultLanguage, sDefaultCountry);
    	}

    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	// Load standard packages
    	pack.append("\\usepackage{amsmath,amssymb,amsfonts}").nl()
    		.append("\\usepackage{fontspec}").nl()
    		.append("\\usepackage{xunicode}").nl()
    		.append("\\usepackage{xltxtra}").nl();
    	
    	// xepersian.sty and polyglossia (or rather bidi) should be loaded as the last package
		// We put them in the declarations part to achieve this
    	if (bUseXepersian) { // For farsi, use xepersian rather than polyglossia
    		decl.append("\\usepackage{xepersian}").nl();
    	}
    	else {
    		String[] polyglossiaDeclarations = polyglossia.getDeclarations();
    		for (String s: polyglossiaDeclarations) {
    			decl.append(s).nl();
    		}
    	}

    	// Set the default font if this is a CTL document
    	if (bCTL) {
			if ("he".equals(sDefaultCTLLanguage)) { // Use a default font set for hebrew
				decl.append("\\setmainfont[Script=Hebrew]{Frank Ruehl CLM}").nl();
				decl.append("\\setsansfont[Script=Hebrew]{Nachlieli CLM}").nl();
				decl.append("\\setmonofont[Script=Hebrew]{Miriam Mono CLM}").nl();
			}
			else { // Use default CTL font for other languages
	    		StyleWithProperties defaultStyle = ofr.getDefaultParStyle();	
				if (defaultStyle!=null) {
					String sDefaultCTLFont = defaultStyle.getProperty(XMLString.STYLE_FONT_NAME_COMPLEX);
					if (sDefaultCTLFont!=null) {
			    		decl.append("\\settextfont{").append(sDefaultCTLFont).append("}").nl();
					}
				}
			}
    	}
    }
    
    /** Apply a language
     *  @param style the LO style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
    	// TODO: Support multilingual CTL documents
        if (!bCTL && !bAlwaysUseDefaultLang && style!=null) {
            String sISOLang = style.getProperty(XMLString.FO_LANGUAGE,bInherit);
            String sISOCountry = style.getProperty(XMLString.FO_COUNTRY, bInherit);
            if (sISOLang!=null) {
            	String[] sCommand = polyglossia.applyLanguage(sISOLang, sISOCountry);
            	if (bDecl) {
            		ba.add(sCommand[1],sCommand[2]);
            	} 
            	else {
            		ba.add(sCommand[0]+"{","}");
            	}
            }
        }
    }

    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public void pushSpecialTable(String sName) {
    	// TODO
    }
	
    /** Pop a font from the font stack
     */
    public void popSpecialTable() {
    	// TODO
    }
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the ISO language of the string
     *  @return the LaTeX string
     */
    public String convert(String s, boolean bMathMode, String sLang){
    	StringBuilder buf = new StringBuilder();
    	int nLen = s.length();
        char c;
        if (bMathMode) {
        	// No string replace or writing direction in math mode
        	for (int i=0; i<nLen; i++) {
        		convert(s.charAt(i),buf);
        	}        	
        }
        else if (!bUseXepersian) {
        	int i = 0;
        	while (i<nLen) {
        		ReplacementTrieNode node = stringReplace.get(s,i,nLen);
        		if (node!=null) {
        			buf.append(node.getLaTeXCode());
        			i += node.getInputLength();
        		}
        		else {
        			c = s.charAt(i++);
        			convert (c,buf);
        		}
        	}
        }
        else {
        	// TODO: Add support for string replace
			Bidi bidi = new Bidi(s,Bidi.DIRECTION_RIGHT_TO_LEFT);
			int nCurrentLevel = bidi.getBaseLevel();
			int nNestingLevel = 0;
			for (int i=0; i<nLen; i++) {
				int nLevel = bidi.getLevelAt(i);
				if (nLevel>nCurrentLevel) {
					if (nLevel%2==0) { // even is LTR
						buf.append(sLTRCommand).append("{");
					}
					else { // odd is RTL
						buf.append(sRTLCommand).append("{");						
					}
					nCurrentLevel=nLevel;
					nNestingLevel++;
				}
				else if (nLevel<nCurrentLevel) {
					buf.append("}");
					nCurrentLevel=nLevel;
					nNestingLevel--;
				}
				convert(s.charAt(i),buf);
			}
			while (nNestingLevel>0) {
				buf.append("}");
				nNestingLevel--;
			}
		}
        
        return buf.toString();
    }
    
    private void convert(char c, StringBuilder buf) {
		switch (c) {
		case '#' : buf.append("\\#"); break; // Parameter
		case '$' : buf.append("\\$"); break; // Math shift
		case '%' : buf.append("\\%"); break; // Comment
		case '&' : buf.append("\\&"); break; // Alignment tab
		case '\\' : buf.append("\\textbackslash{}"); break; // Escape
		case '^' : buf.append("\\^{}"); break; // Superscript
		case '_' : buf.append("\\_"); break; // Subscript
		case '{' : buf.append("\\{"); break; // Begin group
		case '}' : buf.append("\\}"); break; // End group
		case '~' : buf.append("\\textasciitilde{}"); break; // Active (non-breaking space)
		case '\u00A0' : buf.append('~'); break; // Make non-breaking spaces visible
		default: buf.append(c);
	}
    	
    }
    

	

}
