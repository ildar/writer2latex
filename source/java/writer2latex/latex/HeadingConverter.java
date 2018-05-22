/************************************************************************
 *
 *  HeadingConverter.java
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
 *  Version 2.0 (2018-05-22)
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.HeadingMap;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/* This class converts OpenDocument headings (<code>text:h</code>) into LaTeX
 */
public class HeadingConverter extends ConverterHelper {
    // Display hidden text?
    private boolean bDisplayHiddenText = false;

    /** Constructs a new <code>HeadingConverter</code>.
     */
    public HeadingConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        this.bDisplayHiddenText = config.displayHiddenText();
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
    	// Nothing to do
    }
	
    /** Process a heading
     *  @param node The text:h element node containing the heading
     *  @param ldp The LaTeXDocumentPortion to add LaTeX code to
     *  @param oc The current context
     */
    public void handleHeading(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	// Get the style
		StyleWithProperties style = ofr.getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME));
		
		// Check for hidden text
        if (!bDisplayHiddenText && style!=null && "none".equals(style.getProperty(XMLString.TEXT_DISPLAY))) {
        	return;
        }

        // Get the heading map and the level
        HeadingMap hm = config.getHeadingMap();
        int nLevel = Misc.getPosInteger(Misc.getAttribute(node, XMLString.TEXT_OUTLINE_LEVEL),1);

        if (nLevel<=hm.getMaxLevel()) {
            // Always push the font used
            palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(style));

            Context ic = (Context) oc.clone();
            ic.setInSection(true);
            // Footnotes with more than one paragraph are not allowed within sections. To be safe, we disallow all footnotes
            ic.setNoFootnotes(true);

            // Apply style (hard page break and hard character formatting)
            BeforeAfter baHardPage = new BeforeAfter();
            BeforeAfter baHardChar = new BeforeAfter();
            applyHardHeadingStyle(nLevel, style, baHardPage, baHardChar, ic);

            // Get plain content for the optional argument
            LaTeXDocumentPortion ldpOpt = new LaTeXDocumentPortion(true);
            palette.getInlineCv().traversePlainInlineText(node,ldpOpt,ic);
            String sOpt = ldpOpt.toString();

            // Get formatted content
            LaTeXDocumentPortion ldpContent = new LaTeXDocumentPortion(true);
            palette.getInlineCv().traverseInlineText(node,ldpContent,ic);
            String sContent = ldpContent.toString();
            
            // Export the heading
            ldp.append(baHardPage.getBefore());
            ldp.append("\\"+hm.getName(nLevel));
            
            if ("true".equals(Misc.getAttribute(node,XMLString.TEXT_IS_LIST_HEADER))) {
            	// Unnumbered heading
            	ldp.append("*");
            }
            else if (!sContent.equals(sOpt)) {
            	// The heading contains e.g. formatting or footnotes, this requires an optional argument
                ldp.append("[").append(sOpt).append("]");
            }
            
            ldp.append("{").append(baHardChar.getBefore()).append(sContent).append(baHardChar.getAfter()).append("}").nl();
            ldp.append(baHardPage.getAfter());
			
            // Include pending index marks, labels, footnotes & floating frames
            palette.getFieldCv().flushReferenceMarks(ldp,oc);
            palette.getIndexCv().flushIndexMarks(ldp,oc);
            palette.getNoteCv().flushFootnotes(ldp,oc);
            palette.getDrawCv().flushFloatingFrames(ldp,ic);

            // Pop the font name
            palette.getI18n().popSpecialTable();
        }
        else { // beyond supported headings - export as ordinary paragraph
            palette.getParCv().handleParagraph(node,ldp,oc,false);
        }
    }

    /** Use a paragraph style on a heading. If hard paragraph formatting
     *  is applied to a heading, page break and font is converted - other
     *  hard formatting is ignored.
     *  This method also collects name of heading style
     *  @param <code>nLevel</code> The level of this heading
     *  @param <code>style</code> the office style to apply
     *  @param <code>baPage</code> a <code>BeforeAfter</code> to put page break code into
     *  @param <code>baText</code> a <code>BeforeAfter</code> to put character formatting code into
     *  @param <code>context</code> the current context. This method will use and update the formatting context  
     */
    private void applyHardHeadingStyle(int nLevel, StyleWithProperties style, BeforeAfter baPage, BeforeAfter baText, Context context) {
        if (style!=null && style.isAutomatic()) {
            palette.getPageSc().applyPageBreak(style,false,baPage);
            palette.getCharSc().applyHardCharFormatting(style,baText);
	        // Update context
	        context.updateFormattingFromStyle(style);
        }
    }


}
