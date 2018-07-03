/************************************************************************
 *
 *  NoteConverter.java
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
 *  Version 2.0 (2018-07-02)
 *
 */

package writer2latex.latex;

import java.util.LinkedList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.util.Calc;
import writer2latex.util.Misc;
import writer2latex.util.ExportNameCollection;
import writer2latex.office.OfficeReader;
import writer2latex.office.PropertySet;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

/** This class handles conversion of footnotes and endnotes, including references.
 *  The export depends on the options <code>use_endnotes</code>, <code>notesname</code>,
 *  <code>notes_numbering</code>, <code>use_perpage</code>, <code>footnote_rule</code>
 *  and <code>formatting</code>. Most configuration options are supported, except
 *  continuation notices, which are not supported in LaTeX

 */
class NoteConverter extends ConverterHelper {
	
	private boolean bNeedPerpage = false; // do we need the package perpage.sty?

    private ExportNameCollection notenames = new ExportNameCollection(true);
    private boolean bFootnotesAsEndnotes = false;
    private boolean bContainsEndnotes = false;
    private boolean bContainsFootnotes = false;
    private boolean bContainsFootnotesAsEndnotes = false;
    // Keep track of footnotes (inside minipage etc.), that should be typeset later
    private LinkedList<Element> postponedFootnotes = new LinkedList<Element>();

    NoteConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        PropertySet configuration=ofr.getFootnotesConfiguration();
        if (configuration!=null) {
        	bFootnotesAsEndnotes = "document".equals(configuration.getProperty(XMLString.TEXT_FOOTNOTES_POSITION));
        }
    }

    /** Append declarations needed by the <code>NoteConverter</code> to the preamble.
     * @param pacman the <code>LaTeXPacman</code> to which
     * declarations of packages should be added (<code>\\usepackage</code>).
     * @param decl the <code>LaTeXDocumentPortion</code> to which
     * other declarations should be added.
     */
    void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        if (bContainsEndnotes) { pacman.usepackage("endnotes"); }
        LaTeXDocumentPortion ldp = new LaTeXDocumentPortion(false);
        if (bContainsFootnotes) { 
            convertNotesConfiguration(ofr.getFootnotesConfiguration(),"foot",ldp);
        }
        if (bContainsEndnotes) {
        	// Footnotes configuration takes precedence if footnotes are inserted as endnotes
            convertNotesConfiguration(
            		bContainsFootnotesAsEndnotes ? ofr.getFootnotesConfiguration() : ofr.getEndnotesConfiguration(),"end",ldp);
            if (config.notesname().length()>0) {
            	ldp.append("\\renewcommand\\notesname{").append(config.notesname()).append("}").nl();
            }
        }
        if (!ldp.isEmpty()) {
        	if (bContainsFootnotes && bContainsEndnotes) {
        		decl.append("% Footnotes and endnotes").nl();
        	}
        	else if (bContainsFootnotes) {
        		decl.append("% Footnotes").nl();
        	}
        	else {
        		decl.append("% Endnotes").nl();
        	}
        	decl.append(ldp);
        }
        // We do not know whether to use perpage.sty until now
        if (bNeedPerpage) { pacman.usepackage("perpage"); }
    }
    
    /** <p>Process a footnote or endnote (<code>text:note</code>)
     * @param node The element containing the note
     * @param ldp the <code>LaTeXDocumentPortion</code> to which LaTeX code should be added
     * @param oc the current context
     */
    void handleNote(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();
        ic.setInFootnote(true);

        Element body = Misc.getChildByTagName(node,XMLString.TEXT_NOTE_BODY);
        if (body!=null) {
    	    boolean bEndnote = "endnote".equals(node.getAttribute(XMLString.TEXT_NOTE_CLASS));
        	if (config.useEndnotes() && (bEndnote || bFootnotesAsEndnotes)) {
        		// Treat this note as an endnote
        		handleNote("endnote",body,ldp,oc);
        		bContainsEndnotes = true;
        		if (!bEndnote) { bContainsFootnotesAsEndnotes = true; }
        	}
        	else {
        		if (ic.isNoFootnotes()) {
	        		// Treat this note as a footnote, to be processed when we leave an area with no footnotes
	                ldp.append("\\footnotemark{}");
	                postponedFootnotes.add(body);
	        	}
	        	else {
	        		// Treat this note as a normal footnote
	        		handleNote("footnote",body,ldp,oc);
	        	}
        		bContainsFootnotes = true;
        	}
        }
	    
    }
    
    /** Do we have any pending footnotes, that may be inserted in this context?
     * 
     * @param oc the context to verify against
     * @return true if there are pending footnotes
     */
    boolean hasPendingFootnotes(Context oc) {
    	return !oc.isNoFootnotes() && postponedFootnotes.size()>0;
    }
	
    /** Flush the queue of postponed footnotes */
    void flushFootnotes(LaTeXDocumentPortion ldp, Context oc) {
        // We may still be in a context with no footnotes
        if (oc.isNoFootnotes()) { return; }
        // Type out all postponed footnotes:
        Context ic = (Context) oc.clone();
        ic.setInFootnote(true);
        int n = postponedFootnotes.size();
        if (n>0) {
	        if (n==1) {
	        	handleNote("footnotetext",postponedFootnotes.get(0),ldp,ic);
	        }
	        else if (n>1) {
	            // Several footnotes; have to adjust the footnote counter
	            ldp.append("\\addtocounter{footnote}{-"+(n-1)+"}");
	            for (int i=0; i<n; i++) {
	                if (i>0) { ldp.append("\\stepcounter{footnote}"); }
	            	handleNote("footnotetext",postponedFootnotes.get(i),ldp,ic);
	            }
	        }
            ldp.nl();
            postponedFootnotes.clear();
        }
    }
	
    /** Insert the endnotes into the documents.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which the endnotes should be added.
     */
    void insertEndnotes(LaTeXDocumentPortion ldp) {
        if (bContainsEndnotes) {
        	// Start on a new page
        	ldp.append("\\clearpage").nl();
        	// Apply master page as defined in the configuration
        	// Note that the footnotes configuration takes precedence if footnotes are placed as endnotes
	    	PropertySet endnotesConfig = bContainsFootnotesAsEndnotes ? ofr.getFootnotesConfiguration() : ofr.getEndnotesConfiguration();
	    	if (config!=null) {
	    		String sMasterPage = endnotesConfig.getProperty(XMLString.TEXT_MASTER_PAGE_NAME); 
	    		if (sMasterPage!=null) {
	    			BeforeAfter ba = new BeforeAfter();
	    			palette.getPageSc().applyMasterPage(sMasterPage, ba);
	    			ldp.append(ba.getBefore());
	    		}
	    	}
	    	// Insert the actual endnotes
            ldp.append("\\theendnotes").nl();
        }
    }
	
    /** Process a note reference (<code>text:note-ref</code>)
     * @param node The element containing the note reference
     * @param ldp the <code>LaTeXDocumentPortion</code> to which LaTeX code should be added
     * @param oc the current context
     */
    void handleNoteRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{note:"+notenames.getExportName(sName)+"}");
        }
        else if ("text".equals(sFormat) && sName!=null) {
            ldp.append("\\ref{note:"+notenames.getExportName(sName)+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    } 

    private void handleNote(String sNoteType, Element body, LaTeXDocumentPortion ldp, Context oc) {
        ldp.append("\\").append(sNoteType).append("{");
        String sId = Misc.getAttribute(body.getParentNode(),XMLString.TEXT_ID);
        if (sId != null && ofr.hasNoteRefTo(sId)) { 
       		ldp.append("\\label{note:"+notenames.getExportName(sId)+"}");
        }
        traverseNoteBody(body,ldp,oc);
        ldp.append("}");    	
    }
	
    /* Process the contents of a footnote or endnote
     */
    private void traverseNoteBody (Element node, LaTeXDocumentPortion ldp, Context oc) {
    	Node child = node.getFirstChild();
    	boolean bAfterParagraph = false;
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                Element elm = (Element)child;
                String nodeName = elm.getTagName();

                palette.getInfo().addDebugInfo(elm,ldp);
                
                // Headings inside footnotes are considered a mistake and exported as ordinary paragraphs
                if (nodeName.equals(XMLString.TEXT_H) || nodeName.equals(XMLString.TEXT_P)) {
                	StyleWithProperties style = ofr.getParStyle(node.getAttribute(XMLString.TEXT_STYLE_NAME));
                	oc.resetFormattingFromStyle(style);
                	if (bAfterParagraph) { ldp.append("\\par "); }
                    palette.getInlineCv().traverseInlineText(elm,ldp,oc);
                    bAfterParagraph = true;
                }					
                else if (nodeName.equals(XMLString.TEXT_LIST)) {
                	// The \par before the list is important if we export full formatting
                	if (bAfterParagraph) { ldp.append("\\par "); }
                    palette.getListCv().handleList(elm,ldp,oc);
                    bAfterParagraph = true;
                }
            }
            child = child.getNextSibling();
        }
    }
	
    /* Convert {foot|end}notes configuration.
     */
    private void convertNotesConfiguration(PropertySet notes, String sType, LaTeXDocumentPortion ldp) {
    	if (notes!=null) {
            if (config.notesNumbering()) {
            	convertNotesNumbering(notes, sType, ldp);
            }
        	// Formatting of the note citation and note text is still controlled by the formatting option
            if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            	convertNotesFormatting(notes, sType, ldp);
            }
    	}
    }
    
    private void convertNotesNumbering(PropertySet notes, String sType, LaTeXDocumentPortion ldp) {
		// The numbering style is controlled by \the{foot|end}note
		String sFormat = notes.getProperty(XMLString.STYLE_NUM_FORMAT);
		if (sFormat!=null) {
		    ldp.append("\\renewcommand\\the").append(sType).append("note{")
		    .append(ListConverter.numFormat(sFormat))
		    .append("{").append(sType).append("note}}").nl();
		}
		
		if (sType.equals("foot")) {
			String sStartAt = notes.getProperty(XMLString.TEXT_START_NUMBERING_AT);
			if ("chapter".equals(sStartAt)) {
				// Number footnotes by sections
				ldp.append("\\makeatletter").nl()
				.append("\\@addtoreset{").append(sType).append("note}{section}").nl()
				.append("\\makeatother").nl();    	
			}
			else if ("page".equals(sStartAt)) {
				// Number footnotes by pages
				bNeedPerpage = true;
				ldp.append("\\MakePerPage{").append(sType).append("note}").nl();
			}
		}
		
		// Set start value offset (default 0)
		int nStartValue = Misc.getPosInteger(notes.getProperty(XMLString.TEXT_START_VALUE),0);
		if (nStartValue!=0) {
			ldp.append("\\setcounter{").append(sType).append("note}{"+nStartValue+"}").nl();
		}
    }

    /* Note: All {foot|end}notes are formatted with the default style for {foot|end}footnotes.
     * (This doesn't conform with the file format specification, but in LaTeX
     * all {foot|end}notes are usually formatted in a fixed style.)

     */
    private void convertNotesFormatting(PropertySet notes, String sType, LaTeXDocumentPortion ldp) {
        ldp.append("\\makeatletter").nl();
        String sTypeShort = sType.equals("foot") ? "fn" : "en";
        // The formatting of the {foot|end}note citation is controlled by \@make{fn|en}mark
        String sCitBodyStyle = notes.getProperty(XMLString.TEXT_CITATION_BODY_STYLE_NAME);
        if (sCitBodyStyle!=null && ofr.getTextStyle(sCitBodyStyle)!=null) {
            BeforeAfter baText = new BeforeAfter();
            palette.getCharSc().applyTextStyle(sCitBodyStyle,baText,new Context());
            ldp.append("\\renewcommand\\@make").append(sTypeShort).append("mark{\\mbox{")
               .append(baText.getBefore())
               .append("\\@the").append(sTypeShort).append("mark")
               .append(baText.getAfter())
               .append("}}").nl();
        }
	
        // The layout and formatting of the footnote is controlled by \@makefntext
        // The documentation for endnotes.sty wrongly claims the existence for a similar \@makeentext
        // Currently endnotes are ignored
        // TODO: Can we use \enoteformat and \enotesize as defined by endnotes.sty?
        if (sType.equals("foot")) {
	        String sCitStyle = notes.getProperty(XMLString.TEXT_CITATION_STYLE_NAME);
	        String sStyleName = notes.getProperty(XMLString.TEXT_DEFAULT_STYLE_NAME);
	        if (sStyleName!=null) {
	            BeforeAfter baText = new BeforeAfter();
	            palette.getCharSc().applyTextStyle(sCitStyle,baText,new Context());
	            StyleWithProperties style = ofr.getParStyle(sStyleName);
	            if (style!=null) {
	            	// If the paragraph uses hanging indentation, the footnote citation is but in a box of this width
	            	String sTextIndent = style.getAbsoluteParProperty(XMLString.FO_TEXT_INDENT);
	            	if (Calc.isLessThan(sTextIndent, "0cm")) {
	            		baText.enclose("\\makebox["+Calc.multiply("-100%", sTextIndent)+"][l]{", "}");
	            	}
	                BeforeAfter baPar = new BeforeAfter();
	                palette.getCharSc().applyHardCharFormatting(style,baPar);
	                ldp.append("\\renewcommand\\@make").append(sTypeShort)
	                   .append("text[1]{\\noindent")
	                   .append(baPar.getBefore())
	                   .append(baText.getBefore())
	                   .append("\\@the").append(sTypeShort).append("mark ")
	                   .append(baText.getAfter())
	                   .append("#1")
	                   .append(baPar.getAfter());
	                ldp.append("}").nl();
	            }	 
	        }    
        }
    }
		 
}