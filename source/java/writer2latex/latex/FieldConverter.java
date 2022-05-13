/************************************************************************
 *
 *  FieldConverter.java
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
 *  Version 2.0 (2022-05-13)
 *
 */

package writer2latex.latex;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.latex.util.Context; 
import writer2latex.latex.util.HeadingMap;
import writer2latex.office.ListStyle;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;
import writer2latex.util.SimpleInputBuffer;

/**
 *  This class handles text fields and links in the document.
 *  Packages: lastpage, hyperref, titleref (all optional)
 *  TODO: Need proper treatment of "caption" and "text" for sequence
 *  references not to figures and tables (should be fairly rare, though)

 */
public class FieldConverter extends ConverterHelper {
	
	// Identify Writer2LaTeX extended citations
	private static final String WRITER2LATEX_CITE = "Writer2LaTeX_cite";
	
	// Identify Zotero items
	private static final String ZOTERO_ITEM = "ZOTERO_ITEM";
	// Identify JabRef items
	private static final String JABREF_ITEM = "JR_cite";	
	
    // Links & references
    private ExportNameCollection targets = new ExportNameCollection(true);
    private ExportNameCollection refnames = new ExportNameCollection(true);
    private ExportNameCollection bookmarknames = new ExportNameCollection(true);
    private ExportNameCollection seqnames = new ExportNameCollection(true);
    private ExportNameCollection seqrefnames = new ExportNameCollection(true);
    
    // Options for hyperref
    private CSVList hyperrefColor = new CSVList(",","=");
	
    // sequence declarations (maps name->text:sequence-decl element)
    private Hashtable<String, Node> seqDecl = new Hashtable<String, Node>();
    // first usage of sequence (maps name->text:sequence element)
    private Hashtable<String, Element> seqFirst = new Hashtable<String, Element>();
	
    private Vector<Element> postponedReferenceMarks = new Vector<Element>();
    private Vector<Element> postponedBookmarks = new Vector<Element>();

    private boolean bUseHyperref = false;
    private boolean bUsesPageCount = false;
    private boolean bUsesTitleref = false;
    private boolean bConvertZotero = false;
    private boolean bConvertJabRef = false;
    private boolean bIncludeOriginalCitations = false;
	
    public FieldConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        // hyperref.sty is not compatible with titleref.sty:
        bUseHyperref = config.useHyperref() && !config.useTitleref();
        bConvertZotero = config.useBiblatex() && config.zoteroBibtexFiles().length()>0;
        bConvertJabRef = config.useBiblatex() && config.jabrefBibtexFiles().length()>0;
        bIncludeOriginalCitations = config.includeOriginalCitations();
    }
	
    /** <p>Append declarations needed by the <code>FieldConverter</code> to
     * the preamble.</p>
     * @param pacman the <code>LaTeXPacman</code> to which
     * declarations of packages should be added (<code>\\usepackage</code>).
     * @param decl the <code>LaTeXDocumentPortion</code> to which
     * other declarations should be added.
     */
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        // use lastpage.sty
        if (bUsesPageCount) {
        	pacman.usepackage("lastpage");
        }
		
        // use titleref.sty
        if (bUsesTitleref) {
        	pacman.usepackage("titleref");
        } 

        // use hyperref.sty
        if (bUseHyperref){
        	CSVList options = new CSVList(",","=");
        	pacman.usepackage("hyperref");
        	// Only dvips needs an explicit backend, others are either autodetected or are fine with hypertex
            if (config.backend()==LaTeXConfig.DVIPS) options.addValue("dvips");
            // Color links only if we are using color
            if (config.useXcolor()) {
            	options.addValue("colorlinks", "true");
            	if (!hyperrefColor.isEmpty()) {
            		// The color may have been collected during conversion
            		options.addValues(hyperrefColor);
            	}
            	else {
            		// Otherwise blue is always available and is a traditional link color
            		options.addValue("allcolors","blue");
            	}
            }
            // Add PDF meta data if we are using an appropriate backend
            if (config.backend()==LaTeXConfig.PDFTEX || config.backend()==LaTeXConfig.XETEX) {
                createPdfMeta("pdftitle",palette.getMetaData().getTitle(),options);
                if (config.metadata()) {
                    createPdfMeta("pdfauthor",palette.getMetaData().getCreator(),options);
                    createPdfMeta("pdfsubject",palette.getMetaData().getSubject(),options);
                    createPdfMeta("pdfkeywords",palette.getMetaData().getKeywords(),options);
                }
            }
            if (!options.isEmpty()) {
            	pacman.append("\\hypersetup{").append(options.toString()).append("}").nl();
            }
        }	
        		
        // Export sequence declarations
        // The number format is fetched from the first occurence of the
        // sequence in the text, while the outline level and the separation
        // character are fetched from the declaration
        Enumeration<String> names = seqFirst.keys();
        while (names.hasMoreElements()) {
            // Get first text:sequence element
            String sName = names.nextElement();
            Element first = seqFirst.get(sName);
            // Collect data
            String sNumFormat = Misc.getAttribute(first,XMLString.STYLE_NUM_FORMAT);
            if (sNumFormat==null) { sNumFormat="1"; }
            int nLevel = 0;
            String sSepChar = ".";
            if (seqDecl.containsKey(sName)) {
                Element sdecl = (Element) seqDecl.get(sName);
                nLevel = Misc.getPosInteger(sdecl.getAttribute(XMLString.TEXT_DISPLAY_OUTLINE_LEVEL),0);
                if (sdecl.hasAttribute(XMLString.TEXT_SEPARATION_CHARACTER)) {
                    sSepChar = palette.getI18n().convert(
                        sdecl.getAttribute(XMLString.TEXT_SEPARATION_CHARACTER),
                        false,palette.getMainContext().getLang());
                }
            }
            // Create counter
            decl.append("\\newcounter{")
                .append(seqnames.getExportName(sName))
                .append("}");
            String sPrefix = "";
            if (nLevel>0) {
                HeadingMap hm = config.getHeadingMap();
                int nUsedLevel = nLevel<=hm.getMaxLevel() ? nLevel : hm.getMaxLevel();
                if (nUsedLevel>0) {
                    decl.append("[").append(hm.getName(nUsedLevel)).append("]");
                    sPrefix = "\\the"+hm.getName(nUsedLevel)+sSepChar;
                }
            }
            decl.nl()
                .append("\\renewcommand\\the")
                .append(seqnames.getExportName(sName))
                .append("{").append(sPrefix)
                .append(ListConverter.numFormat(sNumFormat))
                .append("{").append(seqnames.getExportName(sName))
                .append("}}").nl();
        }
    }
	
    /** <p>Process sequence declarations</p>
     *  @param node the text:sequence-decls node
     */
    public void handleSequenceDecls(Element node) {
        Node child = node.getFirstChild();
        while (child!=null) {
            if (Misc.isElement(child,XMLString.TEXT_SEQUENCE_DECL)) {
                // Don't process the declaration, but store a reference
                seqDecl.put(((Element)child).getAttribute(XMLString.TEXT_NAME),child);
            }
            child = child.getNextSibling();
        }
    }
	
    /** <p>Process a sequence field (text:sequence tag)</p>
     * @param node The element containing the sequence field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleSequence(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sName = Misc.getAttribute(node,XMLString.TEXT_NAME);
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        String sFormula = Misc.getAttribute(node,XMLString.TEXT_FORMULA);
        if (sFormula==null) {
            // If there's no formula, we must use the content as formula
            // The parser below requires a namespace, so we add that..
            sFormula = "ooow:"+Misc.getPCDATA(node);
        }
        if (sName!=null) {
            if (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName)) {
                // Export \label only, assuming the number is generated by \caption
                if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
                    ldp.append("\\label{seq:")
                       .append(seqrefnames.getExportName(sRefName))
                       .append("}");
                }
            }
            else {
                // General purpose sequence -> export as counter
                if (!seqFirst.containsKey(sName)) {
                    // Save first occurence -> used to determine number format
                    seqFirst.put(sName,node);
                }
                if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
                    // Export as {\refstepcounter{name}\thename\label{refname}}
                    ldp.append("{").append(changeCounter(sName,sFormula,true))
                       .append("\\the").append(seqnames.getExportName(sName))
                       .append("\\label{seq:")
                       .append(seqrefnames.getExportName(sRefName))
                       .append("}}");
                }
                else {
                    // Export as \stepcounter{name}{\thename}
                    ldp.append(changeCounter(sName,sFormula,false))
                       .append("{\\the")
                       .append(seqnames.getExportName(sName))
                       .append("}");
                }
            }
        }
    }
	
    /** <p>Create label for a sequence field (text:sequence tag)</p>
     * @param node The element containing the sequence field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void handleSequenceLabel(Element node, LaTeXDocumentPortion ldp) {
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        if (sRefName!=null && ofr.hasSequenceRefTo(sRefName)) {
            ldp.append("\\label{seq:")
               .append(seqrefnames.getExportName(sRefName))
               .append("}");
        }
    }

    // According to the spec for OpenDocument, the formula is application
    // specific, prefixed with a namespace. OOo uses the namespace ooow, and
    // we accept the formulas ooow:<number>, ooow:<name>, ooow:<name>+<number>
    // and ooow:<name>-<number>
    // Note: In OOo a counter is a 16 bit unsigned integer, whereas a (La)TeX
    // counter can be negative - thus there will be a slight deviation in the
    // (rare) case of a negative number
    private String changeCounter(String sName, String sFormula, boolean bRef) {
        if (sFormula!=null) { 
            sFormula = sFormula.trim();
            if (sFormula.startsWith("ooow:")) {
                SimpleInputBuffer input = new SimpleInputBuffer(sFormula.substring(5));
                if (input.peekChar()>='0' && input.peekChar()<='9') {
                    // Value is <number>
                    String sNumber = input.getInteger();
                    if (input.atEnd()) {
                        return setCounter(sName, Misc.getPosInteger(sNumber,0), bRef);
                    }
                }
                else if (input.peekChar()=='-') {
                    // Value is a negative <number>
                    input.getChar();
                    if (input.peekChar()>='0' && input.peekChar()<='9') {
                        String sNumber = input.getInteger();
                        if (input.atEnd()) {
                            return setCounter(sName, -Misc.getPosInteger(sNumber,0), bRef);
                        }
                    }
                }
                else {
                    // Value starts with <name>
                    String sToken = input.getIdentifier();
                    if (sToken.equals(sName)) {
                        input.skipSpaces();
                        if (input.peekChar()=='+') {
                            // Value is <name>+<number>
                            input.getChar();
                            input.skipSpaces();
                            String sNumber = input.getInteger();
                            if (input.atEnd()) {
                                return addtoCounter(sName, Misc.getPosInteger(sNumber,0), bRef);
                            }
                        }
                        else if (input.peekChar()=='-') {
                            // Value is <name>-<number>
                            input.getChar();
                            input.skipSpaces();
                            String sNumber = input.getInteger();
                            if (input.atEnd()) {
                                return addtoCounter(sName, -Misc.getPosInteger(sNumber,0), bRef);
                            }
                        }
                        else if (input.atEnd()) {
                            // Value is <name>
                            return addtoCounter(sName, 0, bRef);
                        }
                    }
                }
            }
        }
        // No formula, or a formula we don't understand -> use default behavior
        return stepCounter(sName, bRef);
    }
	
    private String stepCounter(String sName, boolean bRef) {
        if (bRef) {
            return "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else {
            return "\\stepcounter{" + seqnames.getExportName(sName) + "}";
        }
    }
	
    private String addtoCounter(String sName, int nValue, boolean bRef) {
        if (nValue==1) {
            return stepCounter(sName, bRef);
        }
        else if (bRef) {
            return "\\addtocounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue-1) + "}"
                 + "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else if (nValue!=0) {
            return "\\addtocounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue) + "}";
        }
        else {
            return "";
        }
    }
	
    private String setCounter(String sName, int nValue, boolean bRef) {
        if (bRef) {
            return "\\setcounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue-1) + "}"
                 + "\\refstepcounter{" + seqnames.getExportName(sName) + "}";
        }
        else {
            return "\\setcounter{" + seqnames.getExportName(sName) + "}"
                 + "{" + Integer.toString(nValue) + "}";
        }
    }
	
    /** <p>Process a sequence reference (text:sequence-ref tag)</p>
     * @param node The element containing the sequence reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleSequenceRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sRefName = Misc.getAttribute(node,XMLString.TEXT_REF_NAME);
        String sFormat = Misc.getAttribute(node,XMLString.TEXT_REFERENCE_FORMAT);
        String sName = ofr.getSequenceFromRef(sRefName);
        if (sRefName!=null) {
            if (sFormat==null || "page".equals(sFormat)) {
                ldp.append("\\pageref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("value".equals(sFormat)) {
                ldp.append("\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("category-and-value".equals(sFormat)) {
                // Export as Name~\\ref{refname}
                if (sName!=null) {
                    if (ofr.isFigureSequenceName(sName)) {
                        ldp.append("\\figurename~");
                    }
                    else if (ofr.isTableSequenceName(sName)) {
                        ldp.append("\\tablename~");
                    }
                    else {
                        ldp.append(sName).append("~");
                    }
                }
                ldp.append("\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
            }
            else if ("caption".equals(sFormat) && config.useTitleref() &&
                    (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName))) {
                ldp.append("\\titleref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
                bUsesTitleref = true;
            }
            else if ("text".equals(sFormat) && config.useTitleref() &&
                    (ofr.isFigureSequenceName(sName) || ofr.isTableSequenceName(sName))) {
                // This is a combination of "category-and-value" and "caption"
                // Export as \\figurename~\ref{refname}:~\titleref{refname}
                if (ofr.isFigureSequenceName(sName)) {
                    ldp.append("\\figurename");
                }
                else if (ofr.isTableSequenceName(sName)) {
                    ldp.append("\\tablename");
                }
                ldp.append("~\\ref{seq:")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}:~\\titleref{")
                   .append(seqrefnames.getExportName(sRefName))
                   .append("}");
                bUsesTitleref = true;
            }
            else { // use current value
                palette.getInlineCv().traversePCDATA(node,ldp,oc);
            }
        }
    }
    
    // Try to handle this reference name as a Writer2LaTeX extended citation, return true on success
    private boolean handleWriter2LaTeXReferenceName(String sName, LaTeXDocumentPortion ldp, Context oc) {
    	if (sName.startsWith(WRITER2LATEX_CITE)) {
    		int nObjectStart = sName.indexOf('{');
    		int nObjectEnd = sName.lastIndexOf('}');
    		if (nObjectStart>-1 && nObjectEnd>-1 && nObjectStart<nObjectEnd) {
    			String sJsonObject = sName.substring(nObjectStart, nObjectEnd+1);
    			JSONObject citation = null;
    			try {
    				citation = new JSONObject(sJsonObject);
    			} catch (JSONException e) {
    				return false;
    			}
    			// Successfully parsed the reference, now generate the code
    			// (we don't expect any errors and ignore them, if they happen anyway)
    			String sKey = getJSONString(citation,"key");
    			String sType = getJSONString(citation,"type");
    			String sPrefix = getJSONString(citation,"prefix");
    			String sSuffix = getJSONString(citation,"suffix");
    			oc.setInRefMarkCiteText(true);
    			oc.setRefMarkData(sKey, sType, sPrefix, sSuffix);
    			return true;
    		}
    	}
    	return false;
    }
    
    // Try to handle this reference name as a Zotero reference, return true on success
    private boolean handleZoteroReferenceName(String sName, LaTeXDocumentPortion ldp, Context oc) {
    	// First parse the reference name:
    	// A Zotero reference name has the form ZOTERO_ITEM <json object> <identifier> with a single space separating the items
    	// The identifier is a unique identifier for the reference and is not used here
    	if (sName.startsWith(ZOTERO_ITEM)) {
    		int nObjectStart = sName.indexOf('{');
    		int nObjectEnd = sName.lastIndexOf('}');
    		if (nObjectStart>-1 && nObjectEnd>-1 && nObjectStart<nObjectEnd) {
    			String sJsonObject = sName.substring(nObjectStart, nObjectEnd+1);
    			JSONObject jo = null;
    			try {
    				jo = new JSONObject(sJsonObject);
    			} catch (JSONException e) {
    				return false;
    			}
    			// Successfully parsed the reference, now generate the code
    			// (we don't expect any errors and ignore them, if they happen anyway)

    			// The object has four keys: citationID, properties, citationItems and schema, of which we only need citationItems
    			JSONArray citationItemsArray = null;
    			try { // The value is an array of objects, one for each source in this citation
    				citationItemsArray = jo.getJSONArray("citationItems");
    			}
    			catch (JSONException e) {	
    			}

    			if (citationItemsArray!=null) {
    				int nCitationCount = citationItemsArray.length();
    				
					// TODO: Combine citations if nCitationCount>1
					for (int nIndex=0; nIndex<nCitationCount; nIndex++) {
						/* Each citation is represented as an object, this is a sample:
					  	{	"id":1,
							"uris":["http://zotero.org/users/local/hwPOD4lw/items/8ASX6FUP"],
							"itemData":
							{	"id":1,
								"type":"book",
								"title":"The elements",
								"author":[{"family":"Euclid","given":"John"}],
								"citation-key":"euklidElements"
							},
							"locator":"7",
							"label":"page",
							"prefix":"see",
							"suffix":"!"
						}
						*/
						JSONObject citationItems = null;
						JSONObject itemData = null;
						try {
							citationItems = citationItemsArray.getJSONObject(nIndex);
							itemData = citationItems.getJSONObject("itemData");
						}
						catch (JSONException e) {
						}

						if (citationItems!=null && itemData!=null) {
							if (nIndex>0) {
								ldp.append("; "); // Separate multiple citations in this reference
							}
							String sCitationKey = getJSONString(itemData,"citation-key");
							String sPrefix = getJSONString(citationItems,"prefix");
							String sSuffix = getJSONString(citationItems,"suffix");
							String sLocator = getJSONString(citationItems,"locator"); // e.g. a page number
							String sLabel = getJSONString(citationItems,"label"); // e.g. book, verse, page (default is page)
							boolean bSuppressAuthor = getJSONBoolean(citationItems,"suppressAuthor");

							// Adjust locator type (empty locator type means "page")
							// TODO: Handle other locator types (localize and abbreviate): Currently the internal name (e.g. book) is used.
							if (sLocator.length()>0 && (sLabel.length()==0 || sLabel.equals("page"))) {
								// A locator of the form <number><other characters><number> is interpreted as several pages
								if (Pattern.compile("[0-9]+[^0-9]+[0-9]+").matcher(sLocator).find()) {
									sLabel = "pp.";
								} 
								else {
									sLabel = "p.";
								}
							}

							// Insert command (Zotero always add parentheses if not in footnotes)
							if (oc.isInFootnote()) {
								if (bSuppressAuthor) { ldp.append("\\cite*"); }
								else { ldp.append("\\cite"); }								
							}
							else {
								if (bSuppressAuthor) { ldp.append("\\autocite*"); }
								else { ldp.append("\\autocite"); }
							}

							if (sPrefix.length()>0) {
								ldp.append("[").append(palette.getI18n().convert(sPrefix,true,oc.getLang())).append("]");
							}

							if (sPrefix.length()>0 || sSuffix.length()>0 || sLabel.length()>0 || sLocator.length()>0) {
								// Note that we need to include an empty suffix if there's a prefix!
								ldp.append("[")
								   .append(palette.getI18n().convert(sLabel,true,oc.getLang()));
								if (sLabel.length()>0 && sLocator.length()>0) {
									ldp.append("~");
								}
								ldp.append(palette.getI18n().convert(sLocator,true,oc.getLang()))
								   .append(palette.getI18n().convert(sSuffix,true,oc.getLang()))
								   .append("]");
							}

							ldp.append("{").append(sCitationKey).append("}");
						}
					}

    				oc.setInRefMarkCiteText(true);
    				
    				return true;
    			}
    		}
    	}
    	return false;
    }
    
    // Try to handle this reference name as a JabRef reference, return true on success
    private boolean handleJabRefReferenceName(String sName, LaTeXDocumentPortion ldp, Context oc) {
    	// First parse the reference name:
    	// A JabRef reference name has the form JR_cite<m>_<n>_<identifiers> where
    	//   m is a sequence number to ensure unique citations (may be empty)
    	//   n=1 for (Author date) and n=2 for Author (date) citations
    	//   identifiers is a comma separated list of BibTeX keys
    	// TODO: Update to BibLaTeX
    	if (sName.startsWith(JABREF_ITEM)) {
    		String sRemains = sName.substring(JABREF_ITEM.length());
    		int nUnderscore = sRemains.indexOf('_');
    		if (nUnderscore>-1) {
    			sRemains = sRemains.substring(nUnderscore+1);
    			if (sRemains.length()>2) {
    				ldp.append("\\cite").append("{").append(sRemains.substring(2)).append("}");
    			}
    		}
			oc.setInRefMarkCiteText(true);			
    		return true;
    	}
    	return false;
    }
    
    private String getJSONString(JSONObject obj, String sKey) {
    	try {
    		return obj.getString(sKey);
    	} catch (JSONException e) {
    		return ""; // Fail silently, we don't expect any errors
    	}
    }
    
    private boolean getJSONBoolean(JSONObject obj, String sKey) {
    	try {
    		return obj.getBoolean(sKey);
    	} catch (JSONException e) {
    		return false; // Fail silently, we don't expect any errors
    	}
    }
    private String shortenRefname(String s) {
    	// For Zotero items, use the trailing unique identifier
    	if (s.startsWith(ZOTERO_ITEM)) {
    		int nLast = s.lastIndexOf(' ');
    		if (nLast>0) {
    			return s.substring(nLast+1);
    		}
    	}
		return s;
    }

    /** <p>Process a reference mark end (text:reference-mark-end tag)</p>
     * @param node The element containing the reference mark 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleReferenceMarkEnd(Element node, LaTeXDocumentPortion ldp, Context oc) {
    	// Nothing to do, except to mark that this ends any Zotero/JabRef/Writer2LaTeX citation
    	oc.setInRefMarkCiteText(false);
    	if (bIncludeOriginalCitations) { // Protect space after comment
    		ldp.append("{}");
    	}
    }

    /** <p>Process a reference mark (text:reference-mark or text:reference-mark-start tag)</p>
     * @param node The element containing the reference mark 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleReferenceMark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            String sName = node.getAttribute(XMLString.TEXT_NAME);
            // Zotero, JabRef and Writer2LaTeX (mis)uses reference marks to store citations, so check this first
            if (sName!=null && (!handleWriter2LaTeXReferenceName(sName, ldp, oc))
            				&& (!bConvertZotero || !handleZoteroReferenceName(sName, ldp, oc))
            				&& (!bConvertJabRef || !handleJabRefReferenceName(sName, ldp, oc))) {
            	// Plain reference mark
            	// Note: Always include \label here, even when it's not used
            	ldp.append("\\label{ref:"+refnames.getExportName(shortenRefname(sName))+"}");
            }
        }
        else {
            // Reference marks should not appear within \section or \caption
            postponedReferenceMarks.add(node);
        }
    }
	
    /** <p>Process a reference (text:reference-ref tag)</p>
     * @param node The element containing the reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleReferenceRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{ref:"+refnames.getExportName(shortenRefname(sName))+"}");
        }
        else if ("chapter".equals(sFormat) && ofr.referenceMarkInHeading(sName)) {
            // This is safe if the reference mark is contained in a heading
            ldp.append("\\ref{ref:"+refnames.getExportName(shortenRefname(sName))+"}");
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    } 
	
    /** <p>Process a bookmark (text:bookmark tag)</p>
     * <p>A bookmark may be the target for either a hyperlink or a reference,
     * so this will generate a <code>\\hyperref</code> and/or a <code>\\label</code></p>
     * @param node The element containing the bookmark 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleBookmark(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            String sName = node.getAttribute(XMLString.TEXT_NAME);
            if (sName!=null) {
                // A bookmark may be used as a target for a hyperlink as well as
                // for a reference. We export whatever is actually used:
                addTarget(node,"",ldp);
                if (ofr.hasBookmarkRefTo(sName)) {
                    ldp.append("\\label{bkm:"+bookmarknames.getExportName(sName)+"}");
                }
            }
        }
        else {
            // Bookmarks should not appear within \section or \caption
            postponedBookmarks.add(node);
        }
    }
	
    /** <p>Process a bookmark reference (text:bookmark-ref tag).</p>
     * @param node The element containing the bookmark reference 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleBookmarkRef(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sFormat = node.getAttribute(XMLString.TEXT_REFERENCE_FORMAT);
        String sName = node.getAttribute(XMLString.TEXT_REF_NAME);
        if (("page".equals(sFormat) || "".equals(sFormat)) && sName!=null) {
            ldp.append("\\pageref{bkm:"+bookmarknames.getExportName(sName)+"}");
        }
        else if ("chapter".equals(sFormat) && ofr.bookmarkInHeading(sName)) {
            // This is safe if the bookmark is contained in a heading
            ldp.append("\\ref{bkm:"+bookmarknames.getExportName(sName)+"}");
        }
        else if (("number".equals(sFormat) || "number-no-superior".equals(sFormat) || "number-all-superior".equals(sFormat)) &&
        		(ofr.bookmarkInHeading(sName) || ofr.bookmarkInList(sName))) {
        	ListStyle style=null;
        	int nLevel = 0;
        	String sPrefix=null;
        	String sSuffix=null;
        	// Only convert the prefix and suffix if it is converted at the reference target
        	if (ofr.bookmarkInHeading(sName)) {
        		if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
        			style = ofr.getOutlineStyle();
        		}
        		nLevel = ofr.getBookmarkHeadingLevel(sName);
        	}
        	else {
        		if (config.formatting()>=LaTeXConfig.CONVERT_BASIC) {
        			style = ofr.getListStyle(ofr.getBookmarkListStyle(sName));
        		}
        		nLevel = ofr.getBookmarkListLevel(sName);
        	}
        	if (style!=null) {
            	sPrefix = style.getLevelProperty(nLevel, XMLString.STYLE_NUM_PREFIX);
            	sSuffix = style.getLevelProperty(nLevel, XMLString.STYLE_NUM_SUFFIX);
        	}
        	if (sPrefix!=null) ldp.append(palette.getI18n().convert(sPrefix,false,oc.getLang()));
        	ldp.append("\\ref{bkm:").append(bookmarknames.getExportName(sName)).append("}");
        	if (sSuffix!=null) ldp.append(palette.getI18n().convert(sSuffix,false,oc.getLang()));
        }
        else { // use current value
            palette.getInlineCv().traversePCDATA(node,ldp,oc);
        }
    }
    
    /** Do we have any pending reference marks or bookmarks, that may be inserted in this context?
     * 
     * @param oc the context to verify against
     * @return true if there are pending marks
     */
    public boolean hasPendingReferenceMarks(Context oc) {
    	return !oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim() &&
    			postponedReferenceMarks.size()+postponedBookmarks.size()>0;
    }
	
    /** <p>Process pending reference marks and bookmarks (which may have been
     * postponed within sections, captions or verbatim text.</p>
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void flushReferenceMarks(LaTeXDocumentPortion ldp, Context oc) {
        // We may still be in a context with no reference marks
        if (!oc.isInSection() && !oc.isInCaption() && !oc.isVerbatim()) {
            // Type out all postponed reference marks
            int n = postponedReferenceMarks.size();
            for (int i=0; i<n; i++) {
                handleReferenceMark(postponedReferenceMarks.get(i),ldp,oc);
            }
            postponedReferenceMarks.clear();
            // Type out all postponed bookmarks
            n = postponedBookmarks.size();
            for (int i=0; i<n; i++) {
                handleBookmark(postponedBookmarks.get(i),ldp,oc);
            }
            postponedBookmarks.clear();
        }
    }
	
    /** <p>Process a hyperlink (text:a tag)</p>
     * @param node The element containing the hyperlink 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleAnchor(Element node, LaTeXDocumentPortion ldp, Context oc) {
        String sHref = node.getAttribute(XMLString.XLINK_HREF);
        if (sHref!=null) {
        	if (hyperrefColor.isEmpty()) {
        		// We collect the color now to ensure the color name is defined before we insert xcolor declarations
        		String sLinkStyleName = Misc.getAttribute(node, XMLString.TEXT_STYLE_NAME);
            	StyleWithProperties style = ofr.getTextStyle(sLinkStyleName);
            	if (style!=null) {
            		palette.getColorCv().applyNamedColor(style.getTextProperty(XMLString.FO_COLOR, true),"allcolors",hyperrefColor);
            		
            	}
        	}

            if (sHref.startsWith("#")) {
                // TODO: hyperlinks to headings (?) and objects
                if (bUseHyperref) {
                    ldp.append("\\hyperlink{")
                       .append(targets.getExportName(Misc.urlDecode(sHref.substring(1))))
                       .append("}{");
                    // ignore text style (let hyperref.sty handle the decoration):
                    palette.getInlineCv().traverseInlineText(node,ldp,oc);
                    ldp.append("}");
                }
                else { // user don't want to include hyperlinks
                    palette.getInlineCv().handleTextSpan(node,ldp,oc);
                }
            }
            else {
			    if (bUseHyperref) {
                    if (OfficeReader.getTextContent(node).trim().equals(sHref)) {
                        // The link text equals the url
                        ldp.append("\\url{")
                           .append(escapeHref(sHref,oc.isInFootnote()))
                           .append("}");
                    }
                    else {
                        ldp.append("\\href{")
                           .append(escapeHref(sHref,oc.isInFootnote()))
                           .append("}{");
                        // ignore text style (let hyperref.sty handle the decoration):
                        palette.getInlineCv().traverseInlineText(node,ldp,oc);
                        ldp.append("}");
                    }
                }
                else { // user don't want to include hyperlinks
                    palette.getInlineCv().handleTextSpan(node,ldp,oc);
                }
            }
        }
        else {
            palette.getInlineCv().handleTextSpan(node,ldp,oc);
        }
    }
	
    /** <p>Add a <code>\\hypertarget</code></p>
     * @param node The element containing the name of the target
     * @param sSuffix A suffix to be added to the target,
     * e.g. "|table" for a reference to a table.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void addTarget(Element node, String sSuffix, LaTeXDocumentPortion ldp) {
        // TODO: Remove this and use addTarget by name only
        String sName = node.getAttribute(XMLString.TEXT_NAME);
        if (sName == null) { sName = node.getAttribute(XMLString.TABLE_NAME); }
        if (sName == null || !bUseHyperref) { return; }
        if (!ofr.hasLinkTo(sName+sSuffix)) { return; }
        ldp.append("\\hypertarget{")
           .append(targets.getExportName(sName+sSuffix))
           .append("}{}");
    }
    
    /** <p>Add a <code>\\hypertarget</code></p>
     * @param sName The name of the target
     * @param sSuffix A suffix to be added to the target,
     * e.g. "|table" for a reference to a table.
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     */
    public void addTarget(String sName, String sSuffix, LaTeXDocumentPortion ldp) {
        if (sName!=null && bUseHyperref && ofr.hasLinkTo(sName+sSuffix)) {
            ldp.append("\\hypertarget{")
               .append(targets.getExportName(sName+sSuffix))
               .append("}{}");
        }
    }

    /** <p>Process a page number field (text:page-number tag)</p>
     * @param node The element containing the page number field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handlePageNumber(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // TODO: Obey attributes!
        ldp.append("\\thepage{}");
    }
    
    /** <p>Process a page count field (text:page-count tag)</p>
     * @param node The element containing the page count field 
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handlePageCount(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // TODO: Obey attributes!
        // Note: Actually LastPage refers to the page number of the last page, not the number of pages
        if (config.useLastpage()) {
            bUsesPageCount = true;
            ldp.append("\\pageref{LastPage}");
        }
        else {
            ldp.append("?");
        }
    }

    // Helpers:
	
    private void createPdfMeta(String sName, String sValue, CSVList options) {
        if (sValue!=null && !sValue.isEmpty()) {
	        // Replace commas with semicolons (the keyval package doesn't like commas):
	        sValue = sValue.replace(',', ';');
	        // Meta data is assumed to be in the default language:
	        options.addValue(sName,palette.getI18n().convert(sValue,false,palette.getMainContext().getLang()));
        }
    }

    // For the argument to a href, we have to escape or encode certain characters
    private String escapeHref(String s, boolean bInFootnote) {
        StringBuilder buf = new StringBuilder();
        for (int i=0; i<s.length(); i++) {
            if (bInFootnote && s.charAt(i)=='#') { buf.append("\\#"); }
            else if (bInFootnote && s.charAt(i)=='%') { buf.append("\\%"); }
            // The following should not occur in an URL (see RFC1738), but just to be sure we encode them
            else if (s.charAt(i)=='\\') { buf.append("\\%5C"); }
            else if (s.charAt(i)=='{') { buf.append("\\%7B"); }
            else if (s.charAt(i)=='}') { buf.append("\\%7D"); }
            // hyperref.sty deals safely with other characters
            else { buf.append(s.charAt(i)); }
        }
        return buf.toString();
    }
 
}