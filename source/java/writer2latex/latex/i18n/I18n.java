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
 *  Version 2.0 (2018-04-14) 
 * 
 */

package writer2latex.latex.i18n;

import java.io.InputStream;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import writer2latex.office.*;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.LaTeXDocumentPortion;
import writer2latex.latex.ConverterPalette;
import writer2latex.latex.util.BeforeAfter;

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

    // Collected data
    protected HashSet<String> languages = new HashSet<String>(); // All western languages used
    protected String sDefaultLanguage; // The default western ISO language to use
    protected String sDefaultCountry; // The default western ISO country to use
    
    // Unicode translation
    protected UnicodeTable table; // currently active table (top of stack)
    private Hashtable<String,UnicodeTable> tableSet; // all tables
    private Stack<UnicodeTable> tableStack; // stack of active tables

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
    
    protected void readSymbols(String sSymbols) {
        tableSet = new Hashtable<String,UnicodeTable>();
        UnicodeTableHandler handler=new UnicodeTableHandler(tableSet, sSymbols);
        SAXParserFactory factory=SAXParserFactory.newInstance();
        InputStream is = this.getClass().getResourceAsStream("symbols.xml");
        try {
            SAXParser saxParser=factory.newSAXParser();
            saxParser.parse(is,handler);
        }
        catch (Throwable t){
		    System.err.println("Oops - Unable to read symbols.xml");
            t.printStackTrace();
        }
        // put root table at top of stack
        tableStack = new Stack<UnicodeTable>();
        tableStack.push(tableSet.get("root"));
        table = tableSet.get("root");
    }
	
    /** Add declarations to the preamble to load the required packages
     *  @param pack usepackage declarations
     *  @param decl other declarations
     */
    public abstract void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl);
	
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
}
