/************************************************************************
 *
 *  XhtmlConfig.java
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
 *  Version 2.0 (2018-06-23)
 *
 */

package writer2latex.xhtml;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import writer2latex.api.ComplexOption;
import writer2latex.base.BooleanOption;
import writer2latex.base.IntegerOption;
import writer2latex.base.Option;
import writer2latex.util.Misc;

public class XhtmlConfig extends writer2latex.base.ConfigBase {
    // Implement configuration methods
    protected String getDefaultConfigPath() { return "/writer2latex/xhtml/config/"; }
	
    // Override setOption: To be backwards compatible, we must accept options
    // with the prefix xhtml_
    public void setOption(String sName,String sValue) {
        if (sName.startsWith("xhtml_")) { sName = sName.substring(6); }
        // this option has been renamed:
        if (sName.equals("keep_image_size")) { sName = "original_image_size"; }
        // and later renamed and extended:
        if (sName.equals("original_image_size")) {
        	sName = "image_size";
        	if (sValue.equals("true")) { sValue = "none"; }
        	else { sValue="absolute"; }
        }
        // this option has been renamed and extended:
        if (sName.equals("use_list_hack")) {
        	sName = "list_formatting";
        	if (sValue.equals("true")) { sValue = "css1_hack"; }
        	else { sValue = "css1"; }
        }
        // this option has been renamed and extended
        if (sName.equals("ignore_table_dimensions")) {
        	sName = "table_size";
        	if (sValue.equals("true")) { sValue="none"; }
        	else { sValue="absolute"; }
        }
        // this option has been renamed and extended
        if (sName.equals("convert_to_px")) {
        	sName = "units";
        	if (sValue.equals("true")) { sValue="px"; }
        	else { sValue="original"; }
        }
        
        super.setOption(sName, sValue);
    }
 
    // Formatting
    public static final int IGNORE_ALL = 0;
    public static final int IGNORE_STYLES = 1;
    public static final int IGNORE_HARD = 2;
    public static final int CONVERT_ALL = 3;
    
    // Units
    public static final int ORIGINAL = 0;
    public static final int PX = 1;
    public static final int REM = 2;
    
    // List formatting
    public static final int CSS1 = 0;
    public static final int CSS1_HACK = 1;
    public static final int HARD_LABELS = 2;
    
    // Image and table dimensions
    public static final int NONE = 0;
    public static final int ABSOLUTE = 1;
    public static final int RELATIVE = 2;
    
    // Page breaks
    // public static final int NONE = 0;
    public static final int STYLES = 1;
    public static final int EXPLICIT = 2;
    public static final int ALL = 3;
    

    protected ComplexOption xheading = addComplexOption("heading-map");
    protected ComplexOption xpar = addComplexOption("paragraph-map");
    protected ComplexOption xtext = addComplexOption("text-map");
    protected ComplexOption xframe = addComplexOption("frame-map");
    protected ComplexOption xlist = addComplexOption("list-map");
    protected ComplexOption xattr = addComplexOption("text-attribute-map");
	
    public XhtmlConfig() {
        super();
        // create options with default values
        addOption(new BooleanOption("ignore_hard_line_breaks","false"));
        addOption(new BooleanOption("ignore_empty_paragraphs","false"));
        addOption(new BooleanOption("ignore_double_spaces","false"));
        addOption(new IntegerOption("image_size","auto") {
        	@Override public void setString(String sValue) {
        		super.setString(sValue);
        		if ("relative".equals(sValue)) { nValue = RELATIVE; }
        		else if ("none".equals(sValue)) { nValue = NONE; }
        		else if ("original_image_size".equals(sValue)) { nValue = NONE; }
        		else { nValue = ABSOLUTE; }
        	}
        });
        addOption(new BooleanOption("no_doctype","false"));
        addOption(new BooleanOption("add_bom","false"));
        addOption(new Option("encoding","UTF-8") {
        	@Override public void setString(String sValue) {
        		if ("US-ASCII".equalsIgnoreCase(sValue)) {
        			super.setString(sValue);
        		}
        		else {
        			super.setString("UTF-8");
        		}
        	}
        });
        addOption(new BooleanOption("use_named_entities","false"));
        addOption(new BooleanOption("hexadecimal_entities","true"));
        addOption(new BooleanOption("pretty_print","true"));
        addOption(new BooleanOption("multilingual","true"));
        addOption(new Option("template_ids",""));
        addOption(new BooleanOption("separate_stylesheet","false"));
        addOption(new Option("custom_stylesheet",""));
        addOption(new XhtmlFormatOption("formatting","convert_all"));
        addOption(new XhtmlFormatOption("frame_formatting","convert_all"));
        addOption(new XhtmlFormatOption("section_formatting","convert_all"));
        addOption(new XhtmlFormatOption("table_formatting","convert_all"));
        addOption(new IntegerOption("table_size","auto") {
        	@Override public void setString(String sValue) {
        		super.setString(sValue);
        		if ("relative".equals(sValue)) { nValue = RELATIVE; }
        		else if ("none".equals(sValue)) { nValue = NONE; }
        		else { nValue = ABSOLUTE; }
        	}
        });
        addOption(new IntegerOption("list_formatting","css1") {
        	@Override public void setString(String sValue) {
        		super.setString(sValue);
        		if ("css1_hack".equals(sValue)) { nValue = CSS1_HACK; }
        		else if ("hard_labels".equals(sValue)) { nValue = HARD_LABELS; }
        		else { nValue = CSS1; }
        	}
        });
        addOption(new Option("max_width","800px"));
        addOption(new BooleanOption("use_default_font","false"));
        addOption(new BooleanOption("default_font_name",""));
        addOption(new BooleanOption("use_dublin_core","true"));
        addOption(new BooleanOption("notes","true"));
        addOption(new BooleanOption("display_hidden_text", "false"));
        addOption(new IntegerOption("units","rem") {
        	@Override public void setString(String sValue) {
        		super.setString(sValue);
        		if ("original".equals(sValue)) { nValue = ORIGINAL; }
        		else if ("px".equals(sValue)) { nValue = PX; }
        		else { nValue = REM; }
        	}
        });
        addOption(new Option("scaling","100%"));
        addOption(new Option("column_scaling","100%"));
        addOption(new BooleanOption("float_objects","true"));
        addOption(new Option("tabstop_style",""));
        addOption(new Option("endnotes_heading",""));
        addOption(new Option("footnotes_heading",""));
        addOption(new BooleanOption("include_toc","true"));
        addOption(new IntegerOption("split_level","0") {
        	@Override public void setString(String sValue) {
                super.setString(sValue);
                nValue = Misc.getPosInteger(sValue,0);
            }
        });
        addOption(new IntegerOption("repeat_levels","5") {
        	@Override public void setString(String sValue) {
                super.setString(sValue);
                nValue = Misc.getPosInteger(sValue,0);
            }
        });
        addOption(new IntegerOption("page_break_split", "none") {
        	@Override public void setString(String sValue) {
        		super.setString(sValue);
        		if ("styles".equals(sValue)) { nValue = STYLES; }
        		else if ("explicit".equals(sValue)) { nValue = EXPLICIT; }
        		else if ("all".equals(sValue)) { nValue = ALL; }
        		else { nValue = NONE; }
        	}
        });
        addOption(new BooleanOption("embed_svg","false"));
        addOption(new BooleanOption("embed_img","false"));
        addOption(new BooleanOption("use_mathjax","false"));
        addOption(new BooleanOption("calc_split","false"));
        addOption(new BooleanOption("display_hidden_sheets", "false"));
        addOption(new BooleanOption("display_hidden_rows_cols","false"));
        addOption(new BooleanOption("display_filtered_rows_cols","false"));
        addOption(new BooleanOption("apply_print_ranges","false"));
        addOption(new BooleanOption("use_title_as_heading","true"));
        addOption(new BooleanOption("use_sheet_names_as_headings","true"));
        addOption(new BooleanOption("save_images_in_subdir","false"));
        addOption(new Option("uplink",""));
    }
    
	protected void readInner(Element elm) {
        if (elm.getTagName().equals("xhtml-style-map")) {
            String sName = elm.getAttribute("name");
            String sFamily = elm.getAttribute("family");
            if (sFamily.length()==0) { // try old name
                sFamily = elm.getAttribute("class");
            }
            Map<String,String> attr = new HashMap<String,String>();

            String sElement = elm.getAttribute("element");
            String sCss = elm.getAttribute("css");
            if (sCss.length()==0) { sCss="(none)"; }
            attr.put("element", sElement);
            attr.put("css", sCss);

            String sBlockElement = elm.getAttribute("block-element");
            String sBlockCss = elm.getAttribute("block-css");
            if (sBlockCss.length()==0) { sBlockCss="(none)"; }
            
            String sBefore = elm.getAttribute("before");
            String sAfter = elm.getAttribute("after");
            
            if ("heading".equals(sFamily)) {
                attr.put("block-element", sBlockElement);
                attr.put("block-css", sBlockCss);
                attr.put("before", sBefore);
                attr.put("after", sAfter);
                xheading.put(sName,attr);
            }
            if ("paragraph".equals(sFamily)) {
                attr.put("block-element", sBlockElement);
                attr.put("block-css", sBlockCss);
                attr.put("before", sBefore);
                attr.put("after", sAfter);
                xpar.put(sName,attr);
            }
            else if ("text".equals(sFamily)) {
                attr.put("before", sBefore);
                attr.put("after", sAfter);
                xtext.put(sName,attr);
            }
            else if ("frame".equals(sFamily)) {
                xframe.put(sName,attr);
            }
            else if ("list".equals(sFamily)) {
                xlist.put(sName,attr);
            }
            else if ("attribute".equals(sFamily)) {
                xattr.put(sName,attr);
            }
        }
    }

    protected void writeInner(Document dom) {
        writeXStyleMap(dom,xheading,"heading");
        writeXStyleMap(dom,xpar,"paragraph");
        writeXStyleMap(dom,xtext,"text");
        writeXStyleMap(dom,xlist,"list");
        writeXStyleMap(dom,xframe,"frame");
        writeXStyleMap(dom,xattr,"attribute");
    }
	
    private void writeXStyleMap(Document dom, ComplexOption option, String sFamily) {
        Iterator<String> iter = option.keySet().iterator();
        while (iter.hasNext()) {
            String sName = iter.next();
            Element smNode = dom.createElement("xhtml-style-map");
            smNode.setAttribute("name",sName);
	        smNode.setAttribute("family",sFamily);
            Map<String,String> attr = option.get(sName);
            smNode.setAttribute("element",attr.get("element"));
            smNode.setAttribute("css",attr.get("css"));
            if (attr.containsKey("block-element")) smNode.setAttribute("block-element",attr.get("block-element"));
            if (attr.containsKey("block-css")) smNode.setAttribute("block-css",attr.get("block-css"));
            if (attr.containsKey("before")) smNode.setAttribute("before",attr.get("before"));
            if (attr.containsKey("after")) smNode.setAttribute("after",attr.get("after"));
            dom.getDocumentElement().appendChild(smNode);
        }
    }

    // Convenience accessor methods
    public boolean ignoreHardLineBreaks() { return ((BooleanOption) options.get("ignore_hard_line_breaks")).getValue(); }
    public boolean ignoreEmptyParagraphs() { return ((BooleanOption) options.get("ignore_empty_paragraphs")).getValue(); }
    public boolean ignoreDoubleSpaces() { return ((BooleanOption) options.get("ignore_double_spaces")).getValue(); }
    public int imageSize() { return ((IntegerOption) options.get("image_size")).getValue(); }
    public boolean noDoctype() { return ((BooleanOption) options.get("no_doctype")).getValue(); }
    public boolean addBOM() { return ((BooleanOption) options.get("add_bom")).getValue(); }
    public String encoding() { return options.get("encoding").getString(); }
    public boolean useNamedEntities() { return ((BooleanOption) options.get("use_named_entities")).getValue(); }
    public boolean hexadecimalEntities() { return ((BooleanOption) options.get("hexadecimal_entities")).getValue(); }
    public boolean prettyPrint() { return ((BooleanOption) options.get("pretty_print")).getValue(); }
    public boolean multilingual() { return ((BooleanOption) options.get("multilingual")).getValue(); }
    public String templateIds() { return options.get("template_ids").getString(); }
    public boolean separateStylesheet() { return ((BooleanOption) options.get("separate_stylesheet")).getValue(); }
    public String customStylesheet() { return replaceParameters(options.get("custom_stylesheet").getString()); }
    public int formatting() { return ((XhtmlFormatOption) options.get("formatting")).getValue(); }
    public int frameFormatting() { return ((XhtmlFormatOption) options.get("frame_formatting")).getValue(); }
    public int sectionFormatting() { return ((XhtmlFormatOption) options.get("section_formatting")).getValue(); }
    public int tableFormatting() { return ((XhtmlFormatOption) options.get("table_formatting")).getValue(); }
    public int tableSize() { return ((IntegerOption) options.get("table_size")).getValue(); }
    public int listFormatting() { return ((IntegerOption) options.get("list_formatting")).getValue(); }
    public String maxWidth() { return options.get("max_width").getString(); }
    public boolean useDefaultFont() { return ((BooleanOption) options.get("use_default_font")).getValue(); }
    public String defaultFontName() { return options.get("default_font_name").getString(); }
    public boolean useDublinCore() { return ((BooleanOption) options.get("use_dublin_core")).getValue(); }
    public boolean notes() { return ((BooleanOption) options.get("notes")).getValue(); }
    public boolean displayHiddenText() { return ((BooleanOption) options.get("display_hidden_text")).getValue(); }
    public int units() { return ((IntegerOption) options.get("units")).getValue(); }
    public String scaling() { return options.get("scaling").getString(); }
    public String columnScaling() { return options.get("column_scaling").getString(); }
    public boolean floatObjects() { return ((BooleanOption) options.get("float_objects")).getValue(); }
    public String tabstopStyle() { return options.get("tabstop_style").getString(); }
    public String endnotesHeading() { return options.get("endnotes_heading").getString(); }
    public String footnotesHeading() { return options.get("footnotes_heading").getString(); }
    public boolean includeToc() { return ((BooleanOption) options.get("include_toc")).getValue(); }
    public int splitLevel() { return ((IntegerOption) options.get("split_level")).getValue(); }
    public int repeatLevels() { return ((IntegerOption) options.get("repeat_levels")).getValue(); }
    public int pageBreakSplit() { return ((IntegerOption) options.get("page_break_split")).getValue(); }
    public boolean embedSVG() { return ((BooleanOption) options.get("embed_svg")).getValue(); }
    public boolean embedImg() { return ((BooleanOption) options.get("embed_img")).getValue(); }
    public boolean useMathJax() { return ((BooleanOption) options.get("use_mathjax")).getValue(); }
    public boolean calcSplit() { return ((BooleanOption) options.get("calc_split")).getValue(); }
    public boolean displayHiddenSheets() { return ((BooleanOption) options.get("display_hidden_sheets")).getValue(); }
    public boolean displayHiddenRowsCols() { return ((BooleanOption) options.get("display_hidden_rows_cols")).getValue(); }
    public boolean displayFilteredRowsCols() { return ((BooleanOption) options.get("display_filtered_rows_cols")).getValue(); }
    public boolean applyPrintRanges() { return ((BooleanOption) options.get("apply_print_ranges")).getValue(); }
    public boolean useTitleAsHeading() { return ((BooleanOption) options.get("use_title_as_heading")).getValue(); }
    public boolean useSheetNamesAsHeadings() { return ((BooleanOption) options.get("use_sheet_names_as_headings")).getValue(); }
    public boolean saveImagesInSubdir() { return ((BooleanOption) options.get("save_images_in_subdir")).getValue(); }
    public String uplink() { return options.get("uplink").getString(); }
	
    public XhtmlStyleMap getXParStyleMap() { return getStyleMap(xpar); }
    public XhtmlStyleMap getXHeadingStyleMap() { return getStyleMap(xheading); }
    public XhtmlStyleMap getXTextStyleMap() { return getStyleMap(xtext); }
    public XhtmlStyleMap getXFrameStyleMap() { return getStyleMap(xframe); }
    public XhtmlStyleMap getXListStyleMap() { return getStyleMap(xlist); }
    public XhtmlStyleMap getXAttrStyleMap() { return getStyleMap(xattr); }
	
    private XhtmlStyleMap getStyleMap(ComplexOption co) {
    	XhtmlStyleMap map = new XhtmlStyleMap();
    	for (String sName : co.keySet()) {
    		Map<String,String> attr = co.get(sName);
    		String sElement = attr.containsKey("element") ? attr.get("element") : "";
    		String sCss = attr.containsKey("css") ? attr.get("css") : "";
    		String sBlockElement = attr.containsKey("block-element") ? attr.get("block-element") : "";
    		String sBlockCss = attr.containsKey("block-css") ? attr.get("block-css") : "";
    		String sBefore = attr.containsKey("before") ? attr.get("before") : "";
    		String sAfter = attr.containsKey("after") ? attr.get("after") : "";
    		map.put(sName, new XhtmlStyleMapItem(sBlockElement, sBlockCss, sElement, sCss, sBefore, sAfter));
    	}
    	return map;

    }
}
