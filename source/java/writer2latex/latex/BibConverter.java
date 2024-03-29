/************************************************************************
 *
 *  BibConverter.java
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
 *  Version 2.0 (2022-05-22)
 *
 */

package writer2latex.latex;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.w3c.dom.Element;
import writer2latex.base.BibliographyGenerator;
import writer2latex.bibtex.BibTeXDocument;

import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;

import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;

/** This class handles bibliographic citations and the bibliography. The result depends on these
 *  configuration options:
 *  <li><code>use_index</code>: If false, the bibliography will be omitted</li>
 *  <li><code>use_bibtex</code> true and <code>external_bibtex_files</code>
 *  empty: The citations will be exported to a BibTeX file, which will be used
 *  for the bibliography</li>
 *  <li><code>use_bibtex</code> true and <code>external_bibtex_files</code>
 *  non-empty: The citations will be not be exported to a BibTeX file, the
 *  files referred to by the option will be used instead</li>
 *  <li><code>use_bibtex</code> false: The bibliography will be exported as
 *  a thebibliography environment
 *  <li><code>bibtex_style</code> If BibTeX is used, this style will be applied
 *  </ul>
 *  The citations will always be exported as \autocite commands
 */
class BibConverter extends ConverterHelper {

    private BibTeXDocument bibDoc = null;
    
    private Set<String> validCommands = null;
    
    /** Construct a new BibConverter.
     * 
     * @param config the configuration to use 
     * @param palette the ConverterPalette to use
     */
    BibConverter(OfficeReader ofr,LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        
        // We need to create a BibTeX document except if we are using external BibTeX files
        if (!(config.useBiblatex() && config.externalBibtexFiles().length()>0)) {
        	bibDoc = new BibTeXDocument(palette.getOutFileName(),false,ofr);
        }
        
        // TODO: Repeated from BibTeXDialog; what is the obvious place to put the definitions?
    	String[] sCitationTypes = { "autocite", "textcite", "citeauthor", "citeauthor*", "citetitle", "citetitle*",
    			"citeyear", "citedate", "citeurl", "nocite" };
    	validCommands = new HashSet<>(Arrays.asList(sCitationTypes));

    }

    /** Export the bibliography directly as a thebibliography environment (as an alternative to using BibTeX) 
     */
    private class ThebibliographyGenerator extends BibliographyGenerator {
    	// The bibliography is the be inserted in this LaTeX document portion with that context
    	private LaTeXDocumentPortion ldp;
    	private Context context;
    	
    	// The current bibliography item is to formatted with this before/after pair with that context
    	private BeforeAfter itemBa = null;
    	private Context itemContext = null;

    	ThebibliographyGenerator(OfficeReader ofr) {
    		super(ofr,true);
    	}
    	
    	void handleBibliography(Element bibliography, LaTeXDocumentPortion ldp, Context context) {
    		this.ldp = ldp;
    		this.context = context;
    		
    		String sWidestLabel = "";
    		Collection<String> labels = getLabels();
    		for (String sLabel : labels) {
    			if (sLabel.length()>=sWidestLabel.length()) {
    				sWidestLabel = sLabel;
    			}
    		}
    		
        	ldp.append("\\begin{thebibliography}{").append(sWidestLabel).append("}\n");
    		generateBibliography(bibliography);
    		endBibliographyItem();
        	ldp.append("\\end{thebibliography}\n");
    	}
    	
    	@Override protected void insertBibliographyItem(String sStyleName, String sKey) {
    		endBibliographyItem();
    		
    		itemBa = new BeforeAfter();
    		itemContext = (Context) context.clone();

    		// Apply paragraph style (character formatting only)
            StyleWithProperties style = ofr.getParStyle(sStyleName);
            if (style!=null) {
                palette.getI18n().applyLanguage(style,true,true,itemBa);
                palette.getCharSc().applyFont(style,true,true,itemBa,itemContext);
                if (itemBa.getBefore().length()>0) {
                	itemBa.add(" ","");
                    itemBa.enclose("{", "}");
               	}
            }
            
            // Convert item
            ldp.append(itemBa.getBefore());
	        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(style));
	        
    		ldp.append("\\bibitem");
    		if (!isNumberedEntries()) {
    			ldp.append("[").append(bibDoc.getExportName(sKey)).append("]");
    		}
    		ldp.append("{").append(bibDoc.getExportName(sKey)).append("} ");
    	}
    	
    	private void endBibliographyItem() {
    		if (itemBa!=null) {
    	        palette.getI18n().popSpecialTable();
        		ldp.append(itemBa.getAfter()).append("\n");    		    			
        		itemBa = null;
    		}
    	}
    	
    	@Override protected void insertBibliographyItemElement(String sStyleName, String sText) {
    		BeforeAfter ba = new BeforeAfter();
    		Context elementContext = (Context) itemContext.clone();
    		
        	// Apply character style
            StyleWithProperties style = ofr.getTextStyle(sStyleName);
           	palette.getCharSc().applyTextStyle(sStyleName,ba,elementContext);
            
           	// Convert text
    		ldp.append(ba.getBefore());
	        palette.getI18n().pushSpecialTable(palette.getCharSc().getFontName(style));
    		ldp.append(palette.getI18n().convert(sText, false, elementContext.getLang()));
            palette.getI18n().popSpecialTable();
    		ldp.append(ba.getAfter());    		
    	}
    }

    /** Append declarations needed by the <code>BibConverter</code> to the preamble.
     * 
     * @param pack the LaTeXPacman to which declarations of packages (\\usepackage) should be added.
     * @param decl the LaTeXDocumentPortion to which other declarations should be added.
     */
    public void appendDeclarations(LaTeXPacman pack, LaTeXDocumentPortion decl) {
    	if (config.useBiblatex()) {
	    	CSVList options = new CSVList(",","=");
	    	if (config.biblatexOptions().length()>0) {
	    		options.addValue(config.biblatexOptions());
	    	}
	        // We may need to use a different encoding for the BibTeX files
	        if (config.externalBibtexFiles().length()>0) {
	        	int nBibTeXEncoding = config.bibtexEncoding();
	        	if (nBibTeXEncoding>-1 && nBibTeXEncoding!=config.inputencoding()) {
	           		options.addValue("bibencoding", ClassicI18n.writeInputenc(nBibTeXEncoding));
	        	}
	        }
	        options.addValue("backend", "biber");
	
	    	pack.usepackage(options.toString(), "biblatex");
	    	BibTeXDocument doc = getBibTeXDocument();
	    	if (doc!=null) {
	            decl.append("\\addbibresource{").append(bibDoc.getName()).append(".bib}").nl();
	
	    	}
	    	Set<String> bibNames = new HashSet<>();
	    	addBibresources(config.zoteroBibtexFiles(), bibNames, decl);
	    	addBibresources(config.jabrefBibtexFiles(), bibNames, decl);
    	}
    }
    
    private void addBibresources(String sFiles, Set<String> bibNames, LaTeXDocumentPortion ldp) {
    	if (sFiles.length()>0) {
	    	String[] sItems = sFiles.split(",");
	    	for (String s : sItems) {
	    		String s1 = s.trim();
	    		if (!bibNames.contains(s1)) {
	    			ldp.append("\\addbibresource{").append(s1).append(".bib}").nl();
	    			bibNames.add(s1);
	    		}
	    	}
    	}
    }

    /** Process a bibliography
     * 
     * @param node A text:bibliography element
     * @param ldp the LaTeXDocumentPortion to which LaTeX code should be added
     * @param oc the current context
     */
    void handleBibliography(Element node, LaTeXDocumentPortion ldp, Context oc) {
        if (!config.noIndex()) {        	
            Element source = Misc.getChildByTagName(node,XMLString.TEXT_BIBLIOGRAPHY_SOURCE);
	        if (config.useBiblatex()) { // Export using BibLaTeX
	        	ldp.append("\\printbibliography");
	            if (source!=null) {
	    	        Element title = Misc.getChildByTagName(source,XMLString.TEXT_INDEX_TITLE_TEMPLATE);
	    	        if (title!=null && config.convertIndexNames()) {
	    	            ldp.append("[title={"); // Actually {} is only needed if the title contains , or =
	    	            palette.getInlineCv().traversePCDATA(title,ldp,oc); 
	    	            ldp.append("}]");
	    	        }
	            }
	        	ldp.nl();
	        }
	        else { // Export as thebibliography environment
	            if (source!=null) {
	            	// Unfortunately the index name depends on the documentclass
	            	// For now we only handle the standard classes article, report and book
	            	if ("article".equals(config.documentclass())) {
	            		palette.getIndexCv().convertIndexName(source, "\\refname", ldp, oc);
	            	}
	            	else if ("book".equals(config.documentclass()) || "report".equals(config.documentclass())) {
	            		palette.getIndexCv().convertIndexName(source, "\\bibname", ldp, oc);
	            	}
	            }
	        	ThebibliographyGenerator bibCv = new ThebibliographyGenerator(ofr);
	        	bibCv.handleBibliography(source, ldp, oc);
	        }
        }
    }
	
    /** Process a Bibliography Mark
     * @param marks list of text:bibliography-mark elements
     * @param ldp the LaTeXDocumentPortion to which LaTeX code should be added
     * @param oc the current context
     */
    void handleBibliographyMark(List<Element> marks, LaTeXDocumentPortion ldp, Context oc) {
    	CSVList identifiers = new CSVList(",");
    	for (Element mark : marks) {
    		String sIdentifier = mark.getAttribute(XMLString.TEXT_IDENTIFIER);
    		if (sIdentifier.length()>0) {
    			if (config.externalBibtexFiles().length()>0) { // External BibTeX files; use original identifier
    				identifiers.addValue(sIdentifier);
    			} else { // Converting fields to BibTeX; strip identifier of illegal characters
    				identifiers.addValue(bibDoc.getExportName(sIdentifier));
    			}
    		}
    	}
        if (!identifiers.isEmpty()) {
        	if (oc.getType().length()>0) { // Writer2LaTeX extended citation
        		if (validCommands.contains(oc.getType())) {
        			ldp.append("\\").append(oc.getType());
        		} else { // In case someone has created a fake w2l cite entry...
        			ldp.append("\\autocite");
        		}
        		if (oc.getPrefix().length()>0) {
        			ldp.append("[").append(oc.getPrefix()).append("]");
        		}
        		if (oc.getPrefix().length()>0 || oc.getSuffix().length()>0) { // One optional arguments means suffix
        			ldp.append("[").append(oc.getSuffix()).append("]");
        		}
        		ldp.append("{").append(identifiers.toString()).append("}"); // TODO: Check sIdentifier in the list oc.getKey() and do what if not??
        	} else {
	            ldp.append("\\autocite{").append(identifiers.toString()).append("}");
        	}
        }
    }
	
    /** Get the BibTeX document, if any (that is if the document contains bibliographic data <em>and</em>
     * the configuration does not specify external BibTeX files)
     * 
     * @return the BiBTeXDocument, or null if no BibTeX file is needed
     */
    BibTeXDocument getBibTeXDocument() {
    	if (config.useBiblatex() && bibDoc!=null && !bibDoc.isEmpty()) {
    		return bibDoc;
    	}
    	return null;
    }
	    
}