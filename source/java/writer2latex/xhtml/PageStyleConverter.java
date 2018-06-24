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
 *  Version 2.0 (2018-06-12)
 *
 */

package writer2latex.xhtml;

import java.util.Enumeration;

import writer2latex.office.MasterPage;
import writer2latex.office.OfficeReader;
import writer2latex.office.OfficeStyleFamily;
import writer2latex.office.PageLayout;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;

/**
 * This class converts OpenDocument page styles to CSS2 styles.
 * A page style in a presentation is represented through the master page,
 * which links to a page layout defining the geometry and optionally a drawing
 * page defining the drawing background.
 * 
 * In a presentation document we export the full page style.
 * In a text document we export the writing direction, background color and footnote rule for the first master page only
 */
public class PageStyleConverter extends StyleConverterHelper {
	
	private boolean bHasFootnoteRules = false;

	/** Create a new <code>PageStyleConverter</code>
     *  @param ofr an <code>OfficeReader</code> to read style information from
     *  @param config the configuration to use
     *  @param converter the main <code>Converter</code> class
     */
    public PageStyleConverter(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        super(ofr,config,converter);
        this.bConvertStyles = config.formatting()==XhtmlConfig.CONVERT_ALL || config.formatting()==XhtmlConfig.IGNORE_HARD;
    }
    
    /** Get the text width of the first master page (page width minus left and right margin)
     * 
     *  @return the text width
     */
    public String getTextWidth() {
        MasterPage masterPage = ofr.getFirstMasterPage();
        if (masterPage!=null) {
            PageLayout pageLayout = ofr.getPageLayout(masterPage);
            if (pageLayout!=null) {
                String sWidth = pageLayout.getProperty(XMLString.FO_PAGE_WIDTH);
                if (sWidth!=null) {
                	String sMarginLeft = pageLayout.getProperty(XMLString.FO_MARGIN_LEFT);
                	if (sMarginLeft!=null) {
                		sWidth = Calc.sub(sWidth, sMarginLeft);
                	}
                	String sMarginRight = pageLayout.getProperty(XMLString.FO_MARGIN_RIGHT);
                	if (sMarginRight!=null) {
                		sWidth = Calc.sub(sWidth, sMarginRight);
                	}
                	return sWidth;
                }
            }
        }
        return "17cm"; // Default in Writer, hence usable as a fallback value
    }
    
    /** Apply footnote rule formatting (based on first master page)
     * 
     * @param info then StyleInfo to which style information should be attached
     */
    public void applyFootnoteRuleStyle(StyleInfo info) {
    	bHasFootnoteRules = true;
    	info.sClass="footnoterule";
    }
    
    /** Apply default writing direction (based on first master page)
     * 
     * @param info then StyleInfo to which style information should be attached
     */
    public void applyDefaultWritingDirection(StyleInfo info) {
        MasterPage masterPage = ofr.getFirstMasterPage();
        if (masterPage!=null) {
            PageLayout pageLayout = ofr.getPageLayout(masterPage);
            if (pageLayout!=null) {
                applyDirection(pageLayout,info);
            }
        }    	
    }

    /** Apply a master page style - currently only for presentations
     * 
     * @param sStyleName The name of the master page
     * @param info the StyleInfo to which style information should be attached
     */
    public void applyStyle(String sStyleName, StyleInfo info) {
        MasterPage masterPage = ofr.getMasterPage(sStyleName);
        if (masterPage!=null) {
            String sDisplayName = masterPage.getDisplayName();
            if (ofr.isPresentation()) {
                // Always generates class name
                info.sClass="masterpage"+styleNames.getExportName(sDisplayName);
            }
        }
    }

    /** Convert style information for used styles
     *  @param sIndent a String of spaces to add before each line
     */
    public String getStyleDeclarations(String sIndent) {
        StringBuilder buf = new StringBuilder();

        // This will be master pages for presentations only
        Enumeration<String> names = styleNames.keys();
        while (names.hasMoreElements()) {
            String sDisplayName = names.nextElement();
            MasterPage style = (MasterPage)
                getStyles().getStyleByDisplayName(sDisplayName);
            StyleInfo info = new StyleInfo();
            // First apply page layout (size)
            PageLayout pageLayout = ofr.getPageLayout(style);
            if (pageLayout!=null) {
                applyDirection(pageLayout,info);
                cssPageSize(pageLayout,info.props);
                getFrameSc().cssBackground(StyleWithProperties.PAGE,pageLayout,info.props,true);
            }
            // Next apply drawing-page style (draw background)
            StyleWithProperties drawingPage = ofr.getDrawingPageStyle(style.getProperty(XMLString.DRAW_STYLE_NAME));
            if (drawingPage!=null) {
                cssDrawBackground(drawingPage,info.props,true);
            }
            // Then export the results
            buf.append(sIndent)
               .append(".masterpage").append(styleNames.getExportName(sDisplayName))
               .append(" {").append(info.props.toString()).append("}")
               .append(config.prettyPrint() ? "\n" : " ");
        }
        
        if (ofr.isText()) {
        	// Export page formatting for first master page in text documents
        	MasterPage masterPage = ofr.getFirstMasterPage();
        	if (masterPage!=null) {
        		PageLayout pageLayout = ofr.getPageLayout(masterPage);
        		if (pageLayout!=null) {
        			if (bConvertStyles) {
        				// Background color
        				StyleInfo pageInfo = new StyleInfo();
                        getFrameSc().cssBackground(StyleWithProperties.PAGE,pageLayout,pageInfo.props,true);
                        getFrameSc().cssPadding(StyleWithProperties.PAGE,pageLayout,pageInfo.props,true);
                        getFrameSc().cssBorder(StyleWithProperties.PAGE,pageLayout,pageInfo.props,true);
                        getFrameSc().cssShadow(StyleWithProperties.PAGE,pageLayout,pageInfo.props,true);
        				if (pageInfo.hasAttributes()) {
        					buf.append(sIndent).append("body {").append(pageInfo.props.toString()).append("}")
        					.append(config.prettyPrint() ? "\n" : " ");
        				}
        				
        				// Footnote rule
        				if (bHasFootnoteRules) {
        					StyleInfo ruleInfo = new StyleInfo();
        					cssFootnoteRule(pageLayout,ruleInfo.props);
        					buf.append(sIndent).append("hr.footnoterule {").append(ruleInfo.props.toString()).append("}")
        					.append(config.prettyPrint() ? "\n" : " ");
        				}
        			}
        		}
        	}
        }
        return buf.toString();
    }
	
    /** Get the family of page styles (master pages)
     *  @return the style family
     */
    public OfficeStyleFamily getStyles() {
        return ofr.getMasterPages();
    }

    // Background properties in draw: Color, gradient, hatching or bitmap
    private void cssDrawBackground(StyleWithProperties style, CSVList props, boolean bInherit){
        // Fill color: Same as in css
        String s = style.getProperty(XMLString.DRAW_FILL_COLOR,bInherit);
        if (s!=null) { props.addValue("background-color",s); }
    }

	
    private void cssPageSize(PageLayout style, CSVList props) {
        String sWidth = style.getProperty(XMLString.FO_PAGE_WIDTH);
        if (sWidth!=null) { props.addValue("width",scale(sWidth)); }
        String sHeight = style.getProperty(XMLString.FO_PAGE_HEIGHT);
        if (sHeight!=null) { props.addValue("height",scale(sHeight)); }
    }
    
	// Footnote rule
    private void cssFootnoteRule(PageLayout style, CSVList props) {
    	String sBefore = style.getFootnoteProperty(XMLString.STYLE_DISTANCE_BEFORE_SEP);
    	if (sBefore!=null) { props.addValue("margin-top",scale(sBefore)); }
    	String sAfter = style.getFootnoteProperty(XMLString.STYLE_DISTANCE_AFTER_SEP);
    	if (sAfter!=null) { props.addValue("margin-bottom", scale(sAfter)); }
    	String sHeight = style.getFootnoteProperty(XMLString.STYLE_WIDTH);
    	if (sHeight!=null) { props.addValue("height", scale(sHeight)); }
    	String sWidth = style.getFootnoteProperty(XMLString.STYLE_REL_WIDTH);
    	if (sWidth!=null) { props.addValue("width", sWidth); }
    	
    	String sColor = style.getFootnoteProperty(XMLString.STYLE_COLOR);
    	if (sColor!=null) { // To get the expected result in all browsers we must set both
    		props.addValue("color", sColor);
    		props.addValue("background-color", sColor);
    	}

    	String sAdjustment = style.getFootnoteProperty(XMLString.STYLE_ADJUSTMENT);
    	if ("right".equals(sAdjustment)) {
    		props.addValue("margin-left", "auto");
    		props.addValue("margin-right", "0");
    	}
    	else if ("center".equals(sAdjustment)) {
    		props.addValue("margin-left", "auto");
    		props.addValue("margin-right", "auto");
    	}
    	else { // default left
    		props.addValue("margin-left", "0");
    		props.addValue("margin-right", "auto");
    	}
    }


	
	
}
