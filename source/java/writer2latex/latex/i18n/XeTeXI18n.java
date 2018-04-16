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
 *  Version 2.0 (2018-04-15)
 * 
 */

package writer2latex.latex.i18n;

import java.text.Bidi;
import java.util.HashSet;
import java.util.Set;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;


/** This class (and the helpers in the same package) takes care of i18n in
 *  Writer2LaTeX. In XeLaTeX, i18n is simpler than in Classic LaTeX:
 *  Input encoding is always UTF-8, and font encoding is mostly UTF-8.
 *  Languages is handled with polyglossia, xepersian and xeCJK.
 *  The class XeTeXI18n thus manages these, and like the classic version
 *  implements a Unicode->LaTeX translation that can handle unicode
 *  characters which requires a special treatment.
 *  The translation is table driven, using symbols.xml (embedded in the jar)
 *  Various sections of symbols.xml handles different cases:
 *  <ul>
 *    <li>common symbols which has a special meaning i LaTeX</li>
 *    <li>additional symbol fonts such as wasysym, dingbats etc.</li>
 *    <li>font-specific symbols, eg. for 8-bit fonts/private use area</li>
 *  </ul>
 *  The class uses the packages polyglossia, xepersia, xeCJK, fontspec,
 *  xunicode, xltxtra, 
 *  tipa, bbding, ifsym, pifont, eurosym, amsmath, wasysym, amssymb, amsfonts
 *  in various combinations depending on the configuration.
 */
public class XeTeXI18n extends I18n {
	
    protected String sDefaultCTLLanguage=null; // The default CTL ISO language to use
    protected String sDefaultCTLCountry=null; // The default CTL ISO country to use
    protected String sDefaultCJKLanguage=null; // The default CJK ISO language to use
    protected String sDefaultCJKCountry=null; // The default CJK ISO country to use

    private int nScript;
    private Polyglossia polyglossia;
    // TODO: Add a use_xepersian option, using polyglossia if false
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
    	    	
        // Read the default CTL and CJK languages from the default paragraph style
        if (ofr!=null) {
            StyleWithProperties style = ofr.getDefaultParStyle();
            if (style!=null) { 
                sDefaultCTLLanguage = style.getProperty(XMLString.STYLE_LANGUAGE_COMPLEX);
                sDefaultCTLCountry = style.getProperty(XMLString.STYLE_COUNTRY_COMPLEX);
                sDefaultCJKLanguage = style.getProperty(XMLString.STYLE_LANGUAGE_ASIAN);
                sDefaultCJKCountry = style.getProperty(XMLString.STYLE_COUNTRY_ASIAN);
            }
        }
        
        // Determine main script type
    	nScript = config.getScript();
        if (nScript==LaTeXConfig.AUTO) {
        	if (sDefaultCJKLanguage!=null && sDefaultCJKLanguage.length()>0) {
        		nScript = LaTeXConfig.CJK;
        	}
        	else if (sDefaultCTLLanguage!=null && sDefaultCTLLanguage.length()>0) {
        		nScript = LaTeXConfig.CTL;
        	}
        	else {
        		nScript = LaTeXConfig.WESTERN;
        	}
        }

        // Define main language for polyglossia
        polyglossia = new Polyglossia();
    	
    	switch (nScript) {
    	case LaTeXConfig.CJK:
        	// Use xeCJK:
    		break;
    	case LaTeXConfig.CTL:
        	// For farsi, we load xepersian.sty
        	bUseXepersian = "fa".equals(sDefaultCTLLanguage);
        	if (bUseXepersian) {
        		sLTRCommand = "\\lr";
        		sRTLCommand = "\\rl";
        	}
        	else {
        		// For other CTL languages, we use polyglossia
            	polyglossia.applyLanguage(sDefaultCTLLanguage, sDefaultCTLCountry);
        	}
        	break;
    	case LaTeXConfig.WESTERN:
    	default:
    		// For western languages, we use polyglossia
    		polyglossia.applyLanguage(sDefaultLanguage, sDefaultCountry);
    	}

    	Set<String> symbols = new HashSet<>();
    	symbols.add("xetex");
    	readSymbols(symbols,true);
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	useSymbolFonts(pack);
    	
    	// Load standard packages
    	pack.append("\\usepackage{fontspec,xunicode,xltxtra}").nl();
    	
    	// Load xeCJK
    	if (nScript==LaTeXConfig.CJK) {
    		pack.append("\\usepackage{xeCJK}").nl();
    	}
    	
    	// Load xepersian or polyglossia 
    	// These packages (or rather bidi) should be loaded as the last package
		// We put them in the declarations part to achieve this
    	if (bUseXepersian) { // For farsi, use xepersian rather than polyglossia
    		decl.append("\\usepackage{xepersian}").nl();
    	}
    	else if (nScript!=LaTeXConfig.CJK){
    		String[] polyglossiaDeclarations = polyglossia.getDeclarations();
    		for (String s: polyglossiaDeclarations) {
    			decl.append(s).nl();
    		}
    	}
    	// Set the default font if this is a CTL document
    	if (nScript==LaTeXConfig.CTL) {
			if ("he".equals(sDefaultCTLLanguage)) { // Use a default font set for hebrew
				decl.append("\\setmainfont[Script=Hebrew]{Frank Ruehl CLM}").nl();
				decl.append("\\setsansfont[Script=Hebrew]{Nachlieli CLM}").nl();
				decl.append("\\setmonofont[Script=Hebrew]{Miriam Mono CLM}").nl();
			}
			else { // Use default CTL font for other languages
				String sFont = getDefaultFontFamily(XMLString.STYLE_FONT_NAME_COMPLEX,XMLString.STYLE_FONT_FAMILY_COMPLEX);
				if (sFont!=null) {
		    		decl.append("\\settextfont{").append(sFont).append("}").nl();
				}
			}
    	}
    	else if (nScript==LaTeXConfig.CJK) {
			String sFont = getDefaultFontFamily(XMLString.STYLE_FONT_NAME_ASIAN,XMLString.STYLE_FONT_FAMILY_ASIAN);
			if (sFont!=null) {
	    		decl.append("\\setCJKmainfont{").append(sFont).append("}").nl();
			}
    	}
    }
    
	// Get the default font (or null if no default font exists)
    private String getDefaultFontFamily(String sXMLStyleName, String sXMLStyleFamily) {
		StyleWithProperties defaultStyle = ofr.getDefaultParStyle();	
		if (defaultStyle!=null) {
            // Get font family information from font declaration or from style
            String sStyleName = defaultStyle.getProperty(sXMLStyleName);
            if (sStyleName!=null) {
                FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(sStyleName);
                if (fd!=null) {
                    return fd.getFontFamily();
                }
            }
            return defaultStyle.getProperty(XMLString.FO_FONT_FAMILY);
		}
		return null;
    }
    
    /** Apply a language
     *  @param style the LO style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba) {
    	// TODO: Support multilingual CTL and CJK documents
        if (nScript==LaTeXConfig.WESTERN && !bAlwaysUseDefaultLang && style!=null) {
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

    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the ISO language of the string
     *  @return the LaTeX string
     */
    public String convert(String s, boolean bMathMode, String sLang){
    	boolean bGreekText = "el".equals(sLang);
    	StringBuilder buf = new StringBuilder();
    	int nLen = s.length();
        char c;
        if (bMathMode) {
        	// No string replace or writing direction in math mode
        	for (int i=0; i<nLen; i++) {
        		convert(s.charAt(i),buf,false,bGreekText);
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
        			convert(c,buf,true,bGreekText);
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
				convert(s.charAt(i),buf,true,bGreekText);
			}
			while (nNestingLevel>0) {
				buf.append("}");
				nNestingLevel--;
			}
		}
        
        return buf.toString();
    }
    
    private void convert(char c, StringBuilder buf, boolean bTextMode, boolean bGreekText) {
    	if (bTextMode && table.hasTextChar(c)) {
    		// Text character with translation
    		buf.append(table.getTextChar(c));
    	}
    	else if (bTextMode && bGreekMath && (!bGreekText) && (Character.UnicodeBlock.of(c)==Character.UnicodeBlock.GREEK) && table.hasMathChar(c)) {
    		// Greek letter as symbol
    		buf.append("$").append(table.getMathChar(c)).append("$");
    	}
    	else if ((!bTextMode) && table.hasMathChar(c)) {
    		// Math character with translation
    		buf.append(table.getMathChar(c));
    	}
    	else {
    		// Character which does not require any special treatment
    		buf.append(c);
    	}
    }
    
}
