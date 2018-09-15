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
 *  Version 2.0 (2018-09-14)
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
import writer2latex.latex.LaTeXPacman;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;


/** This class (and the helpers in the same package) takes care of i18n in
 *  Writer2LaTeX. In XeLaTeX, i18n is simpler than in Classic LaTeX:
 *  Input encoding is always UTF-8, and font encoding is mostly UTF-8.
 *  Languages are handled with polyglossia, xepersian and xeCJK.
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
    private boolean bUseGenericFonts;
    private Polyglossia polyglossia;
    // TODO: Add a use_xepersian option, using polyglossia if false
    private boolean bUseXepersian;
    private boolean bBidi = false;
    private String sLTRCommand = null;
    private String sRTLCommand = null;
    
    // Data for western text conversion 
	private String sCTLCommand = null;
	private String sCTLLanguage = null;
	private boolean bNeedCJK = true;


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
        
        // Get main script type
    	nScript = config.script();
        
        // Determine whether or not to use the generic fonts rm, sf and tt
        bUseGenericFonts = config.fontspec().equals("default");

        // Define main language for polyglossia
        polyglossia = new Polyglossia();
    	
    	switch (nScript) {
    	case LaTeXConfig.CJK:
        	// Use xeCJK:
    		break;
    	case LaTeXConfig.CTL:
    		bBidi = polyglossia.isBidi(sDefaultCTLLanguage, sDefaultCTLCountry);
    		if (bBidi) {
            	if ("fa".equals(sDefaultCTLLanguage)) {
                	// For farsi, we load xepersian.sty
                	bUseXepersian = true ;
            		sLTRCommand = "\\lr";
            		sRTLCommand = "\\rl";
            	}
            	else {
            		// Other bidi scripts uses bidi.sty (loaded by polyglossia)
            		sLTRCommand = "\\LR";
            		sRTLCommand = "\\RL";        			
            	}
    		}
    		if (!bUseXepersian) {
        		// Use polyglossia for all languages but farsi
            	polyglossia.applyLanguage(sDefaultCTLLanguage, sDefaultCTLCountry);
        	}
        	break;
    	case LaTeXConfig.WESTERN:
    	default:
    		// For western languages, we use polyglossia
    		// The same applies for CTL languages embedded in the western text
    		// (Currently only one CTL language per document is supported)
    		polyglossia.applyLanguage(sDefaultLanguage, sDefaultCountry);
    	}

    	Set<String> symbols = new HashSet<>();
    	symbols.add("xetex");
    	readSymbols(symbols,true);
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pacman usepackage declarations
     *  @param decl other declarations
     */
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
    	useSymbolFonts(pacman);
    	useBasePackages(pacman);
    	useLanguages(pacman,decl);
    	useFonts(decl);
    }
    
	// Load standard packages
    private void useBasePackages(LaTeXPacman pacman) {
    	if (!config.fontspec().equals("original+math")) {
    		// xunicode and xltxtra seems not to be needed anymore?
    		//pack.append("\\usepackage{fontspec,xunicode,xltxtra}").nl();
    		pacman.usepackage("fontspec");
    	}
    	else {
    		pacman.usepackage("mathspec");
    	}
    }
    
    // Load
    private void useLanguages(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
    	// Load xeCJK if the main script is CJK or western with CJK text
    	if (nScript==LaTeXConfig.CJK || bNeedCJK) {
    		pacman.usepackage("xeCJK");
    	}
    	// Load xepersian or polyglossia 
    	// These packages (or rather bidi) should be loaded as the last package
		// We put them in the declarations part to achieve this
    	if (bUseXepersian) { // For farsi, use xepersian rather than polyglossia
    		pacman.usepackage("xepersian");
    	}
    	else if (nScript!=LaTeXConfig.CJK){
    		String[] polyglossiaDeclarations = polyglossia.getDeclarations();
    		for (String s: polyglossiaDeclarations) {
    			decl.append(s).nl();
    		}
    	}
    }
    
    // Load fonts based on fontspec and script option
    private void useFonts(LaTeXDocumentPortion decl) {
    	switch (config.fontspec()) {
    	case "original+math":
    		// TODO: The font may not have greek letters
    		useDefaultFont("\\setmathsfont(Digits,Latin,Greek)",XMLString.STYLE_FONT_NAME,decl);
    	case "original":
    		switch (nScript) {
			case LaTeXConfig.WESTERN:
	    		useDefaultFont("\\setmainfont",XMLString.STYLE_FONT_NAME,decl);
	    		if (sCTLLanguage!=null && !sCTLLanguage.isEmpty()) {
	    			useDefaultFont("\\newfontfamily\\"+sCTLLanguage+"font[Scale=MatchUppercase]",XMLString.STYLE_FONT_NAME_COMPLEX,decl);
	    		}
	    		if (bNeedCJK) {
	    			useDefaultFont("\\setCJKmainfont",XMLString.STYLE_FONT_NAME_ASIAN,decl);
	    		}
	    		break;
    		case LaTeXConfig.CTL:
    			useDefaultFont("\\setmainfont",XMLString.STYLE_FONT_NAME_COMPLEX,decl);
    			break;
    		case LaTeXConfig.CJK:
    			useDefaultFont("\\setmainfont",XMLString.STYLE_FONT_NAME,decl);
    			useDefaultFont("\\setCJKmainfont",XMLString.STYLE_FONT_NAME_ASIAN,decl);
    		}
	    	break;
    	case "default":
    	default:
    		// Do nothing, uses default fonts (Latin Modern)
		}
    }
    
	/*if ("he".equals(sDefaultCTLLanguage)) { // Use a default font set for hebrew
	decl.append("\\setmainfont[Script=Hebrew]{Frank Ruehl CLM}").nl();
	decl.append("\\setsansfont[Script=Hebrew]{Nachlieli CLM}").nl();
	decl.append("\\setmonofont[Script=Hebrew]{Miriam Mono CLM}").nl();
	}*/
    
    private void useDefaultFont(String sCommand, String sXMLStyleName, LaTeXDocumentPortion decl) {
		String sFont = getDefaultFontFamily(sXMLStyleName);
		if (sFont!=null) {
    		decl.append(sCommand).append("{").append(sFont).append("}").nl();
		}    		    	
    }
    
	// Get the default font for the specified script (or null if no default font exists)
    private String getDefaultFontFamily(String sXMLStyleName) {
		StyleWithProperties defaultStyle = ofr.getDefaultParStyle();	
		if (defaultStyle!=null) {
            // Get font family information from font declaration
            String sStyleName = defaultStyle.getProperty(sXMLStyleName);
            if (sStyleName!=null) {
                FontDeclaration fd = (FontDeclaration) ofr.getFontDeclarations().getStyle(sStyleName);
                if (fd!=null) {
                    return fd.getFirstFontFamily();
                }
            }
		}
		return null;
    }
    
    public void applyFontFamily(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
    	if (bUseGenericFonts) {
    		super.applyFontFamily(style, bDecl, bInherit, ba, context);
    	}
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
    public String convert(String s, boolean bMathMode, String sLang) {
    	boolean bGreekText = "el".equals(sLang);
    	StringBuilder buf = new StringBuilder();
        if (bMathMode) {
        	convertMath(s,buf,bGreekText);
        }
        else if (bBidi) {
        	convertBidiText(s,buf,bGreekText);
        }
        else if (nScript==LaTeXConfig.WESTERN && !bAlwaysUseDefaultLang && sDefaultCTLLanguage!=null) {
        	convertWesternText(s,buf,bGreekText);
		}
        else {
        	convertText(s,buf,bGreekText);
		}
        
        return buf.toString();
    }
    
    private void convertMath(String s, StringBuilder buf, boolean bGreekText) {
    	// No string replace or writing direction in math mode
    	int nLen = s.length();
    	for (int i=0; i<nLen; i++) {
    		convert(s.charAt(i),buf,false,bGreekText);
    	}        	    	
    }
    
    private void convertWesternText(String s, StringBuilder buf, boolean bGreekText) {
    	boolean bComplex = false;
    	int i = 0;
    	int nLen = s.length();
    	while (i<nLen) {
    		char c = s.charAt(i);
    		if (bComplex && (isWestern(c) || isAsian(c))) {
    			buf.append("}");
    			bComplex = false;
    		}
    		else if (!bComplex && isComplex(c)) {
    			if (sCTLCommand==null) {
    				String[] commands = polyglossia.applyLanguage(sDefaultCTLLanguage, sDefaultCTLCountry);
    				sCTLCommand = commands[0];
    				sCTLLanguage = commands[3];
    			}
    			if (!sCTLCommand.isEmpty()) {
        			buf.append(sCTLCommand).append("{");
        			bComplex = true;
    			}
    		}
    		if (isAsian(c)) {
    			bNeedCJK = true;
    		}
    		ReplacementTrieNode node = stringReplace.get(s,i,nLen);
    		if (node!=null) {
    			buf.append(node.getLaTeXCode());
    			i += node.getInputLength();
    		}
    		else {
    			convert(s.charAt(i++),buf,true,bGreekText);
    		}
    	}
    	if (bComplex) {
    		buf.append("}");
    	}
    }
    
    private void convertText(String s, StringBuilder buf, boolean bGreekText) {
    	int i = 0;
    	int nLen = s.length();
    	while (i<nLen) {
    		ReplacementTrieNode node = stringReplace.get(s,i,nLen);
    		if (node!=null) {
    			buf.append(node.getLaTeXCode());
    			i += node.getInputLength();
    		}
    		else {
    			convert(s.charAt(i++),buf,true,bGreekText);
    		}
    	}
    }
    
    private void convertBidiText(String s, StringBuilder buf, boolean bGreekText){
    	// TODO: Add support for string replace
		Bidi bidi = new Bidi(s,Bidi.DIRECTION_DEFAULT_RIGHT_TO_LEFT);
		int nCurrentLevel = bidi.getBaseLevel();
		int nNestingLevel = 0;
		if (nCurrentLevel%2==0) { // Base direction is LTR
			buf.append(sLTRCommand).append("{");
			nNestingLevel++;
		}
    	int nLen = s.length();
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
    
    private static boolean isWestern(char c) {
    	return inRange(c,'\u0003','\u001F')
    			|| inRange(c,'\u0021','\u009F')
    			|| inRange(c,'\u00A1','\u04FF')
    			|| inRange(c,'\u0530','\u058F')
    			|| inRange(c,'\u10A0','\u10FF')
    			|| inRange(c,'\u13A0','\u16FF')
    			|| inRange(c,'\u1E00','\u1FFF')
    			|| inRange(c,'\u2C60','\u2C7F')
    			|| inRange(c,'\u2C80','\u2CE3')
    			|| inRange(c,'\uA720','\uA7FF');
    }
    
    private static boolean isComplex(char c) {
    	return inRange(c,'\u0590','\u074F')
    			|| inRange(c,'\u0780','\u07BF')
    			|| inRange(c,'\u0900','\u109F')
    			|| inRange(c,'\u1200','\u137F')
    			|| inRange(c,'\u1780','\u18AF')
    			|| inRange(c,'\uFB50','\uFDFF')
    			|| inRange(c,'\uFE70','\uFEFF');
    }
    
    private static boolean isAsian(char c) {
    	return inRange(c,'\u1100','\u11FF')
    			|| inRange(c,'\u2E80','\u31BF')
    			|| inRange(c,'\u31C0','\u31EF')
    			|| inRange(c,'\u3200','\u4DBF')
    			|| inRange(c,'\u4E00','\uA4CF')
    			|| inRange(c,'\uAC00','\uD7AF')
    			|| inRange(c,'\uF900','\uFAFF')
    			|| inRange(c,'\uFE30','\uFE4F')
    			|| inRange(c,'\uFF00','\uFFEF');
    	// Note: Also U+20000..U+2A6DF and U+2F800..U+2FA1F, we don't support characters outside BMP currently
    }
    
    private static boolean inRange(char c,char c1,char c2) {
    	return (c>=c1) && (c<=c2);
    }
    
}
