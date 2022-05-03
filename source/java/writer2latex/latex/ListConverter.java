/************************************************************************
 *
 *  ListConverter.java
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
 *  Version 2.0 (2022-05-03)
 *
 */
package writer2latex.latex;

import java.util.Enumeration;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.ListStyle;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Calc;
import writer2latex.util.Misc;

/** This class handles conversion of lists and list formatting, optionally using the package enumitem.sty
 */
public class ListConverter extends StyleConverter {
	private boolean bHasListStyles = false;
	
    /** Construct a new <code>ListConverter</code>
     */
    public ListConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }

	@Override public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
		if (config.useEnumitem()) {
			pacman.usepackage("calc"); // TODO move elsewhere
			pacman.usepackage("enumitem");
		}
		if (bHasListStyles) {
			decl.append("% List styles").nl();
			Enumeration<String> keys = styleNames.keys();
			while (keys.hasMoreElements()) {
				String sStyleName = keys.nextElement();
				ListStyle style = ofr.getListStyle(sStyleName);
				if (style!=null && !style.isAutomatic()) {
					decl.append("\\newlist{list").append(styleNames.getExportName(sStyleName)).append("}{")
						.append("enumerate") // TODO: itemize if no enumerated levels
						.append("}{4}").nl();
					Context oc = new Context();
					oc.setListStyleName(sStyleName);
					for (int nLevel=1; nLevel<5; nLevel++) {
						oc.setListLevel(nLevel);
						CSVList props = new CSVList(",","=");
						createLabel(props, oc);
						createStyledStartValue(props, oc);
						createLayout(props, oc);
						decl.append("\\setlist[list").append(styleNames.getExportName(sStyleName)).append(",")
							.append(nLevel).append("]{").append(props.toString()).append("}").nl();
					}
				}
			}
		}
		super.appendDeclarations(pacman,decl);
	}

    /** Process a list (text:list, text:ordered-list or text:unordered-list tag)
     * @param node The element containing the list
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void handleList(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Set up new context, increasing the list level and saving the list style name
        Context ic = (Context) oc.clone();
        ic.incListLevel();
        if (ic.getListStyleName()==null) {
            ic.setListStyleName(node.getAttribute(XMLString.TEXT_STYLE_NAME));
        }

        // If the list contains headings, ignore it!        
        if (ic.isIgnoreLists() || listContainsHeadings(node)) {
            ic.setIgnoreLists(true);
            traverseList(node,ldp,ic);
            return;
        }
        
        // Any item may restart the numbering. If this happens on the first item, we can fix this on the list level
        // If it happens on another item we currently ignore it
        String sItemStartValue = null;
        Node child = Misc.getFirstChildElement(node);
        if (Misc.isElement(child,XMLString.TEXT_LIST_ITEM) ) {
        	sItemStartValue = Misc.getAttribute(child, XMLString.TEXT_START_VALUE);
        }
        
        // Does this list continue numbering from the previous list (with the same style name)?
        boolean bContinue = "true".equals(node.getAttribute(XMLString.TEXT_CONTINUE_NUMBERING));
        
        // Apply the style
        BeforeAfter ba = new BeforeAfter();
        applyListStyle(sItemStartValue,bContinue,ba,ic);
			
        // Export the list
        if (ba.getBefore().length()>0) { ldp.append(ba.getBefore()).nl(); }
        traverseList(node,ldp,ic);
        if (ba.getAfter().length()>0) { ldp.append(ba.getAfter()).nl(); }
    }

    // Process the contents of a list
    private void traverseList (Element node, LaTeXDocumentPortion ldp, Context oc) {
    	Node child = node.getFirstChild();
    	while (child!=null) {
	        if (child.getNodeType() == Node.ELEMENT_NODE) {
	            String nodeName = child.getNodeName();
				
	            palette.getInfo().addDebugInfo((Element)child,ldp);
	            
	            if (nodeName.equals(XMLString.TEXT_LIST_ITEM)) {
	                handleListItem((Element)child,ldp,oc);
	            }
	            if (nodeName.equals(XMLString.TEXT_LIST_HEADER)) {
	                handleListItem((Element)child,ldp,oc);
	            }
	        }
	        child = child.getNextSibling();
        }
    }
    
    // Process a list item
    private void handleListItem(Element node, LaTeXDocumentPortion ldp, Context oc) {
        // Are we ignoring this list?
        if (oc.isIgnoreLists()) {
            palette.getBlockCv().traverseBlockText(node,ldp,oc);
            return;
        }
        
        // Apply the style
        BeforeAfter ba = new BeforeAfter();
        applyListItemStyle(node.getNodeName().equals(XMLString.TEXT_LIST_HEADER), ba, oc);
			
        // export the list item
        ldp.append(ba.getBefore());
        palette.getBlockCv().traverseBlockText(node,ldp,oc);
        ldp.append(ba.getAfter());
    }

    // Helper: Check to see, if this list contains headings (in that case we will ignore the list!)  
    private boolean listContainsHeadings (Node node) {
    	Node child = node.getFirstChild();
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if (nodeName.equals(XMLString.TEXT_LIST_ITEM)) {
                    if (listItemContainsHeadings(child)) return true;
                }
                if (nodeName.equals(XMLString.TEXT_LIST_HEADER)) {
                    if (listItemContainsHeadings(child)) return true;
                }
            }
	        child = child.getNextSibling();
        }
        return false;
    }
    
    private boolean listItemContainsHeadings(Node node) {
    	Node child = node.getFirstChild();
    	while (child!=null) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                String nodeName = child.getNodeName();
                if(nodeName.equals(XMLString.TEXT_H)) {
                    return true;
                }
                if (nodeName.equals(XMLString.TEXT_LIST)) {
                    if (listContainsHeadings(child)) return true;
                }
            }
	        child = child.getNextSibling();
        }
        return false;
    }
    
    // Remaining methods are used to convert style information
    
	private void applyListStyle(String sItemStartValue, boolean bContinue, BeforeAfter ba, Context oc) {
        String sDisplayName = ofr.getListStyles().getDisplayName(oc.getListStyleName());
		// Step 1. We may have a style map, this always takes precedence
		if (config.getListStyleMap().containsKey(sDisplayName)) {
			ba.add(config.getListStyleMap().get(sDisplayName).getBefore(),
					config.getListStyleMap().get(sDisplayName).getAfter()); 
		}
		else if (oc.getListLevel()<=4) {
			// Step 2. Create list environments
	        ListStyle style = ofr.getListStyle(oc.getListStyleName());
	        if (style!=null && config.useEnumitem() && config.listStyles() && !style.isAutomatic()) { // Convert list styles
	        	ba.add("\\begin{","}");
	        	ba.add("list"+styleNames.getExportName(oc.getListStyleName()),"list"+styleNames.getExportName(oc.getListStyleName()));
	        	ba.add("}","\\end{");
	        	bHasListStyles = true;
	        	// TODO: Restart if required
	        }
	        else { // Otherwise create default lists
				if (style!=null && style.isNumber(oc.getListLevel())) {
					ba.add("\\begin{enumerate}","\\end{enumerate}");
				}
				else {
					ba.add("\\begin{itemize}","\\end{itemize}");
				}
				// Step 3: Use enumitem.sty to add formatting
				if (config.useEnumitem()) {
					CSVList props = new CSVList(",","=");
					createLabel(props, oc);
					createStyledStartValue(props, oc); // which the following may override
					createHardStartValue(sItemStartValue, bContinue, props, oc);
					createLayout(props, oc);
					if (!props.isEmpty()) {
						ba.add("["+props.toString()+"]","");
					}
				}
	        }
		}
	}
	
	// Create label and ref options
	private void createLabel(CSVList props, Context oc) {
		ListStyle style = ofr.getListStyle(oc.getListStyleName());
		if (style!=null) {
			// Apply text style
			BeforeAfter baText = new BeforeAfter();
			palette.getCharSc().applyTextStyle(style.getLevelProperty(oc.getListLevel(),XMLString.TEXT_STYLE_NAME),
					baText,new Context()); // TODO: Probably oc?
			// Create label
			if (style.isNumber(oc.getListLevel())) {
				// Add prefix and suffix. Note: It is not customary in LaTeX to include the prefix and suffix in reference, so we don't.
				// However FieldConverter adds it as plain text in order to give the same result at the original.
				boolean bComma = false;
				String sPrefix = style.getLevelProperty(oc.getListLevel(),XMLString.STYLE_NUM_PREFIX);
				if (sPrefix!=null) {
					baText.addBefore(palette.getI18n().convert(sPrefix,false,"en"));
					bComma|=sPrefix.indexOf(',')>-1;
				}
				String sSuffix = style.getLevelProperty(oc.getListLevel(),XMLString.STYLE_NUM_SUFFIX);
				if (sSuffix!=null) {
					baText.addAfter(palette.getI18n().convert(sSuffix,false,"en"));
					bComma|=sSuffix.indexOf(',')>-1;
				}
				// Create numbering
				StringBuffer label = new StringBuffer();
				int nLevels = Misc.getPosInteger(style.getLevelProperty(oc.getListLevel(),XMLString.TEXT_DISPLAY_LEVELS),1);
				for (int j=oc.getListLevel()-nLevels+1; j<oc.getListLevel(); j++) {
					if (style.isNumber(j)) {
						label.append(numFormat(style.getLevelProperty(j,XMLString.STYLE_NUM_FORMAT)))
							.append("{enum").append(Misc.int2roman(j)).append("}.");
					}
				} 
				label.append(numFormat(style.getLevelProperty(oc.getListLevel(),XMLString.STYLE_NUM_FORMAT))).append("*");
				String sLabel = label.toString();
				// Create properties for enumitem
				boolean bNeedsRef = !baText.isEmpty();
				if (bComma) { // Need to enclose value in {} if the label contains a comma
					baText.enclose("{", "}");
				}
				props.addValue("label", baText.getBefore()+sLabel+baText.getAfter());
				if (bNeedsRef) { // Plain label for references
					props.addValue("ref", sLabel);
				}
			}
			else if (style.isBullet(oc.getListLevel())) {
				// Create bullet
				String sBullet = style.getLevelProperty(oc.getListLevel(),XMLString.TEXT_BULLET_CHAR);
				if (sBullet!=null) {
					String sFontName = palette.getCharSc().getFontName(style.getLevelProperty(oc.getListLevel(),XMLString.TEXT_STYLE_NAME));
					palette.getI18n().pushSpecialTable(sFontName);
					// Bullets are usually symbols, so this should be OK:
					props.addValue("label", baText.getBefore()+palette.getI18n().convert(sBullet,false,"en")+baText.getAfter());
					palette.getI18n().popSpecialTable();
				}
			}
			else {
				// TODO: Support images; currently use default bullet
			}
		}
	}
	
	// Helper: Convert ODF number format to LaTeX number format
	public static final String numFormat(String sFormat){
		if ("1".equals(sFormat)) { return "\\arabic"; }
		else if ("i".equals(sFormat)) { return "\\roman"; }
		else if ("I".equals(sFormat)) { return "\\Roman"; }
		else if ("a".equals(sFormat)) { return "\\alph"; }
		else if ("A".equals(sFormat)) { return "\\Alph"; }
		else { return null; }
	}
	
	// Create start, resume and series options
	private void createHardStartValue(String sItemStartValue, boolean bContinue, CSVList props, Context oc) {
		if (bContinue) { // For at continued list we only need resume
			props.addValue("resume", "list"+styleNames.getExportName(oc.getListStyleName()));
		}
		else { // Otherwise we need series and optionally start
			props.addValue("series", "list"+styleNames.getExportName(oc.getListStyleName()));						
			if (sItemStartValue!=null) { // Start value on list item overrides the value from the style
				props.addValue("start", Integer.toString(Misc.getPosInteger(sItemStartValue, 1)));
			}
		}
	}

	// Create start value from style
	private void createStyledStartValue(CSVList props, Context oc) {
		ListStyle style = ofr.getListStyle(oc.getListStyleName());
		if (style!=null) {
			String sStartValue = style.getLevelProperty(oc.getListLevel(), XMLString.TEXT_START_VALUE);
			if (sStartValue!=null) { // Ensure that we have a valid number
				props.addValue("start", Integer.toString(Misc.getPosInteger(sStartValue, 1)));
			}
		}			
	}
	
	// Create leftmargin, itemindent, labelwidth, labelsep and align options
	private void createLayout(CSVList props, Context oc) {
		ListStyle style = ofr.getListStyle(oc.getListStyleName());
		int nLevel = oc.getListLevel();
		if (config.listLayout() && style!=null && style.isNewType(nLevel)) {
			// This is the new type introduced in ODF 1.2 (text:list-level-position-and-space-mode="label-alignment"); old type is ignored

			// First we have 9 different variants of layout
			String sTextAlign = style.getLevelStyleProperty(nLevel,XMLString.FO_TEXT_ALIGN);
			// fo:text-align (of label): Possible values are start, end, left, right, center, justify
			// We can only support left and right out of the box with enumitem.sty (the package does have provision to introduce new
			// alignment types, but clean LaTeX code has a higher priority). The default value is left.
			boolean bLeft = sTextAlign==null || "start".equals(sTextAlign) || "left".equals(sTextAlign);
			// text:label-followed-by: Possible values are listtab, space, nothing
			String sFormat = style.getLevelStyleProperty(nLevel, XMLString.TEXT_LABEL_FOLLOWED_BY);

			// The actual layout is determined by three lengths
			// fo:margin-left is the left margin of the text body
			String sMarginLeft = getLength(style, nLevel, XMLString.FO_MARGIN_LEFT);
			if (nLevel>1) { // The ODF value is from page margin; we need it to be relative to the previous level
				sMarginLeft = Calc.sub(sMarginLeft, getLength(style, nLevel-1, XMLString.FO_MARGIN_LEFT));
			}
			// fo:text is the position of the label, or rather the justification point.
			// This is relative to the text, we need it to be relative to the previous level
			String sTextIndent = Calc.add(sMarginLeft,getLength(style, nLevel, XMLString.FO_TEXT_INDENT));
			// text:list-tab-stop-position (only if label is followed by tab stop) is the start position of the first line of text body
			String sTabPos = getLength(style, nLevel, XMLString.TEXT_LIST_TAB_STOP_POSITION);
			if (nLevel>1) { // 
				sTabPos = Calc.sub(sTabPos, getLength(style, nLevel-1, XMLString.FO_MARGIN_LEFT));
			}

			// We are now ready to set up options for enumitem.sty
			// The left margin is straightforward
			props.addValue("leftmargin",sMarginLeft);
			if ("listtab".contentEquals(sFormat)) {
				props.addValue("itemindent", Calc.sub(sTabPos, sMarginLeft));
				if (bLeft) { // The label is positioned from the margin to the alignment position (sTextIndent)
					props.addValue("labelsep", "0mm");
					props.addValue("labelwidth", Calc.sub(sTabPos, sTextIndent));
					props.addValue("align", "left");				
				}
				else { // The label is positioned from the alignment position (sTextIndent) to the text body
					props.addValue("labelsep", Calc.sub(sTabPos, sTextIndent));
					props.addValue("labelwidth", sTextIndent);
					props.addValue("align", "right");
				}
			}
			else {
				if ("space".contentEquals(sFormat)) { // The width of a space is 0.33em
					props.addValue("itemindent", Calc.sub(sTextIndent, sMarginLeft)+"+0.33em"); // This one needs calc.sty
					// TODO: Load calc.sty (currently handled by StarMathConverter.java; should be moved elsewhere)
					props.addValue("labelsep", "0.33em");
				}
				else { // "nothing"
					props.addValue("itemindent", Calc.sub(sTextIndent, sMarginLeft));
					props.addValue("labelsep", "0mm");
				}
				if (bLeft) { // The label has zero width, and the label extends into the text body
					props.addValue("labelwidth", "0mm");
					props.addValue("align", "left");
				}
				else { // The label is positioned from the margin to the alignment position (sTextIndent)
					props.addValue("labelwidth", sTextIndent);
					props.addValue("align", "right");				
				}
			}
		}
	}
	
	// Helper: Get a length property from a list level style that defaults to 0cm.
	private String getLength(ListStyle style,int nLevel,String sProperty) {
		String s = style.getLevelStyleProperty(nLevel,sProperty);
		if (s==null) { return "0cm"; }
		else { return s; }
	}	

	// Apply a list style to a list item
	private void applyListItemStyle(boolean bHeader, BeforeAfter ba, Context oc) {
		String sDisplayName = ofr.getListStyles().getDisplayName(oc.getListStyleName());
		if (config.getListItemStyleMap().containsKey(sDisplayName)) {
			// If we have a style map, this always takes precedence
			ba.add(config.getListItemStyleMap().get(sDisplayName).getBefore(),
					config.getListItemStyleMap().get(sDisplayName).getAfter()); 
			return;
		}
		else {
			// Otherwise create a standard \item
			// TODO: May support higher levels for list styles if list_styles is true
			if (oc.getListLevel()<=4) {
				if (bHeader) { ba.addBefore("\\item[] "); }
				else { ba.addBefore("\\item "); }
			}
		}
	}

}
