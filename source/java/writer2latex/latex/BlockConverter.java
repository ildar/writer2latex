/************************************************************************
 *
 *  BlockConverter.java
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
 *  Version 2.0 (2018-07-30)
 *
 */

package writer2latex.latex;

import java.util.Map;
import java.util.Stack;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import writer2latex.latex.util.Context;
import writer2latex.latex.util.StyleMapItem;
import writer2latex.office.OfficeReader;
import writer2latex.office.XMLString;
import writer2latex.util.Misc;

/**
 *  This class handles basic block content, such as the main text body,
 *  sections, tables, lists, headings and paragraphs.</p>
 */
public class BlockConverter extends ConverterHelper {

    public BlockConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
    }
	
    public void appendDeclarations(LaTeXPacman pack, LaTeXDocumentPortion decl) {
        // currently do nothing..
    }


    /** <p> Traverse block text (eg. content of body, section, list item).
     * This is traversed in logical order and dedicated handlers take care of
     * each block element.</p>
     * <p> (Note: As a rule, all handling of block level elements should add a
     * newline to the LaTeX document at the end of the block)</p>
     * @param node The element containing the block text
     * @param ldp the <code>LaTeXDocumentPortion</code> to which
     * LaTeX code should be added
     * @param oc the current context
     */
    public void traverseBlockText(Element node, LaTeXDocumentPortion ldp, Context oc) {
        Context ic = (Context) oc.clone();

        // The current paragraph blocks
        Map<String,StyleMapItem> blockMap = config.getParBlockStyleMap();
        Stack<StyleMapItem> blockStack = new Stack<>();

        if (node.hasChildNodes()) {
            NodeList list = node.getChildNodes();
            int nLen = list.getLength();

            for (int i = 0; i < nLen; i++) {
                Node childNode = list.item(i);
				
                if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                    Element child = (Element)childNode;
                    String sTagName = child.getTagName();
					
                    palette.getFieldCv().flushReferenceMarks(ldp,ic);
                    palette.getIndexCv().flushIndexMarks(ldp,ic);
					
                    palette.getInfo().addDebugInfo(child,ldp);

                    // Basic block content; handle by this class
                    if (sTagName.equals(XMLString.TEXT_P)) {
                        // End paragraph blocks that does not include this paragraph
                        String sStyleName = ofr.getParStyles().getDisplayName(child.getAttribute(XMLString.TEXT_STYLE_NAME));
                        while (!blockStack.isEmpty() && !blockStack.peek().isNext(sStyleName)) {
                        	endBlock(blockStack,ldp,ic);
                        }
                        // start a new block, but not in tables, and only if the top level block allows nesting
                        if (!ic.isInTable() && blockMap.containsKey(sStyleName) 
                        		&& (blockStack.isEmpty() || blockStack.peek().allowsNesting())) {
                           	startBlock(blockMap.get(sStyleName),blockStack,ldp,ic);
                        }
                        // is this a caption?
                        String sSequence = ofr.getSequenceName(child);
                        if (ofr.isFigureSequenceName(sSequence)) {
                            palette.getDrawCv().handleCaption(child,ldp,ic);
                        }
                        else if (ofr.isTableSequenceName(sSequence)) {
                            // Next node *should* be a table
                            if (i+1<nLen && Misc.isElement(list.item(i+1),XMLString.TABLE_TABLE)) {
                                // Found table with caption above
                            	endBlock("table",blockStack,ldp,ic);
                                palette.getTableCv().handleTable((Element)list.item(++i),child,true,ldp,ic);
                            }
                            else {
                                // Found lonely caption
                                palette.getTableCv().handleCaption(child,ldp,ic);
                            }
                        }
                        else {
                            palette.getParCv().handleParagraph(child,ldp,ic,i==nLen-1);
                        }
                    }

                    else if(sTagName.equals(XMLString.TEXT_H)) {
                    	endBlock("heading",blockStack,ldp,ic);
                        palette.getHeadingCv().handleHeading(child,ldp,ic);
                    }
                    
                    else if (sTagName.equals(XMLString.TEXT_LIST)) {
                    	endBlock("list",blockStack,ldp,ic);
                        palette.getListCv().handleList(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TABLE_TABLE)) {
                    	endBlock("table",blockStack,ldp,ic);
                        // Next node *could* be a caption
                        if (i+1<nLen && Misc.isElement(list.item(i+1),XMLString.TEXT_P) &&
                            ofr.isTableSequenceName(ofr.getSequenceName((Element)list.item(i+1)))) {
                            // Found table with caption below
                            palette.getTableCv().handleTable(child,(Element)list.item(++i),false,ldp,oc);
                        }
                        else {
                            // Found table without caption
                            palette.getTableCv().handleTable(child,null,false,ldp,oc);
                        }
                    }

                    else if (sTagName.equals(XMLString.TABLE_SUB_TABLE)) {
                    	endBlock("table",blockStack,ldp,ic);
                        palette.getTableCv().handleTable(child,null,true,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_SECTION)) {
                    	endBlock("section",blockStack,ldp,ic);
                        palette.getSectionCv().handleSection(child,ldp,ic);
                    }

                    // Draw elements may appear in block context if they are
                    // anchored to page
                    else if (sTagName.startsWith("draw:")) {
                        palette.getDrawCv().handleDrawElement(child,ldp,ic);
                    }
					
                    // Indexes
                    else if (sTagName.equals(XMLString.TEXT_TABLE_OF_CONTENT)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleTOC(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_ILLUSTRATION_INDEX)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleLOF(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_TABLE_INDEX)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleLOT(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_OBJECT_INDEX)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleObjectIndex(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_USER_INDEX)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleUserIndex(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_ALPHABETICAL_INDEX)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getIndexCv().handleAlphabeticalIndex(child,ldp,ic);
                    }

                    else if (sTagName.equals(XMLString.TEXT_BIBLIOGRAPHY)) {
                    	endBlock("index",blockStack,ldp,ic);
                        palette.getBibCv().handleBibliography(child,ldp,ic);
                    }

                    // Sequence declarations appear in the main text body (before the actual content)
                    else if (sTagName.equals(XMLString.TEXT_SEQUENCE_DECLS)) {
                        palette.getFieldCv().handleSequenceDecls(child);
                    }
                    // other tags are ignored
                }
            }
        }

        // End of block sequence, end all blocks
        while (!oc.isInTable() && !blockStack.isEmpty()) {
        	endBlock(blockStack,ldp,ic);
        }
        palette.getFieldCv().flushReferenceMarks(ldp,ic);
        palette.getIndexCv().flushIndexMarks(ldp,ic);
    }
    
    private void startBlock(StyleMapItem smi, Stack<StyleMapItem> stack, LaTeXDocumentPortion ldp, Context oc) {
        stack.push(smi);
        String sBefore = smi.getBefore();
        if (sBefore.length()>0) ldp.append(sBefore).nl();
        oc.setVerbatim(smi.getVerbatim());    	
    }
    
    private void endBlock(String sItem, Stack<StyleMapItem> stack, LaTeXDocumentPortion ldp, Context oc) {
    	while (!stack.isEmpty() && !stack.peek().includes(sItem)) {
            // This item is not allowed in this block, end the block
        	endBlock(stack,ldp,oc);
    	}
    }
    
    private void endBlock(Stack<StyleMapItem> stack, LaTeXDocumentPortion ldp, Context oc) {
        String sAfter = stack.pop().getAfter();
        if (sAfter.length()>0) ldp.append(sAfter).nl();
        oc.setVerbatim(false);    	
    }
   
}