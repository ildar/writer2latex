/************************************************************************
 *
 *  I18n.java
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
 *  Version 2.0 (2018-04-20) 
 * 
 */

package writer2latex.latex.i18n;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import writer2latex.office.*;
import writer2latex.util.CSVList;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/** This abstract class takes care of i18n in the LaTeX export.
 *  Since i18n is handled quite differently in LaTeX "Classic"
 *  and XeTeX, we use two different classes
 */
public abstract class I18n {
	
    // **** Global variables ****
	
	// The office reader
	protected OfficeReader ofr;

    // Configuration items
    protected LaTeXConfig config;
    protected ReplacementTrie stringReplace;
    protected int nScript; // Main script
    protected boolean bAlwaysUseDefaultLang; // Ignore sLang parameter to convert()
    protected boolean bGreekMath; // Use math mode for Greek letters
    private boolean bIgnoreFont; // Do not convert font family

    // Collected data
    protected Set<String> languages = new HashSet<>(); // All western languages used
    protected String sDefaultLanguage; // The default western ISO language to use
    protected String sDefaultCountry; // The default western ISO country to use
    
    // Unicode translation
    protected UnicodeTable table; // currently active table (top of stack)
    private Map<String,UnicodeTable> tableSet; // all tables
    private Stack<UnicodeTable> tableStack; // stack of active tables
    
    // Cache of converted font declarations
    private Hashtable<String, String> fontDecls = new Hashtable<String, String>();
	
    // **** Constructors ****

    /** Construct a new I18n as ConverterHelper
     *  @param ofr the OfficeReader to get language information from
     *  @param config the configuration which determines the symbols to use
     *  @param palette the ConverterPalette (unused)
     */
    public I18n(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        // We don't need the palette
    	this.ofr = ofr;

        // Set up config items
        this.config = config;
        stringReplace = config.getStringReplace();
        bAlwaysUseDefaultLang = !config.multilingual();
        bGreekMath = config.greekMath();
        bIgnoreFont = config.formatting()<=LaTeXConfig.IGNORE_MOST;
        
        // Read the default languages from the default paragraph style
        if (ofr!=null) {
	        StyleWithProperties style = ofr.getDefaultParStyle();
	        if (style!=null) { 
	            sDefaultLanguage = style.getProperty(XMLString.FO_LANGUAGE);
	            sDefaultCountry = style.getProperty(XMLString.FO_COUNTRY);
	        }
        }
        // We must have a default western language
        if (sDefaultLanguage==null) { sDefaultLanguage="en"; }
    }
    
    protected void readSymbols(Set<String> symbols, boolean bUnicode) {
    	// Add additional symbols as per configuration
        if (config.useWasysym()) symbols.add("wasysym");
        if (config.useBbding()) symbols.add("bbding");
        if (config.useIfsym()) symbols.add("ifsym");
        if (config.usePifont()) symbols.add("dingbats");
        if (config.useEurosym()) symbols.add("eurosym");
        if (config.useTipa()) symbols.add("tipa");
    	// Parse symbols
        tableSet = new Hashtable<>();
        UnicodeTableHandler handler=new UnicodeTableHandler(tableSet, symbols, bUnicode);
        handler.parse();
        // put root table at top of stack
        table = tableSet.get("root");
        tableStack = new Stack<>();
        tableStack.push(table);
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public abstract void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl);
	
    /** Load symbol font packages common for classic and modern
     * 
     * @param ldp the document portion to which declarations are added
     */
    protected void useSymbolFonts(LaTeXDocumentPortion ldp) {
        if (config.useTipa()) {
            ldp.append("\\usepackage[noenc]{tipa}").nl()
               .append("\\usepackage{tipx}").nl();
        }

        // Has to avoid some nameclashes
        if (config.useBbding()) {
            ldp.append("\\usepackage{bbding}").nl()
               .append("\\let\\bbCross\\Cross\\let\\Cross\\undefined").nl()
               .append("\\let\\bbSquare\\Square\\let\\Square\\undefined").nl()
               .append("\\let\\bbTrianbleUp\\TriangleUp\\let\\TriangleUp\\undefined").nl()
               .append("\\let\\bbTrianlgeDown\\TriangleDown\\let\\TriangleDown\\undefined").nl();
        }    	

        if (config.useIfsym()) {
	        ldp.append("\\usepackage[geometry,weather,misc,clock]{ifsym}").nl();
	    }
        
        // Remaining packages does not need any options
        CSVList packages = new CSVList(",");

        if (config.usePifont()) { packages.addValue("pifont"); }

        if (config.useEurosym()) { packages.addValue("eurosym"); }

        // Always use amsmath
        packages.addValue("amsmath");

        // wasysym *must* be loaded between amsmath and amsfonts!
	    if (config.useWasysym()) { 
	    	packages.addValue("wasysym");
	    }
	
	    // Always use amssymb, amsfonts
	    packages.addValue("amssymb");
	    packages.addValue("amsfonts");
	    
	    ldp.append("\\usepackage{").append(packages.toString()).append("}").nl();
    }
        
    /** Apply a language language
     *  @param style the LO style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public abstract void applyLanguage(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba);

    /** Get the number of characters defined in the current table
     *  (for informational purposes only)
     *  @return the number of characters
     */
    public int getCharCount() { return table.getCharCount(); }
	
    /** Push a font to the font stack
     *  @param sName the name of the font
     */
    public void pushSpecialTable(String sName) {
        // If no name is specified we should keep the current table
        // Otherwise try to find the table, and use root if it's not available
        if (sName!=null) {
            table = tableSet.get(sName);
            if (table==null) { table = tableSet.get("root"); }
        }
        tableStack.push(table);
    }
	
    /** Pop a font from the font stack
     */
    public void popSpecialTable() {
        tableStack.pop();
        table = tableStack.peek();
    }
	
    /** Convert a string of characters into LaTeX
     *  @param s the source string
     *  @param bMathMode true if the string should be rendered in math mode
     *  @param sLang the western ISO language of the string
     *  @return the LaTeX string
     */
    public abstract String convert(String s, boolean bMathMode, String sLang);
    
    /** Get the default western language (either the document language or the most used language)
     * 
     *  @return the default western language
     */
    public String getDefaultLanguage() {
    	return sDefaultLanguage;
    }
    
    /** Get the default western country
     * 
     *  @return the default western country
     */
    public String getDefaultCountry() {
    	return sDefaultCountry;
    }
    
    // Font conversion
    
    /** Apply font family. The default implementation selects a generic font family,
     *  \rmfamily, \sffamily, \ttfamily, resp. \textrm, \textsf, \texttt
     *  
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required (if true,
     *  nothing will be put in the "after" part of ba) 
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFontFamily(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        if (style!=null && !bIgnoreFont) {
	        String sFontName=style.getProperty(XMLString.STYLE_FONT_NAME,bInherit);
	        if (sFontName!=null){
	            String sFamily = convertFontDeclaration(sFontName);
	            if (sFamily!=null && !sFamily.equals(convertFontDeclaration(context.getFontName()))) {
	            	if (bDecl) { ba.add("\\"+sFamily+"family",""); }
	            	else { ba.add("\\text"+sFamily+"{","}"); }
	            }
	        }
        }
    }

    
    /** <p>Convert a font declaration to LaTeX.</p>
     *  <p>It returns a generic LaTeX font family (rm, tt, sf).</p>
     *  @param  sName the name of the font declaration
     *  @return <code>String</code> with a LaTeX generic fontfamily
     */
    private String convertFontDeclaration(String sName) {
        FontDeclaration fd = ofr.getFontDeclaration(sName);
        if (fd!=null) {
	        if (!fontDecls.containsKey(sName)) {
	            String sFontPitch = fd.getFontPitch();
	            String sFontFamilyGeneric = fd.getFontFamilyGeneric();			
	            fontDecls.put(sName,nfssFamily(sFontFamilyGeneric,sFontPitch));
	        }
	        return fontDecls.get(sName);
        }
        return null;
    }
    
    private static final String nfssFamily(String sFontFamilyGeneric, String sFontPitch){
		// All the values roman, decorative, script and system maps to rm
    	// because we have no LaTeX equivalent of the latter three
		if ("fixed".equals(sFontPitch)) return "tt";
		else if ("modern".equals(sFontFamilyGeneric)) return "tt";
		else if ("swiss".equals(sFontFamilyGeneric)) return "sf";
		else return "rm";
    }


}
