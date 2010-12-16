/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Copyright: 2002-2010 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.2 (2010-12-15)
 * 
 */

package writer2latex.latex.i18n;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

/** This class takes care of i18n in XeLaTeX
 */
public class XeTeXI18n extends I18n {

    private Polyglossia polyglossia;

    /** Construct a new XeTeXI18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public XeTeXI18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
    	super(ofr,config,palette);
    	polyglossia = new Polyglossia();
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	pack.append("\\usepackage{amsmath,amssymb,amsfonts}").nl()
    		.append("\\usepackage{fontspec}").nl()
    		.append("\\usepackage{xunicode}").nl()
    		.append("\\usepackage{xltxtra}").nl();
    	String[] polyglossiaDeclarations = polyglossia.getDeclarations();
    	for (String s: polyglossiaDeclarations) {
    		pack.append(s).nl();
    	}
    }
	
    /** Apply a language language
     *  @param style the OOo style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
        if (!bAlwaysUseDefaultLang && style!=null) {
        	// TODO: Support CTL and CJK
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
    	// TODO: Do we need anything special for math mode?
    	StringBuffer buf = new StringBuffer();
        char c;
        int nLen = s.length();
        int i = 0;
        while (i<nLen) {
            ReplacementTrieNode node = stringReplace.get(s,i,nLen);
            if (node!=null) {
                buf.append(node.getLaTeXCode());
                i += node.getInputLength();
            }
            else {
        		c = s.charAt(i++);
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
    	return buf.toString();
    }
    

	

}
