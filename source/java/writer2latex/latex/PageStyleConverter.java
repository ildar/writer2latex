/************************************************************************
 *
 *  PageStyleConverter.java
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
 *  Version 2.0 (2018-07-23)
 *
 */

package writer2latex.latex;

import java.util.Enumeration;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.MasterPage;
import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyle;
import writer2latex.office.PageLayout;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;

/* This class creates LaTeX code from ODF page layouts and master pages. Page layout is exported (if use_geometry is true) 
 * using geometry.sty, and master pages are exported (if use_fancyhdr is true) using fancyhdr.sty.
 * The class sets some global options too, page size (if a standard page size is used), and twoside (if geometry is mirrored
 * or at least one page style has different header/footer content for odd and even pages).
 * Finally the class exports (if footnote_rule is true) the footnote rule.
 */
public class PageStyleConverter extends StyleConverter {
    // Value of attribute text:display of most recent text:chapter field
    // This is used to handle chaptermarks in headings
    private String sChapterField1 = null;
    private String sChapterField2 = null;
	
    // The page layout used for the page geometry
    // (currently, we only support one page geometry per document)
    private PageLayout mainPageLayout;

    /** Constructs a new <code>PageStyleConverter</code>
     * 
     * @param ofr the office reader providing the styles to use
     * @param config the current configuration
     * @param palette the converter palette providing access other converter helpers
     */
    public PageStyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        // Determine the main page layout
        MasterPage firstMasterPage = ofr.getFirstMasterPage();
        if (firstMasterPage!=null) {
            MasterPage nextMasterPage = ofr.getMasterPage(firstMasterPage.getProperty(XMLString.STYLE_NEXT_STYLE_NAME));
            if (nextMasterPage!=null) {
            	// If the first master has a "next" master, use the layout from the next master
                mainPageLayout = ofr.getPageLayout(nextMasterPage);
            }
            else {
            	// Otherwise use the layout from the firstmaster
                mainPageLayout = ofr.getPageLayout(firstMasterPage);
            }
        }
        // Note that the main page layout may still be null
    }
    
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
    	LaTeXDocumentPortion ldp = new LaTeXDocumentPortion(false);
    	
        if (config.useFancyhdr()) {
        	pacman.usepackage("fancyhdr");
        }
        // The first master page must be known
        MasterPage firstMasterPage = ofr.getFirstMasterPage();
        if (firstMasterPage!=null) {
            styleNames.addName(getDisplayName(firstMasterPage.getName()));
        }
        // Convert page layout and master pages
        boolean bTwosideLayout = convertPageGeometry(pacman);
        boolean bTwosideHeaderFooter = convertMasterPages(ldp);
        if (config.useGeometry() && bTwosideHeaderFooter && !bTwosideLayout) {
        	// geometry.sty has a special global option for this case
        	palette.addGlobalOption("asymmetric");
        }
        else if (bTwosideHeaderFooter || bTwosideLayout) {
        	// Other cases are handled by the standard global option twoside
        	palette.addGlobalOption("twoside");
        }
        // Use first master page
        if (firstMasterPage!=null) {
            BeforeAfter ba = new BeforeAfter();
            applyMasterPage(firstMasterPage.getName(),ba);
            ldp.append(ba.getBefore());
        }
        // Convert page color (the context plays no role by now)
        convertPageColor(ldp, new Context());
        // Convert footnote rule
        if (config.footnoteRule()) {
        	convertFootnoteRule(ldp);
        }
        
        if (!ldp.isEmpty()) {
        	decl.append("% Pages").nl().append(ldp);
        }

    }
	
    public void setChapterField1(String s) { sChapterField1 = s; }
	
    public void setChapterField2(String s) { sChapterField2 = s; }
    
    /** Update context information based on the main page layout (columns and background color)
     * 
     * @param context the context that needs to be updated
     */
    public void updateContext(Context context) {
    	if (mainPageLayout!=null) {
    		context.setInMulticols(mainPageLayout.getColCount()>1);
    		// We are only interested in the context, the actual code is thrown away for now
    		convertPageColor(new LaTeXDocumentPortion(false), context);
    	}
    }
	
    /** <p>Apply page break properties from a style.</p>
     *  @param style the style to use
     *  @param bInherit true if inheritance from parent style should be used
     *  @param ba a <code>BeforeAfter</code> to put code into
     */
    public void applyPageBreak(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null && !(style.isAutomatic() && config.ignoreHardPageBreaks())) {
	        // A page break can be a simple page break before or after...
	        String s = style.getProperty(XMLString.FO_BREAK_BEFORE,bInherit);
	        if ("page".equals(s)) { ba.add("\\clearpage",""); }
	        s = style.getProperty(XMLString.FO_BREAK_AFTER,bInherit);
	        if ("page".equals(s)) { ba.add("","\\clearpage"); }
	        // ...or it can be a new master page
	        String sMasterPage = style.getMasterPageName();
	        if (sMasterPage!=null && sMasterPage.length()>0) {
	        	// First the page break
		        ba.add("\\clearpage\n","");
		        // The apply the master page (depending on the use_fancyhdr and page_numbering)
		        applyMasterPage(sMasterPage,ba);
		        // Finally an explicit new page number can be applied
		        if (config.pageNumbering()) {
			        String sPageNumber=style.getProperty(XMLString.STYLE_PAGE_NUMBER);
			        if (sPageNumber!=null && !sPageNumber.equals("auto")) {
			            int nPageNumber = Misc.getPosInteger(sPageNumber,1);
			            ba.add("\\setcounter{page}{"+nPageNumber+"}\n","");
			        }
		        }
	        }
        }
    }
		
    /** <p>Use a Master Page (pagestyle in LaTeX)</p>
     *  @param sName    name of the master page to use
     *  @param ba      the <code>BeforeAfter</code> to add code to.
     */
    void applyMasterPage(String sName, BeforeAfter ba) {
        MasterPage style = ofr.getMasterPage(sName);
        if (style!=null) {
        	if (config.useFancyhdr()) {
	        	if (style.getFooterFirst()!=null || style.getHeaderFirst()!=null) {
	        		// This master page has a special header/footer on the first page.
	        		// With fancyhdr, we have to create an additional page style for this.
		            ba.add("\\pagestyle{"+styleNames.getExportName(getDisplayName(sName))+"}\n"+
		            		"\\thispagestyle{"+styleNames.getExportName(getDisplayName(sName))+"1st}\n","");	        		
	        	}
	        	else {
			        String sNextName = style.getProperty(XMLString.STYLE_NEXT_STYLE_NAME);
			        MasterPage nextStyle = ofr.getMasterPage(sNextName);
			        if (style==nextStyle || nextStyle==null) {
			            ba.add("\\pagestyle{"+styleNames.getExportName(getDisplayName(sName))+"}\n", "");
			        }
			        else {
			            ba.add("\\pagestyle{"+styleNames.getExportName(getDisplayName(sNextName))+"}\n"+
			               "\\thispagestyle{"+styleNames.getExportName(getDisplayName(sName))+"}\n","");
			        }
	        	}
	        }
	        else if (config.pageNumbering()) {
	        	// If we were using fancyhdr.sty the page numbering is included with the style.
	        	// But if not, we have to include it explicitly with the page break

	        	// The format is taken from the next page style (if any)
		        MasterPage nextStyle = ofr.getMasterPage(style.getProperty(XMLString.STYLE_NEXT_STYLE_NAME));
		        if (nextStyle==null) { nextStyle=style; }
	            PageLayout nextPageLayout = ofr.getPageLayout(nextStyle.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME));
                if (nextPageLayout!=null) {
                    String sNumFormat = nextPageLayout.getProperty(XMLString.STYLE_NUM_FORMAT);
                    if (sNumFormat!=null) {
                    	ba.addBefore("\\renewcommand\\thepage{"+ListConverter.numFormat(sNumFormat)+"{page}}\n");
                    }                	
                }
                
                // The first page number is taken directly from this style 
	            PageLayout pageLayout = ofr.getPageLayout(style.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME));
                if (pageLayout!=null) {
                    String sPageNumber = pageLayout.getProperty(XMLString.STYLE_FIRST_PAGE_NUMBER);
                    if (sPageNumber!=null && !sPageNumber.equals("continue")) {
                    	ba.addBefore("\\setcounter{page}{"+Misc.getPosInteger(sPageNumber,0)+"}\n");
                    }
                }	    	
	        }
        }
    }
	
    // Create fancyhdr page styles, return true if any of the page styles has a twosided layout
    private boolean convertMasterPages(LaTeXDocumentPortion ldp) {
    	boolean bTwoside = false;
    	if (config.useFancyhdr()) {
	        Context context = new Context();
	        context.resetFormattingFromStyle(ofr.getDefaultParStyle());
	        context.setInHeaderFooter(true);
				
	        Enumeration<OfficeStyle> styles = ofr.getMasterPages().getStylesEnumeration();
	        while (styles.hasMoreElements()) {
	            MasterPage style = (MasterPage) styles.nextElement();
	            String sName = style.getName();
	            if (styleNames.containsName(getDisplayName(sName))) {
	            	bTwoside|=convertMasterPage(style, false, ldp, context);
	            	if (style.getHeaderFirst()!=null || style.getFooterFirst()!=null) {
		        		// This master page has a special header/footer on the first page.
		        		// With fancyhdr, we have to create an additional page style for this.
	            		convertMasterPage(style, true, ldp, context);
	            	}
	            }
	        }
    	}
    	return bTwoside;
    }
    
    // Return true if the layout is twosided
    private boolean convertMasterPage(MasterPage style, boolean bFirst, LaTeXDocumentPortion ldp, Context context) {
    	boolean bTwoside = false;
        sChapterField1 = null;
        sChapterField2 = null;

        String sPageLayout = style.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME);
        PageLayout pageLayout = ofr.getPageLayout(sPageLayout);

        // Create fancyhdr page style
        ldp.append("\\fancypagestyle{")
           .append(styleNames.getExportName(getDisplayName(style.getName())));
        if (bFirst) { ldp.append("1st"); } // the special first page is named by appending 1st
        ldp.append("}{\\fancyhf{}").nl();
        if (bFirst) {
        	// First page header
	        if (style.getHeaderFirst()!=null) {
		        ldp.append("  \\fancyhead[")
		           .append(getParAlignment(style.getHeaderFirst()))
		           .append("]{");
		        traverseHeaderFooter((Element)style.getHeaderFirst(),ldp,context);
		        ldp.append("}").nl();
        	}
        }
        else {
	        // Header - odd or both
	        ldp.append("  \\fancyhead[")
	           .append(getParAlignment(style.getHeader()))
	           .append(style.getHeaderLeft()!=null ? "O" : "")
	           .append("]{");
	        traverseHeaderFooter((Element)style.getHeader(),ldp,context);
	        ldp.append("}").nl();
	        // Header - even
	        if (style.getHeaderLeft()!=null) {
	        	bTwoside = true;
	            ldp.append("  \\fancyhead[")
	               .append(getParAlignment(style.getHeaderLeft()))
	               .append("E]{");
	            traverseHeaderFooter((Element)style.getHeaderLeft(),ldp,context);
	            ldp.append("}").nl();
	        }
        }
        if (bFirst) {
        	// First page footer
	        if (style.getFooterFirst()!=null) {
		        ldp.append("  \\fancyfoot[")
		           .append(getParAlignment(style.getFooterFirst()))
		           .append("]{");
		        traverseHeaderFooter((Element)style.getFooterFirst(),ldp,context);
		        ldp.append("}").nl();	        	
	        }
        }
        else {
	        // Footer - odd or both
	        ldp.append("  \\fancyfoot[")
	           .append(getParAlignment(style.getFooter()))
	           .append(style.getFooterLeft()!=null ? "O" : "")
	           .append("]{");
	        traverseHeaderFooter((Element)style.getFooter(),ldp,context);
	        ldp.append("}").nl();
	        // Footer - even
	        if (style.getFooterLeft()!=null) {
	        	bTwoside = true;
	        	ldp.append("  \\fancyfoot[")
	               .append(getParAlignment(style.getFooterLeft()))
	               .append("E]{");
	            traverseHeaderFooter((Element)style.getFooterLeft(),ldp,context);
	            ldp.append("}").nl();
	        }
        }
        // Rules are defined in the page layout
        ldp.append("  \\renewcommand\\headrulewidth{")
           .append(getBorderWidth(pageLayout,true))
           .append("}").nl()
           .append("  \\renewcommand\\footrulewidth{")
           .append(getBorderWidth(pageLayout,false))
           .append("}").nl();
		
        // Define sectionmark and subsectionmark
        if (sChapterField1!=null) {
            ldp.append("  \\def\\sectionmark##1{\\markboth{");
            if ("name".equals(sChapterField1)) { ldp.append("##1"); }
            else if ("number".equals(sChapterField1) || "plain-number".equals(sChapterField1)) {
                ldp.append("\\thesection");
            }
            else { ldp.append("\\thesection\\ ##1"); }
            ldp.append("}{}}").nl();
        }
        if (sChapterField2!=null) {
            if (sChapterField1==null) {
                ldp.append("  \\def\\sectionmark##1{\\markboth{}{}}").nl();
            }
            ldp.append("  \\def\\subsectionmark##1{\\markright{");
            if ("name".equals(sChapterField2)) { ldp.append("##1"); }
            else if ("number".equals(sChapterField2) || "plain-number".equals(sChapterField1)) {
                ldp.append("\\thesubsection");
            }
            else { ldp.append("\\thesubsection\\ ##1"); }
            ldp.append("}{}}").nl();
        }
        // Page number is defined in the page layout
        if (config.pageNumbering() && pageLayout!=null) {
            String sNumFormat = pageLayout.getProperty(XMLString.STYLE_NUM_FORMAT);
            if (sNumFormat!=null) {
            ldp.append("  \\renewcommand\\thepage{")
               .append(ListConverter.numFormat(sNumFormat))
               .append("{page}}").nl();
            }
            String sPageNumber = pageLayout.getProperty(XMLString.STYLE_FIRST_PAGE_NUMBER);
            if (sPageNumber!=null && !sPageNumber.equals("continue")) {
            ldp.append("  \\setcounter{page}{")
               .append(Misc.getPosInteger(sPageNumber,0))
               .append("}").nl();
            }
        }	
        
        ldp.append("}").nl();
        return bTwoside;
    }
    
    // Get alignment of first paragraph in node
    private String getParAlignment(Node node) {
        String sAlign = "L";
        if (node!=null) {
            Element par = Misc.getChildByTagName(node,XMLString.TEXT_P);
            if (par!=null) {
                String sStyleName = Misc.getAttribute(par,XMLString.TEXT_STYLE_NAME);
                StyleWithProperties style = ofr.getParStyle(sStyleName);
                if (style!=null) {
                    String s = style.getProperty(XMLString.FO_TEXT_ALIGN);
                    if ("center".equals(s)) { sAlign = "C"; }
                    else if ("end".equals(s)) { sAlign = "R"; }
                }
            }
        }
        return sAlign;
    }
	
    // Get border width from header/footer style
    private String getBorderWidth(PageLayout style, boolean bHeader) {
        if (style!=null) {
	        String sBorder;
	        if (bHeader) {
	            sBorder = style.getHeaderProperty(XMLString.FO_BORDER_BOTTOM);
	            if (sBorder==null) {
	                sBorder = style.getHeaderProperty(XMLString.FO_BORDER);
	            }
	        }
	        else {
	            sBorder = style.getFooterProperty(XMLString.FO_BORDER_TOP);
	            if (sBorder==null) {
	                sBorder = style.getFooterProperty(XMLString.FO_BORDER);
	            }
	        }
	        if (sBorder!=null && !sBorder.equals("none")) {
	            return sBorder.substring(0,sBorder.indexOf(' '));
	        }
        }
        return "0pt";
    }

    private void traverseHeaderFooter(Element node, LaTeXDocumentPortion ldp, Context context) {
        if (node==null) { return; }
        // get first paragraph; all other content is ignored
        Element par = Misc.getChildByTagName(node,XMLString.TEXT_P);
        if (par==null) { return; }
		
        String sStyleName = par.getAttribute(XMLString.TEXT_STYLE_NAME);
        BeforeAfter ba = new BeforeAfter();
        // Temp solution: Ignore hard formatting in header/footer (name clash problem)
        // only in package format. TODO: Find a better solution!
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        if (style!=null && (!ofr.isPackageFormat() || !style.isAutomatic())) {
            palette.getCharSc().applyHardCharFormatting(style,ba);
        }
        else {
        	// At least we can apply automatic color if the page background is dark
        	palette.getColorCv().applyAutomaticColor(ba, true, context);
        }

        if (par.hasChildNodes()) {
            ldp.append(ba.getBefore());
            palette.getInlineCv().traverseInlineText(par,ldp,context);
            ldp.append(ba.getAfter());
        }
        
    }
    
    // Convert page color
    private void convertPageColor(LaTeXDocumentPortion ldp, Context context) {
    	if (config.pageColor() && config.useXcolor() && mainPageLayout!=null) {
    		String sColor = null;
    		// The background color can be a draw attribute or a fo attribute
    		String sFill = mainPageLayout.getProperty(XMLString.DRAW_FILL, true);
    		if (sFill!=null) { // draw:fill takes precedence
    			if (sFill.equals("solid")) { // i.e. color (other values are none, bitmap, gradient and hatch)
    				sColor = mainPageLayout.getProperty(XMLString.DRAW_FILL_COLOR, true);
    			}
    		}
    		else {
    			sColor = mainPageLayout.getProperty(XMLString.FO_BACKGROUND_COLOR,true);
    		}
    		if (sColor!=null) {
    			BeforeAfter ba = new BeforeAfter();
    			palette.getColorCv().applyBgColor("\\pagecolor",sColor, ba, context);
    			ldp.append(ba.getBefore()).nl();
    		}
    	}
    }

    // Return true if the layout is mirrored
    private boolean convertPageGeometry(LaTeXPacman pacman) {
    	boolean bTwoside = false;
        if (config.useGeometry() && mainPageLayout!=null) {
	        // Set global document options
	        if ("mirrored".equals(mainPageLayout.getPageUsage())) {
	            bTwoside = true;
	        }
	        if (mainPageLayout.getColCount()>1) {
	            palette.addGlobalOption("twocolumn");
	        }
	
	        // Collect all page geometry
	        // 1. Page size
	        String sPaperHeight = mainPageLayout.getAbsoluteProperty(XMLString.FO_PAGE_HEIGHT);
	        String sPaperWidth = mainPageLayout.getAbsoluteProperty(XMLString.FO_PAGE_WIDTH);
	        // 2. Margins
	        String sMarginTop = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_TOP);
	        String sMarginBottom = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_BOTTOM);
	        String sMarginLeft = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_LEFT);
	        String sMarginRight = mainPageLayout.getAbsoluteProperty(XMLString.FO_MARGIN_RIGHT);
	        // 3. Header+footer dimensions
	        String sHeadHeight = "0cm";
	        String sHeadSep = "0cm";
	        String sFootHeight = "0cm";
	        String sFootSep = "0cm";
	        boolean bIncludeHead = false;
	        boolean bIncludeFoot = false;
	        // Look through all applied page layouts and use largest heights
	        Enumeration<OfficeStyle> masters = ofr.getMasterPages().getStylesEnumeration();
	        while (masters.hasMoreElements()) {
	            MasterPage master = (MasterPage) masters.nextElement();
	            if (styleNames.containsName(getDisplayName(master.getName()))) {
	                PageLayout layout = ofr.getPageLayout(master.getProperty(XMLString.STYLE_PAGE_LAYOUT_NAME));
	                if (layout!=null) {
	                    if (layout.hasHeaderStyle()) {
	                        String sThisHeadHeight = layout.getHeaderProperty(XMLString.FO_MIN_HEIGHT);
	                        if (sThisHeadHeight!=null && Calc.isLessThan(sHeadHeight,sThisHeadHeight)) {
	                            sHeadHeight = sThisHeadHeight;
	                        }
	                        String sThisHeadSep = layout.getHeaderProperty(XMLString.FO_MARGIN_BOTTOM);
	                        if (sThisHeadSep!=null && Calc.isLessThan(sHeadSep,sThisHeadSep)) {
	                            sHeadSep = sThisHeadSep;
	                        }
	                        bIncludeHead = true;
	                    }
	                    if (layout.hasFooterStyle()) {
	                        String sThisFootHeight = layout.getFooterProperty(XMLString.FO_MIN_HEIGHT);
	                        if (sThisFootHeight!=null && Calc.isLessThan(sFootHeight,sThisFootHeight)) {
	                            sFootHeight = sThisFootHeight;
	                        }
	                        String sThisFootSep = layout.getFooterProperty(XMLString.FO_MARGIN_TOP);
	                        if (sThisFootSep!=null && Calc.isLessThan(sFootSep,sThisFootSep)) {
	                            sFootSep = sThisFootSep;
	                        }
	                        bIncludeFoot = true;
	                    }
	                }
	            }
	        }
	        // Define 12pt as minimum height (the source may specify 0pt..)
	        if (bIncludeHead && Calc.isLessThan(sHeadHeight,"12pt")) {
	            sHeadHeight = "12pt";
	        }
	        if (bIncludeFoot && Calc.isLessThan(sFootHeight,"12pt")) {
	            sFootHeight = "12pt";
	        }
	           
	        String sFootSkip = Calc.add(sFootHeight,sFootSep);
			
            // Set up options for geometry.sty
		    CSVList props = new CSVList(",","=");
		    // Paper size is either set by a global option (for standard sizes) or directly by dimension
            if (!standardPaperSize(sPaperWidth,sPaperHeight)) {
                props.addValue("paperwidth",sPaperWidth);
                props.addValue("paperheight",sPaperHeight);
            }
            // Margin settings are optimized to use as few properties as possible
            if (Calc.isEqual(sMarginTop, sMarginBottom) && Calc.isEqual(sMarginTop, sMarginLeft) && Calc.isEqual(sMarginTop, sMarginRight)) {
            	props.addValue("margin", sMarginTop);
            }
            else {
	            if (Calc.isEqual(sMarginTop, sMarginBottom)) {
	            	props.addValue("vmargin",sMarginTop);
	            }
	            else {
	            	props.addValue("top",sMarginTop);
	            	props.addValue("bottom",sMarginBottom);
	            }
	            if (Calc.isEqual(sMarginLeft, sMarginRight)) {
	            	props.addValue("hmargin",sMarginLeft);
	            }
	            else {
		            // Note the words inner/outer makes more sense for two-sided layout, but there is no semantical difference
		            props.addValue(bTwoside ? "inner" : "left",sMarginLeft);
		            props.addValue(bTwoside ? "outer" : "right",sMarginRight);
	            }
            }
            // Header/footer properties are also optimized to reduce the number of properties
            if (bIncludeHead && bIncludeFoot) {
            	props.addValue("includeheadfoot");
            }
            else if (bIncludeHead) {
            	props.addValue("includehead");
            	props.addValue("nofoot");
            }
            else if (bIncludeFoot) {
            	props.addValue("nohead");
            	props.addValue("includefoot");
            }
            else {
            	props.addValue("noheadfoot");
            }
            if (bIncludeHead) {
                props.addValue("head",sHeadHeight);
                props.addValue("headsep",sHeadSep);
            }
            if (bIncludeFoot) {
                props.addValue("foot",sFootHeight);
                props.addValue("footskip",sFootSkip);
            }
            // Use the package
            pacman.usepackage(props.toString(), "geometry");
        }
        return bTwoside;
    }
    
    private void convertFootnoteRule(LaTeXDocumentPortion ldp) {
        // Footnote rule
        // TODO: Support alignment.
        String sAdjustment = mainPageLayout.getFootnoteProperty(XMLString.STYLE_ADJUSTMENT);
        String sBefore = mainPageLayout.getFootnoteProperty(XMLString.STYLE_DISTANCE_BEFORE_SEP);
        if (sBefore==null) { sBefore = "1mm"; }
        String sAfter = mainPageLayout.getFootnoteProperty(XMLString.STYLE_DISTANCE_AFTER_SEP);
        if (sAfter==null) { sAfter = "1mm"; }
        String sHeight = mainPageLayout.getFootnoteProperty(XMLString.STYLE_WIDTH);
        if (sHeight==null) { sHeight = "0.2mm"; }
        String sWidth = mainPageLayout.getFootnoteProperty(XMLString.STYLE_REL_WIDTH);
        if (sWidth==null) { sWidth = "25%"; }
        sWidth=Float.toString(Calc.getFloat(sWidth.substring(0,sWidth.length()-1),1)/100);
        BeforeAfter baColor = new BeforeAfter();
        String sColor = mainPageLayout.getFootnoteProperty(XMLString.STYLE_COLOR);
        palette.getColorCv().applyColor(sColor,false,baColor,new Context());
		
        String sSkipFootins = Calc.add(sBefore,sHeight);
 
        ldp.append("\\setlength{\\skip\\footins}{").append(sSkipFootins).append("}").nl()
           .append("\\renewcommand\\footnoterule{\\vspace*{-").append(sHeight)
           .append("}");
        if ("right".equals(sAdjustment)) {
            ldp.append("\\setlength\\leftskip{0pt plus 1fil}\\setlength\\rightskip{0pt}");
        }
        else if ("center".equals(sAdjustment)) {
            ldp.append("\\setlength\\leftskip{0pt plus 1fil}\\setlength\\rightskip{0pt plus 1fil}");
        }
        else { // default left
            ldp.append("\\setlength\\leftskip{0pt}\\setlength\\rightskip{0pt plus 1fil}");
        }
        ldp.append("\\noindent")
           .append(baColor.getBefore()).append("\\rule{").append(sWidth)
           .append("\\columnwidth}{").append(sHeight).append("}")
           .append(baColor.getAfter())
           .append("\\vspace*{").append(sAfter).append("}}").nl();
    }
    
    private boolean standardPaperSize(String sWidth, String sHeight) {
        // We recognize all paper sizes known by geometry.sty (only some of them are standard sizes in LO)
    	return standardPaperSize1(sWidth, sHeight, "8.5in", "11in", "letterpaper") ||
    	standardPaperSize1(sWidth, sHeight, "8.5in", "14in", "legalpaper") ||
    	standardPaperSize1(sWidth, sHeight, "7.25in", "10.5in", "executivepaper") ||
    	// ISO A paper
    	standardPaperSize1(sWidth, sHeight, "841mm", "1189mm", "a0paper") ||
    	standardPaperSize1(sWidth, sHeight, "594mm", "841mm", "a1paper") ||
    	standardPaperSize1(sWidth, sHeight, "420mm", "594mm", "a2paper") ||
    	standardPaperSize1(sWidth, sHeight, "297mm", "420mm", "a3paper") ||
    	standardPaperSize1(sWidth, sHeight, "210mm", "297mm", "a4paper") ||
    	standardPaperSize1(sWidth, sHeight, "148mm", "210mm", "a5paper") ||
    	standardPaperSize1(sWidth, sHeight, "105mm", "148mm", "a6paper") ||
    	// ISO B paper
    	standardPaperSize1(sWidth, sHeight, "1000mm", "1414mm", "b0paper") ||
    	standardPaperSize1(sWidth, sHeight, "707mm", "1000mm", "b1paper") ||
    	standardPaperSize1(sWidth, sHeight, "500mm", "707mm", "b2paper") ||
    	standardPaperSize1(sWidth, sHeight, "353mm", "500mm", "b3paper") ||
    	standardPaperSize1(sWidth, sHeight, "250mm", "353mm", "b4paper") ||
    	standardPaperSize1(sWidth, sHeight, "176mm", "250mm", "b5paper") ||
    	standardPaperSize1(sWidth, sHeight, "125mm", "176mm", "b6paper") ||
    	// ISO C paper (envelope size)
    	standardPaperSize1(sWidth, sHeight, "917mm", "1297mm", "c0paper") ||
    	standardPaperSize1(sWidth, sHeight, "648mm", "917mm", "c1paper") ||
    	standardPaperSize1(sWidth, sHeight, "458mm", "648mm", "c2paper") ||
    	standardPaperSize1(sWidth, sHeight, "324mm", "458mm", "c3paper") ||
    	standardPaperSize1(sWidth, sHeight, "229mm", "324mm", "c4paper") ||
    	standardPaperSize1(sWidth, sHeight, "162mm", "229mm", "c5paper") ||
    	standardPaperSize1(sWidth, sHeight, "114mm", "162mm", "c6paper") ||
    	// Japanese B paper
    	standardPaperSize1(sWidth, sHeight, "1030mm", "1456mm", "b0j") ||
    	standardPaperSize1(sWidth, sHeight, "728mm", "1030mm", "b1j") ||
    	standardPaperSize1(sWidth, sHeight, "515mm", "728mm", "b2j") ||
    	standardPaperSize1(sWidth, sHeight, "364mm", "515mm", "b3j") ||
    	standardPaperSize1(sWidth, sHeight, "257mm", "364mm", "b4j") ||
    	standardPaperSize1(sWidth, sHeight, "182mm", "257mm", "b5j") ||
    	standardPaperSize1(sWidth, sHeight, "128mm", "182mm", "b6j") ||
    	// ANSI paper
    	standardPaperSize1(sWidth, sHeight, "8.5in", "14in", "ansiapaper") || // identical to letter
    	standardPaperSize1(sWidth, sHeight, "11in", "17in", "ansibpaper") || // AKA ledger or tabloid
    	standardPaperSize1(sWidth, sHeight, "17in", "22in", "ansicpaper") ||
    	standardPaperSize1(sWidth, sHeight, "22in", "34in", "ansidpaper") ||
    	standardPaperSize1(sWidth, sHeight, "34in", "44in", "ansiepaper") ||
    	// The special "screen" size of geometry.sty
    	standardPaperSize1(sWidth, sHeight, "225mm", "180mm", "screen");
    }
    
    private boolean standardPaperSize1(String sWidth, String sHeight, String sStandardWidth, String sStandardHeight, String sPapersize) {
    	if (compare(sStandardWidth,sWidth,Calc.multiply("0.5%", sStandardWidth))
    			&& compare(sStandardHeight,sHeight,Calc.multiply("0.5%", sStandardHeight))) {
    		palette.addGlobalOption(sPapersize);
    		return true;
    	}
    	else if(compare(sStandardWidth,sHeight,Calc.multiply("0.5%", sStandardWidth))
        		&& compare(sStandardHeight,sWidth,Calc.multiply("0.5%", sStandardHeight))) {
    		palette.addGlobalOption(sPapersize);
    		palette.addGlobalOption("landscape");
    		return true;
    	}
    	return false;
    }
	
    private boolean compare(String sLength1, String sLength2, String sTolerance) {
        return Calc.isLessThan(Calc.abs(Calc.sub(sLength1,sLength2)),sTolerance);
    }

    /* Helper: Get display name, or original name if it doesn't exist */
    private String getDisplayName(String sName) {
        String sDisplayName = ofr.getMasterPages().getDisplayName(sName);
        return sDisplayName!=null ? sDisplayName : sName;
    }

}
