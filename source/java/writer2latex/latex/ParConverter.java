/************************************************************************
 *
 *  ParConverter.java
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
 *  Version 2.0 (2022-08-18)
 *
 */

package writer2latex.latex;

import java.util.Map;

import org.w3c.dom.Element;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.StyleMapItem;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;

/* This class converts OpenDocument paragraphs (<code>text:p</code>) and
 * paragraph styles/formatting into LaTeX.
 * Export of formatting depends on the options useParskip (export global setting of paragraph
 * distance and first line indentation), parAlign (export paragraph alignment) 
 * and indirectly on the option formatting (which controls export of character formatting).
 */
// TODO: Captions and {foot|end}notes should (probably?) also use this class
class ParConverter extends StyleConverter {

	// Do we need to fix redefinition of \\?
    private boolean bNeedArrayBslash = false;
    
    // Configuration options
    private boolean bDisplayHiddenText = false;
    private boolean bParAlign = false;
    
    /** <p>Constructs a new <code>ParConverter</code>.</p>
     */
    public ParConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        this.bDisplayHiddenText = config.displayHiddenText();
        this.bParAlign = config.parAlign();
    }
	
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        if (bNeedArrayBslash) {
            // centering and raggedright redefines \\, fix this
            // Note: aviods nameclash with tabularx (arraybackslash) 
            // TODO: Should perhaps choose to load tabularx instead?
            decl.append("\\makeatletter").nl()
                .append("\\newcommand\\arraybslash{\\let\\\\\\@arraycr}").nl()
                .append("\\makeatother").nl();
        }
        
        if (config.useParskip()) {
        	CSVList options = new CSVList(",","=");
        	String sSkip = ofr.getVerticalMargin();
        	if (sSkip!=null) {
        		options.addValue("skip",sSkip+" plus "+Calc.round(Calc.multiply(0.2F, sSkip))); // Add 20% rubber length
        	}
        	else {
        		options.addValue("skip"); // Use a standard value of .5\baselineskip plus 2pt
        	}
        	String sIndent = ofr.getTextIndent();
    		if ("auto".equals(sIndent)) {
    			options.addValue("indent","1.5em");
    		}
    		else if (sIndent!=null) {
    			options.addValue("indent",sIndent);
    		}
    		else {
    			options.addValue("indent"); // Keep indentation from document class
    		}
    		pacman.usepackage(options.toString(), "parskip");
        }

        super.appendDeclarations(pacman,decl);
    }
	
    /**
     * <p> Process a text:p tag</p>
     *  @param node The text:p element node containing the paragraph
     *  @param ldp The <code>LaTeXDocumentPortion</code> to add LaTeX code to
     *  @param oc The current context
     *  @param bLastInBlock If this is true, the paragraph is the
     *  last one in a block, and we need no trailing blank line (eg. right before
     *  \end{enumerate}).
     */
    public void handleParagraph(Element node, LaTeXDocumentPortion ldp, Context oc, boolean bLastInBlock) {
    	// Check for display equation (except in table cells)
        if ((!oc.isInTable()) && palette.getMathCv().handleDisplayEquation(node,ldp)) {
        	return;
        }
		
        // Get the style for this paragraph
        String sStyleName = node.getAttribute(XMLString.TEXT_STYLE_NAME);
        StyleWithProperties style = ofr.getParStyle(sStyleName);
        String sDisplayName = ofr.getParStyles().getDisplayName(sStyleName);
        
		// Check for hidden text
        if (!bDisplayHiddenText && style!=null && "none".equals(style.getProperty(XMLString.TEXT_DISPLAY))) {
        	return;
        }
		
        // Check for strict handling of styles
        if (config.otherStyles()!=LaTeXConfig.ACCEPT && !config.getParStyleMap().containsKey(sDisplayName)) {
            if (config.otherStyles()==LaTeXConfig.WARNING) {
                System.err.println("Warning: A paragraph with style "+sDisplayName+" was ignored");
            }
            else if (config.otherStyles()==LaTeXConfig.ERROR) {
                ldp.append("% Error in source document: A paragraph with style ")
                   .append(palette.getI18n().convert(sDisplayName,false,oc.getLang()))
                   .append(" was ignored").nl();
            }
            // Ignore this paragraph:
            return;        
        }
		
        // Empty paragraphs are often (mis)used to achieve vertical spacing in WYSIWYG
        // word processors. Hence we translate an empty paragraph to \bigskip.
        // This also solves the problem that LaTeX ignores empty paragraphs, Writer doesn't.
        // In a well-structured document, an empty paragraph is probably a mistake,
        // hence the configuration can specify that it should be ignored.
        // Note: Don't use \bigskip in tables (this can lead to strange results)
        if (OfficeReader.isWhitespaceContent(node)) {
            // Always add page break; other formatting is ignored
            BeforeAfter baPage = new BeforeAfter();
            palette.getPageSc().applyPageBreak(style,true,baPage);
            if (!oc.isInTable()) { ldp.append(baPage.getBefore()); }
            if (!config.ignoreEmptyParagraphs()) {
                if (!oc.isInTable()) {
                    ldp.nl().append("\\bigskip").nl();
                }
                else {
                    ldp.append("~").nl();
                }
                if (!bLastInBlock) { ldp.nl(); }
            }
            if (!oc.isInTable()) { ldp.append(baPage.getAfter()); }
            return;
        }
        
        // Finished treating special cases, now for normal conversion of paragraph
		
        Context ic = (Context) oc.clone();

        // Always push the font used
        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(ofr.getParStyle(sStyleName)));
		
        // Apply the style
        int nBreakAfter;
        BeforeAfter ba = new BeforeAfter();
        if (oc.isInTable()) {
            nBreakAfter = applyCellParStyle(sStyleName,ba,ic,OfficeReader.getCharacterCount(node)==0,bLastInBlock);
        }
        else {
            nBreakAfter = applyParStyle(sStyleName,ba,ic,OfficeReader.getCharacterCount(node)==0);
        }
		
        // Do conversion
        ldp.append(ba.getBefore());
        palette.getInlineCv().traverseInlineText(node,ldp,ic);
        ldp.append(ba.getAfter());
        // Add line break if desired
        if (nBreakAfter!=StyleMapItem.NONE) { ldp.nl(); }
        // Add a blank line except within verbatim and last in a block, and if desired by style map
        if (!bLastInBlock && !ic.isVerbatim() && !ic.isInSimpleTable() && nBreakAfter==StyleMapItem.PAR) { ldp.nl(); }
		
        // Flush any pending index marks, reference marks and floating frames
        palette.getFieldCv().flushReferenceMarks(ldp,oc);
        palette.getIndexCv().flushIndexMarks(ldp,oc);
        palette.getDrawCv().flushFloatingFrames(ldp,oc);

        // pop the font name
        palette.getI18n().popSpecialTable();
    }

    // Paragraphs formatting in table cells need a special treatment for several reasons
    // TODO: This methods would benefit from some refactoring
    private int applyCellParStyle(String sName, BeforeAfter ba, Context context, boolean bNoTextPar, boolean bLastInBlock) {
        context.setVerbatim(false);
        
        int nBreakAfter = bLastInBlock ? StyleMapItem.NONE : StyleMapItem.PAR;
        
        if (context.isInSimpleTable()) {
            if (config.formatting()!=LaTeXConfig.IGNORE_ALL) {
                // only character formatting!
                StyleWithProperties style = ofr.getParStyle(sName);
                if (style!=null) {
                    palette.getI18n().applyLanguage(style,true,true,ba);
                    palette.getCharSc().applyFont(style,true,true,ba,context);
                    if (ba.getBefore().length()>0) { ba.add(" ",""); }
                }
            }
            nBreakAfter = StyleMapItem.NONE;
        }
        else if (config.getParStyleMap().containsKey(ofr.getParStyles().getDisplayName(sName))) {
        	// We have a style map in the configuration
            Map<String,StyleMapItem> sm = config.getParStyleMap();
            String sDisplayName = ofr.getParStyles().getDisplayName(sName);
            String sBefore = sm.get(sDisplayName).getBefore();
            String sAfter = sm.get(sDisplayName).getAfter();
            ba.add(sBefore, sAfter);
            // Add line breaks inside?
            if (sm.get(sDisplayName).getLineBreak()) {
                if (sBefore.length()>0) { ba.add("\n",""); }
                if (sAfter.length()>0 && !"}".equals(sAfter)) { ba.add("","\n"); }
            }
            nBreakAfter = getBreakAfter(sm,sDisplayName);
            if (sm.get(sDisplayName).getVerbatim()) { context.setVerbatim(true); }
        }
        else if (bNoTextPar) {
            // only alignment!
            StyleWithProperties style = ofr.getParStyle(sName);
            if (style!=null) {
                // Apply hard formatting attributes
                // Note: Left justified text is exported as full justified text!
                palette.getPageSc().applyPageBreak(style,false,ba);
                if (bParAlign) {
	                String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
	                if (bLastInBlock && context.isInLastTableColumn()) { // no grouping needed, but need to fix problem with \\
	                    if ("center".equals(sTextAlign)) { ba.add("\\centering\\arraybslash ",""); }
	                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft\\arraybslash ",""); }
	                    bNeedArrayBslash = true;
	                }
	                else if (bLastInBlock) { // no grouping needed
	                    if ("center".equals(sTextAlign)) { ba.add("\\centering ",""); }
	                    else if ("end".equals(sTextAlign)) { ba.add("\\raggedleft ",""); }
	                }
	                else {
	                    if ("center".equals(sTextAlign)) { ba.add("{\\centering ","\\par}"); }
	                    else if ("end".equals(sTextAlign)) { ba.add("{\\raggedleft ","\\par}"); }
	                    nBreakAfter = StyleMapItem.LINE;
	                }
                }
            }
        }
        else {
            // Export character formatting + alignment only
            BeforeAfter baPar = new BeforeAfter();
            BeforeAfter baText = new BeforeAfter();

            // Apply hard formatting attributes
            // Note: Left justified text is exported as full justified text!
            StyleWithProperties style = ofr.getParStyle(sName);
            if (style!=null) {
            	if (bParAlign) {
	                String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
	                if (bLastInBlock && context.isInLastTableColumn()) { // no grouping needed, but need to fix problem with \\
	                    if ("center".equals(sTextAlign)) { baPar.add("\\centering\\arraybslash",""); }
	                    else if ("end".equals(sTextAlign)) { baPar.add("\\raggedleft\\arraybslash",""); }
	                    bNeedArrayBslash = true;
	                }
	                else if (bLastInBlock) { // no \par needed
	                    if ("center".equals(sTextAlign)) { baPar.add("\\centering",""); }
	                    else if ("end".equals(sTextAlign)) { baPar.add("\\raggedleft",""); }
	                }
	                else {
	                    if ("center".equals(sTextAlign)) { baPar.add("\\centering","\\par"); }
	                    else if ("end".equals(sTextAlign)) { baPar.add("\\raggedleft","\\par"); }
	                }
	            }
                palette.getI18n().applyLanguage(style,true,true,baText);
                palette.getCharSc().applyFont(style,true,true,baText,context);
            }

            // Group the contents if this is not the last paragraph in the cell
            boolean bIsGrouped = false;
            if ((!baPar.isEmpty() || !baText.isEmpty()) && !bLastInBlock) {
            	ba.add("{","}");
            	bIsGrouped = true;
            }
            ba.add(baPar);
            // Group the text formatting in any case (supertabular needs this)
            if (!baText.isEmpty() && !bIsGrouped) {
            	ba.add("{", "}");
            }
            ba.add(baText);
            if (ba.getBefore().length()>0) { ba.add(" ",""); }
        } 
		
        // Update context
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style!=null) {
        	context.updateFormattingFromStyle(style);
        }
        return nBreakAfter;
    }
	
    /** <p>Use a paragraph style in LaTeX.</p>
     *  @param <code>sName</code> the name of the text style
     *  @param <code>ba</code> a <code>BeforeAfter</code> to put code into
     *  @param <code>context</code> the current context. This method will use and update the formatting context
     *  @param <code>bNoTextPar</code> true if this paragraph has no text content (hence character formatting is not needed)  
     */
    private int applyParStyle(String sName, BeforeAfter ba, Context context, boolean bNoTextPar) {
        // No style specified?
        if (sName==null) { return StyleMapItem.PAR; }
        
        int nBreakAfter = StyleMapItem.PAR;
        
        if (bNoTextPar) { // Export page break and alignment only
            StyleWithProperties style = ofr.getParStyle(sName);
            palette.getPageSc().applyPageBreak(style,false,ba);
            if (applyAlignment(style, ba)) {
            	nBreakAfter = StyleMapItem.LINE;
            }
        }
        else { // Apply the style
            if (!styleMap.containsKey(sName)) { createParStyle(sName); }
            String sBefore = styleMap.get(sName).getBefore(); 
            String sAfter = styleMap.get(sName).getAfter();
            ba.add(sBefore,sAfter);
            // Add line breaks inside?
            if (styleMap.get(sName).getLineBreak()) {
                if (sBefore.length()>0) { ba.add("\n",""); }
                if (sAfter.length()>0 && !"}".equals(sAfter)) { ba.add("","\n"); }
            }
            nBreakAfter = getBreakAfter(styleMap,sName);
        } 
		
        // Update context
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style!=null) {
        	context.updateFormattingFromStyle(style);
        }
        context.setVerbatim(styleMap.containsKey(sName) && styleMap.get(sName).getVerbatim());
        
        return nBreakAfter;
    }
	
    /** Convert a paragraph style to LaTeX, using paragraph maps from the configuration if available.
     *  @param sName the ODF name of the style
     */
	private void createParStyle(String sName) {
		// A paragraph style should always be created relative to main context
		Context context = (Context) palette.getMainContext().clone();
		String sDisplayName = ofr.getParStyles().getDisplayName(sName);
		if (config.getParStyleMap().containsKey(sDisplayName)) {
			createMappedParStyle(sName, config.getParStyleMap().get(sDisplayName), context);
		}
		else {
			createNormalParStyle(sName, context);
		}
	}
    
    private void createNormalParStyle(String sName, Context context) {
        BeforeAfter ba = new BeforeAfter();
        BeforeAfter baText = new BeforeAfter();
        
        // Apply formatting attributes
    	StyleWithProperties style = ofr.getParStyle(sName);
    	if (style!=null) {
	        palette.getPageSc().applyPageBreak(style,true,ba);
	        applyAlignment(style, baText);
	        palette.getI18n().applyLanguage(style,true,true,baText);
	        palette.getCharSc().applyFont(style,true,true,baText,context);
    	}
    	
        // Assemble the bits. If there is any hard character formatting
        // or alignment we must group the contents.
        if (!baText.isEmpty()) { ba.add("{","}"); }
        ba.add(baText.getBefore(),baText.getAfter());
        styleMap.put(style.getName(),new StyleMapItem(style.getName(),ba.getBefore(),ba.getAfter()));
    }

    private void createMappedParStyle(String sName, StyleMapItem smi, Context context) {
        // Apply hard formatting properties if this is an automatic style
        BeforeAfter ba = new BeforeAfter();
        BeforeAfter baText = new BeforeAfter();
        StyleWithProperties style = ofr.getParStyle(sName);
        if (style!=null && style.isAutomatic()) {
	        palette.getPageSc().applyPageBreak(style,false,ba);
	        if (!smi.getVerbatim()) {
	        	// We cannot use character formatting if the content is treated as verbatim
		        palette.getI18n().applyLanguage(style,true,false,baText);
		        palette.getCharSc().applyFont(style,true,false,baText,context);
	        }
        }
        
    	// Get the content from the style map
    	ba.add(smi.getBefore(), smi.getAfter());
    	
    	if (!baText.isEmpty() && (smi.getBefore().indexOf("{")<0 || smi.getBefore().indexOf("}")<0)) {
    		// The map does not seem to group the contents
        	ba.add("{","}");
        }
    	if (baText.getBefore().length()>0 && smi.getLineBreak()) {
    		// Add an extra line break if we have character formatting
        	ba.addBefore("\n");
        }
        ba.add(baText.getBefore(),baText.getAfter());
        if (baText.getBefore().length()>0 && !smi.getLineBreak()) {
        	// Protect character formatting with space
        	ba.add(" ","");
        }
        
        styleMap.put(style.getName(),new StyleMapItem(sName,ba.getBefore(),ba.getAfter(),smi.getLineBreak(),smi.getBreakAfter(), smi.getVerbatim()));
    }
    
    private boolean applyAlignment(StyleWithProperties style, BeforeAfter ba) {
    	if (bParAlign && style!=null) {
	        String sTextAlign = style.getProperty(XMLString.FO_TEXT_ALIGN,true);
	        if ("center".equals(sTextAlign)) {
	        	ba.add("\\centering","\\par");
	        	return true;
	        }
	        else if ("end".equals(sTextAlign)) {
	        	ba.add("\\raggedleft","\\par");
	        	return true;
	        }
	        // Note: Left justified is exported as fully justified
    	}
    	return false;
    }
    
    private int getBreakAfter(Map<String,StyleMapItem> sm, String sName) {
    	return sm.containsKey(sName) ? sm.get(sName).getBreakAfter() : StyleMapItem.PAR;
    }
}
