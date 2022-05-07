/************************************************************************
 *
 *  HeadingConverter.java
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
 *  Version 2.0 (2022-05-07)
 *
 */

package writer2latex.latex;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.HeadingMap;
import writer2latex.office.ListStyle;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Calc;
import writer2latex.util.Misc;

/** <p>This class converts OpenDocument graphic heading styles to LaTeX. Headings in OpenDocument format is formatted
 * with a combination of a paragraph style and an outline style. Usually all headings of a given level are formatted
 * with the same style, but they may have different styles. In the export we will always export only one style for
 * each heading level.</p>
 *  <p>Formatting in LaTeX is done using the package <code>titlesec.sty</code> (controlled by the option
 *  <code>use_titlesec</code>). Export of the outline numbering scheme is controlled separately by the
 *  option <code>outline_numbering</code>. Unlike most converter helpers, this class only adds content to the preamble.</p>
 */ 
class HeadingStyleConverter extends ConverterHelper {
	
	/** Construct a new <code>HeadingStyleConverter</code>
	 * @param ofr the Office reader from which style information is read
	 * @param config the LaTeX configuration to use
	 * @param palette the converter palette to get access to other converter helpers
	 */
	HeadingStyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
		super(ofr, config, palette);
	}

	@Override
	public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
		if (config.useTitlesec()) {
			pacman.usepackage("explicit", "titlesec");
			convertHeadingStyles(decl);
		}
		
		if (config.outlineNumbering()) {
			convertOutlineNumbering(decl);
		}
	}
	
	// Convert outline numbering
	private void convertOutlineNumbering(LaTeXDocumentPortion ldp) {
		ldp.append("% Outline numbering").nl();
	
		// Get the heading map from the configuration
		HeadingMap hm = config.getHeadingMap();
		int nMaxLevel = Math.min(hm.getMaxLevel(), 10);
		
		// Get the outline numbering from the document
		ListStyle outline = ofr.getOutlineStyle();

        // Collect numbering styles and set the counter secnumdepth
        int nSecnumdepth = nMaxLevel;
        String[] sNumFormat = new String[nMaxLevel+1];
        for (int i=nMaxLevel; i>=1; i--) {
            sNumFormat[i] = ListConverter.numFormat(outline.getLevelProperty(i,XMLString.STYLE_NUM_FORMAT));
            if (sNumFormat[i]==null || "".equals(sNumFormat[i])) {
                nSecnumdepth = i-1;
            }
        }
        ldp.append("\\setcounter{secnumdepth}{"+nSecnumdepth+"}").nl();

        // Convert the individual levels by redefinition of \thesection etc.
        for (int i=1; i<=nSecnumdepth; i++) {
            int nLevels = Misc.getPosInteger(outline.getLevelProperty(i,XMLString.TEXT_DISPLAY_LEVELS),1);
			ldp.append("\\renewcommand\\the").append(hm.getName(i))
			   .append("{");
			for (int j=i-nLevels+1; j<i; j++) {
				ldp.append(sNumFormat[j]).append("{").append(hm.getName(j)).append("}").append(".");
			}
			ldp.append(sNumFormat[i]).append("{").append(hm.getName(i)).append("}")
			   .append("}").nl();
        }	
	}
	
	// Convert heading styles using titlesec.sty
	private void convertHeadingStyles(LaTeXDocumentPortion ldp) {
		ldp.append("% Headings").nl();
		
		// Get the heading map from the configuration
		HeadingMap hm = config.getHeadingMap();
		int nMaxLevel = Math.min(hm.getMaxLevel(), 10);
		
		// Convert the individual levels
		for (int i=0; i<=nMaxLevel; i++) {
			StyleWithProperties style = ofr.getHeadingStyle(i);
			String sSectionName = hm.getName(i);
			if (style!=null && sSectionName!=null) {
				convertTitleformat(style, i, sSectionName, ldp);
				convertTitlespacing(style, sSectionName, ldp);
			}
		}
	}
	
	// Convert a single heading level using \titleformat to define the formatting
	private void convertTitleformat(StyleWithProperties style, int nLevel, String sSectionName, LaTeXDocumentPortion ldp) {
		ldp.append("\\titleformat{\\").append(sSectionName).append("}");
		convertShape(style, ldp);
		convertBeforeContent(style, ldp);
		convertLabel(style, nLevel, sSectionName, ldp);
		ldp.append("{0pt}"); // Distance between label and text, in ODF this distance is part of the label
		convertTitleBody(style, ldp);
		convertAfterContent(style, ldp);
		ldp.nl();
	}
	
	// The space of the heading depends on the indentation of the first line
	private void convertShape(StyleWithProperties style, LaTeXDocumentPortion ldp) {
		String sIndent = style.getParProperty(XMLString.FO_TEXT_INDENT, true);
		if (sIndent!=null && sIndent.startsWith("-")) {
			ldp.append("[hang]"); // negative indent indicates hanging layout
		}
		else {
			ldp.append("[block]");
		}
	}
	
	// The "before" content is vertical material to add before the heading
	// First border and padding, and then paragraph formatting to apply to the heading
	private void convertBeforeContent(StyleWithProperties style, LaTeXDocumentPortion ldp) {
		ldp.append("{");
		
		// Border and padding
		convertBorder(style, true, ldp);
		convertPadding(style, true, ldp);
		
		// Paragraph formatting
		BeforeAfter ba = new BeforeAfter();
        palette.getPageSc().applyPageBreak(style,true,ba);
        applyAlignment(style,ba);
        palette.getCharSc().applyNormalFont(ba);
        palette.getCharSc().applyFullFont(style,true,true,ba,new Context());
        ldp.append(ba.getBefore());
        
        ldp.append("}");
	}
	
	private void applyAlignment(StyleWithProperties style, BeforeAfter ba) {
        String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
        if ("center".equals(sTextAlign)) { ba.add("\\filcenter",""); }
        else if ("end".equals(sTextAlign)) { ba.add("\\filleft",""); }
        else if (!"justify".equals(sTextAlign)) { ba.add("\\filright",""); } // Default is "start"
	}
	
	// The "after" content is (optional) vertical material to add after the heading
	// This amounts to padding and border
	private void convertAfterContent(StyleWithProperties style, LaTeXDocumentPortion ldp) {
		ldp.append("[");
		convertPadding(style, false, ldp);
		convertBorder(style, false, ldp);
		ldp.append("]");
	}
	
	private void convertPadding(StyleWithProperties style, boolean bTop, LaTeXDocumentPortion ldp) {
		String sPadding = style.getAlternativeProperty(StyleWithProperties.PAR, 
			bTop ? XMLString.FO_PADDING_TOP : XMLString.FO_PADDING_BOTTOM, XMLString.FO_PADDING, true);
		if (sPadding!=null && !Calc.isZero(sPadding)) {
			ldp.append("\\vskip").append(sPadding);
		}
	}
	
	private void convertBorder(StyleWithProperties style, boolean bTop, LaTeXDocumentPortion ldp) {
		String sBorder = style.getAlternativeProperty(StyleWithProperties.PAR, 
			bTop ? XMLString.FO_BORDER_TOP : XMLString.FO_BORDER_BOTTOM, XMLString.FO_BORDER, true);
		if (sBorder!=null) {
			String sWidth = null;
			String sColor = null;
			boolean bDouble = false;
			String[] sBorders = sBorder.split(" ");
			for (String sItem : sBorders) {
				if (sItem.length()>0) {
	    			if (Character.isDigit(sItem.charAt(0))) { // If it's a number it must be a unit
	    				sWidth = sItem;
	    			}
	    			else if (sItem.charAt(0)=='#') { // If it starts with # it must be a color
	    				sColor = sItem;
	    			}
	    			else if (sItem.equals("none")) { // No border
	    				return;
	    			}
	    			else if (sItem.equals("double") || sItem.equals("double-thin")) {
	    				bDouble = true;
					}
				}
			}
			BeforeAfter ba = new BeforeAfter();
			ba.add("{", "}");
			palette.getColorCv().applyColor(sColor, true, ba, new Context());
			ldp.append(ba.getBefore());
			// Double border
			if (bDouble) {
				String sDoubleWidth = style.getAlternativeProperty(StyleWithProperties.PAR,
					XMLString.STYLE_BORDER_LINE_WIDTH_TOP, XMLString.STYLE_BORDER_LINE_WIDTH, true);
				if (sDoubleWidth!=null) {
					String[] sValues = sDoubleWidth.split(" ");
					if (sValues.length==3) {
						// The values are width of inner line, separation, width of outer line
						ldp.append("\\titlerule[").append(bTop ? sValues[2] : sValues[0]).append("]")
						   .append("\\vskip").append(sValues[1])
						   .append("\\titlerule[").append(bTop ? sValues[0] : sValues[2]).append("]")
						   .append(ba.getAfter());
						return;
					}
				}
			}
			// Single border
			ldp.append("\\titlerule");
			if (sWidth!=null) {
				ldp.append("[").append(sWidth).append("]");
			}
			ldp.append(ba.getAfter());
		}
	}
	
	// Convert the label, including paragraph formatting
	private void convertLabel(StyleWithProperties style, int nLevel, String sSectionName, LaTeXDocumentPortion ldp) {
		ldp.append("{");
		
		// Add indentation if we are using block layout
		String sTextIndent = style.getParProperty(XMLString.FO_TEXT_INDENT,true);
		if (sTextIndent!=null && !sTextIndent.startsWith("-") && !Calc.isZero(sTextIndent)) {
			ldp.append("\\hspace{").append(sTextIndent).append("}");
		}

		ListStyle outline = ofr.getOutlineStyle();
		
        // Get text style to use for label:
        StyleWithProperties textStyle = ofr.getTextStyle(outline.getLevelProperty(nLevel,XMLString.TEXT_STYLE_NAME));
        BeforeAfter baText = new BeforeAfter();
        palette.getCharSc().applyFullFont(textStyle, false, true, baText, new Context());
        palette.getCharSc().applyFullFontEffects(textStyle, true, baText);

        // Get prefix and suffix text to decorate the label
        String sPrefix = outline.getLevelProperty(nLevel,XMLString.STYLE_NUM_PREFIX);
        String sSuffix = outline.getLevelProperty(nLevel,XMLString.STYLE_NUM_SUFFIX);
        
		ldp.append(baText.getBefore());
        if (outline.isNewType(nLevel)) { // New format since ODF 1.2
	        // Get the label format 	
			String sFollowedBy = outline.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY);
			
			if ("listtab".equals(sFollowedBy)) { // add tab stop after label (simulated with a \makebox)
				String sTabPos = outline.getLevelStyleProperty(nLevel, XMLString.TEXT_LIST_TAB_STOP_POSITION);
				if (sTabPos!=null) {
					ldp.append("\\makebox[").append(sTabPos).append("][l]{");
					convertLabelContent(sPrefix,sSuffix,sSectionName,ldp);
					ldp.append("}");
				}
				else {
					convertLabelContent(sPrefix,sSuffix,sSectionName,ldp);				
				}
			}
			else if ("space".equals(sFollowedBy)) { // add space after label
				convertLabelContent(sPrefix,sSuffix,sSectionName,ldp);
				ldp.append("\\ ");
			}
			else { // Add noting after label
				convertLabelContent(sPrefix,sSuffix,sSectionName,ldp);
			}
        }
        else {
			convertLabelContent(sPrefix,sSuffix,sSectionName,ldp);
        	String sDistance = outline.getLevelStyleProperty(nLevel,XMLString.TEXT_MIN_LABEL_DISTANCE);
        	if (sDistance!=null) {
        		ldp.append("\\hspace{").append(sDistance).append("}");
        	}
        }
		ldp.append(baText.getAfter());
	
		ldp.append("}");
	}
	
	private void convertLabelContent(String sPrefix, String sSuffix, String sSectionName, LaTeXDocumentPortion ldp) {
        ldp.append(sPrefix!=null ? palette.getI18n().convert(sPrefix,false,"en") : "")
           .append("\\the").append(sSectionName)
           .append(sSuffix!=null ? palette.getI18n().convert(sSuffix,false,"en") : "");
	}
	
	// Code applied to the title body only (not the label)
	// Language and font effects must be given as commands, and hence are not applied to the label
	private void convertTitleBody(StyleWithProperties style, LaTeXDocumentPortion ldp) {
		BeforeAfter ba = new BeforeAfter();
        palette.getI18n().applyLanguage(style,false,true,ba);
        palette.getCharSc().applyFullFontEffects(style,true,ba);
        ldp.append("{").append(ba.getBefore()).append("#1").append(ba.getAfter()).append("}");
	}
	
	// \titlespacing* defines the margins (the star kills indentation of first paragraph, like default section commands)
	private void convertTitlespacing(StyleWithProperties style, String sSectionName, LaTeXDocumentPortion ldp) {
		// 1st option is the section name, e.g. \section
		ldp.append("\\titlespacing*{\\").append(sSectionName).append("}");
		// other options are left, top, bottom and right margin (right is optional)
		String sMargin = style.getAbsoluteProperty(StyleWithProperties.PAR,XMLString.FO_MARGIN);
		String sMarginLeft = sMargin!=null ? sMargin :
			style.getAbsoluteProperty(StyleWithProperties.PAR,XMLString.FO_MARGIN_LEFT);
		if (sMarginLeft!=null) {
			String sTextIndent = style.getAbsoluteProperty(StyleWithProperties.PAR, XMLString.FO_TEXT_INDENT);
			if (sTextIndent!=null && sTextIndent.startsWith("-")) {
				// Hanging indentation, add the (negative) indentation to the left margin
				sMarginLeft = Calc.add(sMarginLeft, sTextIndent);
			}
			ldp.append("{").append(sMarginLeft).append("}");
		}
		else {
			ldp.append("{0pt}");
		}
		convertMargin(style, sMargin, XMLString.FO_MARGIN_TOP, ldp);
		convertMargin(style, sMargin, XMLString.FO_MARGIN_BOTTOM, ldp);
		String sMarginRight = sMargin!=null ? sMargin :
			style.getAbsoluteProperty(StyleWithProperties.PAR,XMLString.FO_MARGIN_RIGHT);
		if (sMarginRight!=null && !Calc.isZero(sMarginRight)) {
			ldp.append("[").append(sMarginRight).append("]");
		}
		ldp.nl();
	}
		
	private void convertMargin(StyleWithProperties style, String sMargin, String sXML, LaTeXDocumentPortion ldp) {
		String sThisMargin = sMargin!=null ? sMargin :
			style.getAbsoluteProperty(StyleWithProperties.PAR,sXML);
		if (sThisMargin!=null) {
			// This might be a good place to use some glue
			ldp.append("{")
			   .append(sThisMargin)
			   .append(" plus ").append(Calc.multiply("20%", sThisMargin))
			   .append(" minus ").append(Calc.multiply("10%", sThisMargin))
			   .append("}");
		}
		else {
			ldp.append("{0pt plus 1pt}");
		}
	}
	
}
