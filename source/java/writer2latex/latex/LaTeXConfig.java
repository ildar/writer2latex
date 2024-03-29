/************************************************************************
 *
 *  LaTeXConfig.java
 *
 *  Copyright: 2002-2022 by Henrik Just
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
 *  Version 2.0 (2022-08-11)
 *
 */

package writer2latex.latex;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Element;

import writer2latex.api.ComplexOption;
import writer2latex.base.BooleanOption;
import writer2latex.base.IntegerOption;
import writer2latex.base.Option;
import writer2latex.latex.util.HeadingMap;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.i18n.ReplacementTrie;
import writer2latex.latex.util.StyleMapItem;
import writer2latex.util.Misc;

public class LaTeXConfig extends writer2latex.base.ConfigBase {
	/////////////////////////////////////////////////////////////////////////
	// I. Define items needed by ConfigBase
	
    protected String getDefaultConfigPath() { return "/writer2latex/latex/config/"; } 
    
	/////////////////////////////////////////////////////////////////////////
	// II. Override getter and setter methods for simple options in order to: 
    //  - Treat the custom preamble like a regular option, even though the xml representation is different
    //  - Be backwards compatible (renamed the options keep_image_size, use_color) 
    
    @Override public void setOption(String sName,String sValue) {
    	if (sName.equals("custom-preamble")) {
    		sCustomPreamble = sValue;
    	}
    	else {
    		// these options have been renamed:
    		if (sName.equals("keep_image_size")) { sName = "original_image_size"; }
    		else if (sName.equals("use_color")) { sName = "use_xcolor"; }
    		super.setOption(sName, sValue);
    	}
    }
    
    @Override public String getOption(String sName) {
    	if (sName.equals("custom-preamble")) {
    		return sCustomPreamble;
    	}
    	else {
    		return super.getOption(sName);
    	}
    }
    
	/////////////////////////////////////////////////////////////////////////
    // III. Declare all constants
    
    // Backend
    public static final int GENERIC = 0;
    public static final int DVIPS = 1;
    public static final int PDFTEX = 2;
    public static final int UNSPECIFIED = 3;
    public static final int XETEX = 4;
    
    // Main script
    public static final int WESTERN = 0;
    public static final int CTL = 1;
    public static final int CJK = 2;
	
    // Formatting (must be ordered)
    public static final int IGNORE_ALL = 0;
    public static final int IGNORE_MOST = 1;
    public static final int CONVERT_BASIC = 2;
    public static final int CONVERT_MOST = 3;
    public static final int CONVERT_ALL = 4;
    
    // Handling of other formatting
    public static final int IGNORE = 0;
    public static final int ACCEPT = 1;
    public static final int WARNING = 2;
    public static final int ERROR = 3;
	
    // Notes
    //public static final int IGNORE = 0;
    public static final int COMMENT = 1;
    public static final int PDFANNOTATION = 2;
    public static final int MARGINPAR = 3;
    public static final int CUSTOM = 4;
	    
	/////////////////////////////////////////////////////////////////////////
    // IV. Our options data

    private ComplexOption headingMap;
    private ComplexOption parMap;
    private ComplexOption parBlockMap;
    private ComplexOption listMap;
    private ComplexOption listItemMap;
    private ComplexOption textMap;
    private ComplexOption textAttributeMap;
    private ComplexOption stringReplace;
    private ComplexOption mathSymbols;
    private String sCustomPreamble = "";
    
    // Cached versions of style maps
    private Map<String,StyleMapItem> parStyleMap = null;
    private Map<String,StyleMapItem> parBlockStyleMap = null;
    private Map<String,StyleMapItem> listStyleMap = null;
    private Map<String,StyleMapItem> listItemStyleMap = null;
    private Map<String,StyleMapItem> textAttributeStyleMap = null;
    private Map<String,StyleMapItem> textStyleMap = null;
    
	
	/////////////////////////////////////////////////////////////////////////
    // V. The rather long constructor setting all defaults
    
    /** Construct a new <code>LaTeXConfig</code> with default values for all options
     */
    public LaTeXConfig() {
        super();
        
        // create options with default values
        addOption(new BooleanOption("no_preamble","false"));
        addOption(new BooleanOption("no_index","false"));
        addOption(new Option("documentclass","article"));
        addOption(new Option("global_options",""));
        addOption(new IntegerOption("backend","pdftex") {
            public void setString(String sValue) {
                super.setString(sValue);
                if ("generic".equals(sValue)) nValue = GENERIC;
                else if ("dvips".equals(sValue)) nValue = DVIPS;
                else if ("pdftex".equals(sValue)) nValue = PDFTEX;
                else if ("unspecified".equals(sValue)) nValue = UNSPECIFIED;
                else if ("xetex".equals(sValue)) nValue = XETEX;
            }
        });
        addOption(new IntegerOption("inputencoding",ClassicI18n.writeInputenc(ClassicI18n.UTF8)) {
            public void setString(String sValue) {
                super.setString(sValue);
                nValue = ClassicI18n.readInputenc(sValue);
            }
        });
        addOption(new IntegerOption("script","western") {
            public void setString(String sValue) {
                super.setString(sValue);
                if ("western".equals(sValue)) nValue = WESTERN;
                if ("ctl".equals(sValue)) nValue = CTL;
                if ("cjk".equals(sValue)) nValue = CJK;
            }
        });
        addOption(new BooleanOption("multilingual","true"));
        addOption(new BooleanOption("greek_math","true"));
        addOption(new BooleanOption("use_pifont","false"));
        addOption(new BooleanOption("use_ifsym","false"));
        addOption(new BooleanOption("use_wasysym","false"));
        addOption(new BooleanOption("use_bbding","false"));
        addOption(new BooleanOption("use_eurosym","false"));
        addOption(new BooleanOption("use_tipa","false"));
        addOption(new BooleanOption("use_xcolor","true"));
        addOption(new BooleanOption("use_colortbl","false"));
        addOption(new BooleanOption("use_geometry","true"));
        addOption(new BooleanOption("use_fancyhdr","true"));
        addOption(new BooleanOption("use_perpage","true"));
        addOption(new BooleanOption("use_tikz","true"));
        addOption(new BooleanOption("use_longfbox","true"));
        addOption(new BooleanOption("use_titlesec","false"));
        addOption(new BooleanOption("use_parskip","false"));
        addOption(new BooleanOption("par_align","true"));
        addOption(new BooleanOption("use_enumitem","true"));
        addOption(new BooleanOption("list_layout","false"));
        addOption(new BooleanOption("list_styles","false"));
        addOption(new BooleanOption("use_hyperref","true"));
        addOption(new BooleanOption("use_microtype","false"));
        addOption(new BooleanOption("use_letterspace","false"));
        addOption(new BooleanOption("use_multicol","true"));
        addOption(new BooleanOption("multicols_format","false"));
        addOption(new BooleanOption("use_caption","true"));
        addOption(new BooleanOption("use_longtable","false"));
        addOption(new BooleanOption("use_supertabular","true"));
        addOption(new BooleanOption("use_tabulary","false"));
        addOption(new BooleanOption("use_endnotes","false"));
        addOption(new Option("notesname",""));
        addOption(new BooleanOption("use_ulem","false"));
        addOption(new BooleanOption("page_numbering","true"));
        addOption(new BooleanOption("page_color","false"));
        addOption(new BooleanOption("use_lastpage","false"));
        addOption(new BooleanOption("use_titleref","false"));
        addOption(new BooleanOption("use_biblatex","false"));
        addOption(new Option("biblatex_options",""));
        addOption(new Option("external_bibtex_files",""));
        addOption(new IntegerOption("bibtex_encoding","document") {
            public void setString(String sValue) {
                super.setString(sValue);
                if ("document".equals(sValue)) { nValue = -1; }
                else { nValue = ClassicI18n.readInputenc(sValue); }
            }
        });
        addOption(new Option("zotero_bibtex_files",""));
        addOption(new Option("jabref_bibtex_files",""));
        addOption(new BooleanOption("include_original_citations","false"));
        addOption(new Option("font","default"));
        addOption(new Option("fontspec","default"));
        addOption(new IntegerOption("formatting","convert_basic") {
            public void setString(String sValue) {
                super.setString(sValue);
                if ("convert_all".equals(sValue)) nValue = CONVERT_ALL;
                else if ("convert_most".equals(sValue)) nValue = CONVERT_MOST;
                else if ("convert_basic".equals(sValue)) nValue = CONVERT_BASIC;
                else if ("ignore_most".equals(sValue)) nValue = IGNORE_MOST;
                else if ("ignore_all".equals(sValue)) nValue = IGNORE_ALL;
            }
        });
        addOption(new BooleanOption("footnote_rule", "false"));
        addOption(new BooleanOption("notes_numbering", "false"));
        addOption(new BooleanOption("outline_numbering", "true"));
        addOption(new Option("border_radius","100%"));
        addOption(new ContentHandlingOption("other_styles","accept"));
        addOption(new BooleanOption("convert_index_names", "false"));
        addOption(new ContentHandlingOption("image_content","accept"));
        addOption(new ContentHandlingOption("table_content","accept"));
        addOption(new Option("table_first_head_style",""));
        addOption(new Option("table_head_style",""));
        addOption(new Option("table_foot_style",""));
        addOption(new Option("table_last_foot_style",""));
        addOption(new BooleanOption("ignore_hard_page_breaks","false"));
        addOption(new BooleanOption("ignore_hard_line_breaks","false"));
        addOption(new BooleanOption("ignore_empty_paragraphs","false"));
        addOption(new BooleanOption("ignore_double_spaces","false"));
        addOption(new BooleanOption("display_hidden_text","false"));
        addOption(new BooleanOption("align_frames","true"));
        addOption(new BooleanOption("float_figures","false"));
        addOption(new BooleanOption("float_tables","false"));
        addOption(new Option("float_options","h"));
        addOption(new BooleanOption("figure_sequence_name",""));
        addOption(new BooleanOption("table_sequence_name",""));
        addOption(new Option("image_options",""));
        addOption(new BooleanOption("remove_graphics_extension","false"));
        addOption(new BooleanOption("original_image_size","false"));
        addOption(new IntegerOption("simple_table_limit","0") {
           public void setString(String sValue) {
               super.setString(sValue);
               nValue = Misc.getPosInteger(sValue,0);
           }
        });
        addOption(new IntegerOption("notes","comment") {
            public void setString(String sValue) {
                super.setString(sValue);
                if ("ignore".equals(sValue)) nValue = IGNORE;
                else if ("comment".equals(sValue)) nValue = COMMENT;
                else if ("pdfannotation".equals(sValue)) nValue = PDFANNOTATION;
                else if ("marginpar".equals(sValue)) nValue = MARGINPAR;
                else nValue = CUSTOM;
            }
        });
        addOption(new BooleanOption("metadata","true"));
        addOption(new Option("tabstop",""));
        addOption(new IntegerOption("wrap_lines_after","120") {
            public void setString(String sValue) {
                super.setString(sValue);
                nValue = Misc.getPosInteger(sValue,0);
            }
        });
        addOption(new BooleanOption("split_linked_sections","false"));
        addOption(new BooleanOption("split_toplevel_sections","false"));
        addOption(new BooleanOption("save_images_in_subdir","false"));
        addOption(new BooleanOption("old_math_colors","false"));
        addOption(new BooleanOption("debug","false"));

        // Complex options - heading map
        headingMap = addComplexOption("heading-map");
        Map<String,String> attr = new HashMap<>();
        attr.put("name", "section");
        attr.put("level", "1");
        headingMap.put("1", attr);
        
        attr = new HashMap<>();
        attr.put("name", "subsection");
        attr.put("level", "2");
        headingMap.put("2", attr);
        
        attr = new HashMap<>();
        attr.put("name", "subsubsection");
        attr.put("level", "3");
        headingMap.put("3", attr);
        
        attr = new HashMap<>();
        attr.put("name", "paragraph");
        attr.put("level", "4");
        headingMap.put("4", attr);
        
        attr = new HashMap<>();
        attr.put("name", "subparagraph");
        attr.put("level", "5");
        headingMap.put("5", attr);
        
        // Complex options - style maps
        parMap = addComplexOption("paragraph-map");
        parBlockMap = addComplexOption("paragraph-block-map");
        listMap = addComplexOption("list-map");
        listItemMap = addComplexOption("listitem-map");
        textMap = addComplexOption("text-map");
        textAttributeMap = addComplexOption("text-attribute-map");
        
        // Complex options - string replace
        stringReplace=addComplexOption("string-replace");
        
        // Standard string replace:
        // Fix french spacing; replace nonbreaking space 
        // right before em-dash, !, ?, : and ; (babel handles this)
        attr = new HashMap<String,String>();
        attr.put("fontenc", "any");
        attr.put("latex-code", " \u2014");
        stringReplace.put("\u00A0\u2014",attr);

        attr = new HashMap<String,String>();
        attr.put("fontenc", "any");
        attr.put("latex-code", " !");
        stringReplace.put("\u00A0!",attr);

        attr = new HashMap<String,String>();
        attr.put("fontenc", "any");
        attr.put("latex-code", " ?");
        stringReplace.put("\u00A0?",attr);

        attr = new HashMap<String,String>();
        attr.put("fontenc", "any");
        attr.put("latex-code", " :");
        stringReplace.put("\u00A0:",attr);

        attr = new HashMap<String,String>();
        attr.put("fontenc", "any");
        attr.put("latex-code", " ;");
        stringReplace.put("\u00A0;",attr);
        
        // Right after opening guillemet and right before closing  guillemet:
        // Here we must *keep* the non-breaking space
        // TODO: Use \og and \fg if the document contains french...
        //stringReplace.put("\u00AB\u00A0","\u00AB ",I18n.readFontencs("any"));
        //stringReplace.put("\u00A0\u00BB"," \u00BB",I18n.readFontencs("any"));
        
        // Complex options - math user defined symbols
        mathSymbols = addComplexOption("math-symbol-map");
    }
    
	////////////////////////////////////////////////////////////////////////////
    // VI. Provide methods to fill in the gaps in the supers read and write methods
    	
    protected void readInner(Element elm) {
        if (elm.getTagName().equals("heading-map")) {
        	// Unlike other complex options, a heading map is completely replaced
            headingMap.clear();
        	Node child = elm.getFirstChild();
        	while (child!=null) {
        		if (child.getNodeType()==Node.ELEMENT_NODE) {
        			Element childElm = (Element) child;
        			if (childElm.getTagName().equals("heading-level-map")) {
        				if (childElm.hasAttribute("writer-level")) {
        					Map<String,String> attr = new HashMap<String,String>();
        					attr.put("name",childElm.getAttribute("name"));
        					attr.put("level",childElm.getAttribute("level"));
        					headingMap.put(childElm.getAttribute("writer-level"), attr);
        				}        				
        			}
        		}
        		child = child.getNextSibling();
        	}
        }
        else if (elm.getTagName().equals("style-map")) {
            String sName = elm.getAttribute("name");
            String sFamily = elm.getAttribute("family");
            if (sFamily.length()==0) { // try old name
                sFamily = elm.getAttribute("class");
            }

            Map<String,String> attr = new HashMap<String,String>();
            attr.put("before", elm.getAttribute("before"));
            attr.put("after", elm.getAttribute("after"));
            
            if ("paragraph".equals(sFamily)) {
            	if (elm.hasAttribute("line-break")) { attr.put("line-break", elm.getAttribute("line-break")); }
            	if (elm.hasAttribute("break-after")) { attr.put("break-after", elm.getAttribute("break-after")); }
            	if (elm.hasAttribute("verbatim")) { attr.put("verbatim", elm.getAttribute("verbatim")); }
                parMap.put(sName, attr);
            }
            if ("paragraph-block".equals(sFamily)) {
                attr.put("next", elm.getAttribute("next"));
            	if (elm.hasAttribute("nesting")) { attr.put("nesting", elm.getAttribute("nesting")); }
            	if (elm.hasAttribute("negative")) { attr.put("negative", elm.getAttribute("negative")); }
                attr.put("include", elm.getAttribute("include"));
            	if (elm.hasAttribute("verbatim")) { attr.put("verbatim", elm.getAttribute("verbatim")); }
                parBlockMap.put(sName, attr);
            }
            else if ("list".equals(sFamily)) {
            	listMap.put(sName, attr);
            }
            else if ("listitem".equals(sFamily)) {
                listItemMap.put(sName, attr);
            }
            else if ("text".equals(sFamily)) {
            	if (elm.hasAttribute("verbatim")) { attr.put("verbatim", elm.getAttribute("verbatim")); }
            	textMap.put(sName, attr);
            }
            else if ("text-attribute".equals(sFamily)) {
            	textAttributeMap.put(sName, attr);
            }
        }
        else if (elm.getTagName().equals("string-replace")) {
            String sInput = elm.getAttribute("input");
            Map<String,String> attributes = new HashMap<String,String>();
            attributes.put("latex-code", elm.getAttribute("latex-code"));
            if (elm.hasAttribute("fontenc") && elm.getAttribute("fontenc").length()>0) {
            	// The fontenc attribute is optional
            	attributes.put("fontenc", elm.getAttribute("fontenc"));
            }
            else {
            	attributes.put("fontenc", "any");
            }
            stringReplace.put(sInput,attributes);
        }
        else if (elm.getTagName().equals("math-symbol-map")) {
            String sName = elm.getAttribute("name");
            Map<String,String> attr = new HashMap<String,String>();
            attr.put("latex", elm.getAttribute("latex"));
            mathSymbols.put(sName, attr);
        }
        else if (elm.getTagName().equals("custom-preamble")) {
        	StringBuilder buf = new StringBuilder();
            Node child = elm.getFirstChild();
            while (child!=null) {
                if (child.getNodeType()==Node.TEXT_NODE) {
                    buf.append(child.getNodeValue());
                }
                child = child.getNextSibling();
            }
            sCustomPreamble = buf.toString();
        }
    }

    protected void writeInner(Document dom) {
        // Write heading map
    	int nMaxLevel = 0;
    	while (nMaxLevel<10 && headingMap.get(Integer.toString(nMaxLevel+1))!=null) { nMaxLevel++; }
    	
        Element hmNode = dom.createElement("heading-map");
        // This attribute is not used anymore, but we keep it for backwards compatibility
        hmNode.setAttribute("max-level",Integer.toString(nMaxLevel));
        dom.getDocumentElement().appendChild(hmNode);
        for (int i=1; i<=nMaxLevel; i++) {
            Element hlmNode = dom.createElement("heading-level-map");
            String sWriterLevel = Integer.toString(i);
            hlmNode.setAttribute("writer-level",sWriterLevel);
            Map<String,String> attr = headingMap.get(sWriterLevel);
            hlmNode.setAttribute("name",attr.get("name"));
            hlmNode.setAttribute("level",attr.get("level"));
            hmNode.appendChild(hlmNode);
        }
        
    	// Write style maps
        writeStyleMap(dom,parMap,"paragraph");
        writeStyleMap(dom,parBlockMap,"paragraph-block");
        writeStyleMap(dom,listMap,"list");
        writeStyleMap(dom,listItemMap,"listitem");
        writeStyleMap(dom,textMap,"text");
        writeStyleMap(dom,textAttributeMap,"text-attribute");

        // Write string replace
        Set<String> inputStrings = stringReplace.keySet();
        for (String sInput : inputStrings) {
        	Map<String,String> attributes = stringReplace.get(sInput);
            Element srNode = dom.createElement("string-replace");
            srNode.setAttribute("input",sInput);
            srNode.setAttribute("latex-code",attributes.get("latex-code"));
            srNode.setAttribute("fontenc",attributes.get("fontenc"));
            dom.getDocumentElement().appendChild(srNode);
        }
		
        // Write math symbol map
    	for (String sName : mathSymbols.keySet()) {
            String sLatex = mathSymbols.get(sName).get("latex");
            Element msNode = dom.createElement("math-symbol-map");
            msNode.setAttribute("name",sName);
	        msNode.setAttribute("latex",sLatex);
            dom.getDocumentElement().appendChild(msNode);
        }

    	// Write custom preamble
    	Element cp = dom.createElement("custom-preamble");
        cp.appendChild(dom.createTextNode( sCustomPreamble));
        dom.getDocumentElement().appendChild(cp);
    }

    private void writeStyleMap(Document dom, ComplexOption co, String sFamily) {
    	for (String sName : co.keySet()) {
    		Map<String,String> attr = co.get(sName);
            Element smNode = dom.createElement("style-map");
            smNode.setAttribute("name",sName);
	        smNode.setAttribute("family",sFamily);
            smNode.setAttribute("before",attr.containsKey("before") ? attr.get("before") : "");
            smNode.setAttribute("after",attr.containsKey("after") ? attr.get("after") : "");
            if (attr.containsKey("next")) {
                smNode.setAttribute("next",attr.get("next"));
            }
            if (attr.containsKey("nesting")) {
                smNode.setAttribute("nesting",attr.get("nesting"));
            }
            if (attr.containsKey("negative")) {
                smNode.setAttribute("negative",attr.get("negative"));
            }
            if (attr.containsKey("include")) {
                smNode.setAttribute("include",attr.get("include"));
            }
            if (attr.containsKey("line-break")) {
                smNode.setAttribute("line-break",attr.get("line-break"));
            }
            if (attr.containsKey("break-after")) {
            	smNode.setAttribute("break-after", attr.get("break-after"));
            }
            if (attr.containsKey("verbatim")) {
                smNode.setAttribute("verbatim",attr.get("verbatim"));
            }
            dom.getDocumentElement().appendChild(smNode);
        }
    }
	
	/////////////////////////////////////////////////////////////////////////
    // VII. Convenience accessor methods
    
    public HeadingMap getHeadingMap() {
    	String s = replaceParameters(options.get("documentclass").getString());
    	if (s.equals("article*")) {
        	// Magic documentclass: Use a HeadingMap suitable for articles
    		HeadingMap map = new HeadingMap(5);
    		map.setLevelData(1, "section", 1);
    		map.setLevelData(2, "subsection", 2);
    		map.setLevelData(3, "subsubsection", 3);
    		map.setLevelData(4, "paragraph", 4);
    		map.setLevelData(5, "subparagraph", 5);
    		return map;
    	} else if (s.equals("report*") || s.equals("book*")) {
        	// Magic documentclass: Use a HeadingMap suitable for reports and books
    		HeadingMap map = new HeadingMap(6);
    		map.setLevelData(1, "chapter", 0);
    		map.setLevelData(2, "section", 1);
    		map.setLevelData(3, "subsection", 2);
    		map.setLevelData(4, "subsubsection", 3);
    		map.setLevelData(5, "paragraph", 4);
    		map.setLevelData(6, "subparagraph", 5);
    		return map;    		
    	} else {
        	// Repackage the heading-map option as a HeadingMap
        	int nMaxLevel = 0;
        	while (nMaxLevel<10 && headingMap.get(Integer.toString(nMaxLevel+1))!=null) { nMaxLevel++; }

        	HeadingMap map = new HeadingMap(nMaxLevel);
            for (int i=1; i<=nMaxLevel; i++) {
                String sWriterLevel = Integer.toString(i);
                Map<String,String> attr = headingMap.get(sWriterLevel);
                String sName = attr.get("name");
                int nLevel = Misc.getPosInteger(attr.get("level"),0);
                map.setLevelData(i, sName, nLevel);
            }
            return map;	
    	}    	
    }
    
    // Get style maps
    public Map<String,StyleMapItem> getParStyleMap() {
    	if (parStyleMap==null) { parStyleMap = createStyleMap(parMap); }
    	return parStyleMap;
    }
    public Map<String,StyleMapItem> getParBlockStyleMap() {
    	if (parBlockStyleMap==null) { parBlockStyleMap = createStyleMap(parBlockMap); }
    	return parBlockStyleMap;    	
    }
    public Map<String,StyleMapItem> getListStyleMap() {
    	if (listStyleMap==null) { listStyleMap = createStyleMap(listMap); }
    	return listStyleMap;
    }
    public Map<String,StyleMapItem> getListItemStyleMap() {
    	if (listItemStyleMap==null) { listItemStyleMap = createStyleMap(listItemMap); }
    	return listItemStyleMap;
    }
    public Map<String,StyleMapItem> getTextAttributeStyleMap() {
    	if (textAttributeStyleMap==null) { textAttributeStyleMap = createStyleMap(textAttributeMap); }
    	return textAttributeStyleMap;
    }
    public Map<String,StyleMapItem> getTextStyleMap() {
    	if (textStyleMap==null) { textStyleMap = createStyleMap(textMap); }
    	return textStyleMap;    	
    }
    
    // TODO: Cache the generated style map!!
    private Map<String,StyleMapItem> createStyleMap(ComplexOption co) {
    	Map<String,StyleMapItem> map = new HashMap<>();
    	for (String sName : co.keySet()) {
    		Map<String,String> attr = co.get(sName);
    		String sBefore = attr.containsKey("before") ? attr.get("before") : "";
    		String sAfter = attr.containsKey("after") ? attr.get("after") : "";
    		String[] sNext = attr.containsKey("next") ? attr.get("next").split(";") : new String[0];
    		boolean bNesting = attr.containsKey("nesting") && attr.get("nesting").equals("true");
    		boolean bNegative = attr.containsKey("negative") && attr.get("negative").equals("true");
    		String[] sInclude = attr.containsKey("include") ? attr.get("include").split(";") : new String[0];
    		boolean bLineBreak = !"false".equals(attr.get("line-break"));
    		int nBreakAfter = StyleMapItem.PAR;
    		String sBreakAfter = attr.get("break-after");
    		if ("none".equals(sBreakAfter)) { nBreakAfter = StyleMapItem.NONE; }
    		else if ("line".equals(sBreakAfter)) { nBreakAfter = StyleMapItem.LINE; }
    		boolean bVerbatim = "true".equals(attr.get("verbatim"));
    		StyleMapItem smi = new StyleMapItem(sName, sBefore, sAfter, bLineBreak, nBreakAfter, bVerbatim);
    		map.put(sName, smi);
    		for (int i=0; i<sNext.length; i++) {
    			smi.addNext(sNext[i]);
    		}
    		smi.setNesting(bNesting);
    		smi.setNegative(bNegative);
    		for (int i=0; i<sInclude.length; i++) {
    			smi.addInclude(sInclude[i]);
    		}
    	}
    	return map;
    }

    // Return current string replace as a trie
    public ReplacementTrie getStringReplace() {
        ReplacementTrie trie = new ReplacementTrie();
        for (String sInput : stringReplace.keySet()) {
        	Map<String,String> attributes = stringReplace.get(sInput);
            String sLaTeXCode = attributes.get("latex-code");
            String sFontenc = attributes.get("fontenc");
            trie.put(sInput,sLaTeXCode!=null ? sLaTeXCode : "",
            		 ClassicI18n.readFontencs(sFontenc!=null ? sFontenc : "any"));
        }
        return trie;
    }
    
    // Get the math symbols as a simple Map
    public Map<String, String> getMathSymbols() {
    	Map<String,String> map = new HashMap<String,String>();
    	for (String sName : mathSymbols.keySet()) {
    		String sLatex = mathSymbols.get(sName).get("latex");
    		map.put(sName, sLatex);
    	}
    	return map;
    }

    // Get the custom preamble
    public String getCustomPreamble() { return replaceParameters(sCustomPreamble); }

    // Common options
    public boolean debug() { return ((BooleanOption) options.get("debug")).getValue(); }

    // General options
    public String documentclass() { // Replace parameters and check for magic values
    	String s = replaceParameters(options.get("documentclass").getString());
    	if (s.equals("article*")) {
    		s = "article";
    	} else if (s.equals("report*")) {
    		s = "report";
    	} else if (s.equals("book*")) {
    		s = "book";
    	}
    	return s;
    }
    public String globalOptions() { return replaceParameters(options.get("global_options").getString()); }
    public int backend() { return ((IntegerOption) options.get("backend")).getValue(); }
    public int inputencoding() { return ((IntegerOption) options.get("inputencoding")).getValue(); }
    public int script() { return ((IntegerOption) options.get("script")).getValue(); }
    public boolean multilingual() { return ((BooleanOption) options.get("multilingual")).getValue(); }
    public boolean greekMath() { return ((BooleanOption) options.get("greek_math")).getValue(); }
    public boolean noPreamble() { return ((BooleanOption) options.get("no_preamble")).getValue(); }
    public boolean noIndex() { return ((BooleanOption) options.get("no_index")).getValue(); }
	
    // Package options
    public boolean usePifont() { return ((BooleanOption) options.get("use_pifont")).getValue(); }
    public boolean useIfsym() { return ((BooleanOption) options.get("use_ifsym")).getValue(); }
    public boolean useWasysym() { return ((BooleanOption) options.get("use_wasysym")).getValue(); }
    public boolean useBbding() { return ((BooleanOption) options.get("use_bbding")).getValue(); }
    public boolean useEurosym() { return ((BooleanOption) options.get("use_eurosym")).getValue(); }
    public boolean useTipa() { return ((BooleanOption) options.get("use_tipa")).getValue(); }
    public boolean useXcolor() { return ((BooleanOption) options.get("use_xcolor")).getValue(); }
    public boolean useColortbl() { return ((BooleanOption) options.get("use_colortbl")).getValue(); }
    public boolean useGeometry() { return ((BooleanOption) options.get("use_geometry")).getValue(); }
    public boolean useFancyhdr() { return ((BooleanOption) options.get("use_fancyhdr")).getValue(); }
    public boolean usePerpage() { return ((BooleanOption) options.get("use_perpage")).getValue(); }
    public boolean useTikz() { return ((BooleanOption) options.get("use_tikz")).getValue(); }
    public boolean useLongfbox() { return ((BooleanOption) options.get("use_longfbox")).getValue(); }
    public boolean useTitlesec() { return ((BooleanOption) options.get("use_titlesec")).getValue(); }
    public boolean useParskip() { return ((BooleanOption) options.get("use_parskip")).getValue(); }
    public boolean parAlign() { return ((BooleanOption) options.get("par_align")).getValue(); }
    public boolean useEnumitem() { return ((BooleanOption) options.get("use_enumitem")).getValue(); }
    public boolean listLayout() { return ((BooleanOption) options.get("list_layout")).getValue(); }
    public boolean listStyles() { return ((BooleanOption) options.get("list_styles")).getValue(); }
    public boolean useHyperref() { return ((BooleanOption) options.get("use_hyperref")).getValue(); }
    public boolean useMicrotype() { return ((BooleanOption) options.get("use_microtype")).getValue(); }
    public boolean useLetterspace() { return ((BooleanOption) options.get("use_letterspace")).getValue(); }
    public boolean useMulticol() { return ((BooleanOption) options.get("use_multicol")).getValue(); }
    public boolean multicolsFormat() { return ((BooleanOption) options.get("multicols_format")).getValue(); }
    public boolean useCaption() { return ((BooleanOption) options.get("use_caption")).getValue(); }
    public boolean useLongtable() { return ((BooleanOption) options.get("use_longtable")).getValue(); }
    public boolean useSupertabular() { return ((BooleanOption) options.get("use_supertabular")).getValue(); }
    public boolean useTabulary() { return ((BooleanOption) options.get("use_tabulary")).getValue(); }
    public boolean useEndnotes() { return ((BooleanOption) options.get("use_endnotes")).getValue(); }
    public String notesname() { return options.get("notesname").getString(); }
    public boolean useUlem() { return ((BooleanOption) options.get("use_ulem")).getValue(); }
    public boolean pageNumbering() { return ((BooleanOption) options.get("page_numbering")).getValue(); }
    public boolean pageColor() { return ((BooleanOption) options.get("page_color")).getValue(); }
    public boolean useLastpage() { return ((BooleanOption) options.get("use_lastpage")).getValue(); }
    public boolean useTitleref() { return ((BooleanOption) options.get("use_titleref")).getValue(); }
    public boolean useBiblatex() { return ((BooleanOption) options.get("use_biblatex")).getValue(); }
    public String biblatexOptions() { return options.get("biblatex_options").getString(); }
    public String externalBibtexFiles() { return options.get("external_bibtex_files").getString(); }
    public int bibtexEncoding() { return ((IntegerOption) options.get("bibtex_encoding")).getValue(); }
    public String zoteroBibtexFiles() { return options.get("zotero_bibtex_files").getString(); }
    public String jabrefBibtexFiles() { return options.get("jabref_bibtex_files").getString(); }
    public boolean includeOriginalCitations() { return ((BooleanOption) options.get("include_original_citations")).getValue(); }
	
    // Formatting options
    public String font() { return options.get("font").getString(); }
    public String fontspec() { return options.get("fontspec").getString(); }
    public int formatting() { return ((IntegerOption) options.get("formatting")).getValue(); }
    public boolean footnoteRule() { return ((BooleanOption) options.get("footnote_rule")).getValue(); }
    public boolean notesNumbering() { return ((BooleanOption) options.get("notes_numbering")).getValue(); }
    public boolean outlineNumbering() { return ((BooleanOption) options.get("outline_numbering")).getValue(); }
    public String borderRadius() { return options.get("border_radius").getString(); }
    public int otherStyles() { return ((IntegerOption) options.get("other_styles")).getValue(); }
    public boolean convertIndexNames() { return ((BooleanOption) options.get("convert_index_names")).getValue(); }
    public int imageContent() { return ((IntegerOption) options.get("image_content")).getValue(); }
    public int tableContent() { return ((IntegerOption) options.get("table_content")).getValue(); }
    public String tableFirstHeadStyle() { return options.get("table_first_head_style").getString(); }
    public String tableHeadStyle() { return options.get("table_head_style").getString(); }
    public String tableFootStyle() { return options.get("table_foot_style").getString(); }
    public String tableLastFootStyle() { return options.get("table_last_foot_style").getString(); }
    public boolean ignoreHardPageBreaks() { return ((BooleanOption) options.get("ignore_hard_page_breaks")).getValue(); }
    public boolean ignoreHardLineBreaks() { return ((BooleanOption) options.get("ignore_hard_line_breaks")).getValue(); }
    public boolean ignoreEmptyParagraphs() { return ((BooleanOption) options.get("ignore_empty_paragraphs")).getValue(); }
    public boolean ignoreDoubleSpaces() { return ((BooleanOption) options.get("ignore_double_spaces")).getValue(); }
    public boolean displayHiddenText() { return ((BooleanOption) options.get("display_hidden_text")).getValue(); }

    // Graphics options
    public boolean alignFrames() { return ((BooleanOption) options.get("align_frames")).getValue(); }
    public boolean floatFigures() { return ((BooleanOption) options.get("float_figures")).getValue(); }
    public boolean floatTables() { return ((BooleanOption) options.get("float_tables")).getValue(); }
    public String floatOptions() { return options.get("float_options").getString(); }
    public String figureSequenceName() { return options.get("figure_sequence_name").getString(); }
    public String tableSequenceName() { return options.get("table_sequence_name").getString(); }
    public String imageOptions() { return options.get("image_options").getString(); }
    public boolean removeGraphicsExtension() { return ((BooleanOption) options.get("remove_graphics_extension")).getValue(); }
    public boolean originalImageSize() { return ((BooleanOption) options.get("original_image_size")).getValue(); }
	
    // Tables
    public int simpleTableLimit() { return ((IntegerOption) options.get("simple_table_limit")).getValue(); }
	
    // Notes
    public int notes() { return ((IntegerOption) options.get("notes")).getValue(); }
    public String notesCommand() { return options.get("notes").getString(); }
	
    // Metadata
    public boolean metadata() { return ((BooleanOption) options.get("metadata")).getValue(); }
	
    // Tab stops
    public String tabstop() { return options.get("tabstop").getString(); }
	
    // Files
    public int wrapLinesAfter() { return ((IntegerOption) options.get("wrap_lines_after")).getValue(); }
    public boolean splitLinkedSections() { return ((BooleanOption) options.get("split_linked_sections")).getValue(); }
    public boolean splitToplevelSections() { return ((BooleanOption) options.get("split_toplevel_sections")).getValue(); }
    public boolean saveImagesInSubdir() { return ((BooleanOption) options.get("save_images_in_subdir")).getValue(); }
	
    // Compatibility options
    public boolean oldMathColors() { return ((BooleanOption) options.get("old_math_colors")).getValue(); }
}

