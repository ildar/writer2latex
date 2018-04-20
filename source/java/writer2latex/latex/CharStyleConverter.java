/************************************************************************
 *
 *  CharStyleConverter.java
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
 *  Version 2.0 (2018-04-19)
 *
 */

package writer2latex.latex;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.StyleMap;

/** This class creates LaTeX code from ODF character formatting
   Character formatting in ODF includes font, font effects/decorations and color.
   In addition it includes language/country information. Color is handled by the
   class <code>writer2latex.latex.ColorConverter</code> and language/country and
   font family is handled by the classes
   <code>writer2latex.latex.style.ClassicI18n</code> and
   <code>writer2latex.latex.style.XeTeXI18n</code>
 */
public class CharStyleConverter extends StyleConverter {

    // Which formatting should we export?
    private boolean bIgnoreHardFontsize;
    private boolean bIgnoreFontsize;
    private boolean bIgnoreAll;
    private boolean bUseUlem;
    // Do we need actually use ulem.sty or \textsubscript?
    private boolean bNeedUlem = false;
    private boolean bNeedSubscript = false;
    
    /** <p>Constructs a new <code>CharStyleConverter</code>.</p>
     */
    public CharStyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);

        bUseUlem = config.useUlem();

        // No character formatting at all:
        bIgnoreAll = config.formatting()==LaTeXConfig.IGNORE_ALL;
        // No fontsize:
        bIgnoreFontsize = config.formatting()<=LaTeXConfig.CONVERT_BASIC;
        // No hard fontsize
        bIgnoreHardFontsize = config.formatting()<=LaTeXConfig.CONVERT_MOST;
    }
	
    public void appendDeclarations(LaTeXDocumentPortion pack, LaTeXDocumentPortion decl) {
        if (bNeedUlem) {
            pack.append("\\usepackage[normalem]{ulem}").nl();
        }
        if (bNeedSubscript && !config.getTextAttributeStyleMap().contains("subscript")) {
            decl.append("\\providecommand\\textsubscript[1]{\\ensuremath{{}_{\\text{#1}}}}").nl();
        }
        if (!styleNames.isEmpty()) {
            decl.append("% Text styles").nl().append(declarations);
        }
    }

    /** <p>Use a text style in LaTeX.</p>
     *  @param sName the name of the text style
     *  @param ba a <code>BeforeAfter</code> to put code into
     */
    public void applyTextStyle(String sName, BeforeAfter ba, Context context) {
        if (sName==null) { return; }
        String sDisplayName = ofr.getTextStyles().getDisplayName(sName);

        if (bIgnoreAll) {
            // Even if all is ignored, we still apply style maps from config..
            StyleMap sm = config.getTextStyleMap();
            if (sm.contains(sDisplayName)) {
                ba.add(sm.getBefore(sDisplayName),sm.getAfter(sDisplayName));
                context.setVerbatim(sm.getVerbatim(sDisplayName));
                context.setNoLineBreaks(sm.getVerbatim(sDisplayName));
            }
            return;
        }

        // Style already converted?
        if (styleMap.contains(sName)) {
            ba.add(styleMap.getBefore(sName),styleMap.getAfter(sName));
            context.updateFormattingFromStyle(ofr.getTextStyle(sName));
            // it's verbatim if specified as such in the configuration
            StyleMap sm = config.getTextStyleMap();
            boolean bIsVerbatim = sm.contains(sDisplayName) && sm.getVerbatim(sDisplayName); 
            context.setVerbatim(bIsVerbatim);
            context.setNoLineBreaks(bIsVerbatim);
            return;
        }

        // The style may already be declared in the configuration:
        StyleMap sm = config.getTextStyleMap();
        if (sm.contains(sDisplayName)) {
            styleMap.put(sName,sm.getBefore(sDisplayName),sm.getAfter(sDisplayName));
            applyTextStyle(sName,ba,context);
            return;
        }
		
        // Get the style, if it exists:
        StyleWithProperties style = ofr.getTextStyle(sName);
        if (style==null) {
            styleMap.put(sName,"","");
            applyTextStyle(sName,ba,context);
            return;
        }

        // Convert automatic style  
        if (style.isAutomatic()) {
            palette.getI18n().applyLanguage(style,false,true,ba);
            applyFont(style,false,true,ba,context);
            applyFontEffects(style,true,ba);
            context.updateFormattingFromStyle(ofr.getTextStyle(sName));
            return;			
        }

        // Convert soft style:
        // This must be converted relative to a blank context!
        BeforeAfter baText = new BeforeAfter();
        palette.getI18n().applyLanguage(style,false,true,baText);
        applyFont(style,false,true,baText,new Context());
        applyFontEffects(style,true,baText);
        // declare the text style (\newcommand)
        String sTeXName = styleNames.getExportName(ofr.getTextStyles().getDisplayName(sName));
        styleMap.put(sName,"\\textstyle"+sTeXName+"{","}");
        declarations.append("\\newcommand\\textstyle")
            .append(sTeXName).append("[1]{")
            .append(baText.getBefore()).append("#1").append(baText.getAfter())
            .append("}").nl();
        applyTextStyle(sName,ba,context);
    }
	
    public String getFontName(StyleWithProperties style) {
        if (style!=null) {
            String sName = style.getProperty(XMLString.STYLE_FONT_NAME);
            if (sName!=null) {
                FontDeclaration fd = ofr.getFontDeclaration(sName);
                if (fd!=null) {
                    return fd.getFirstFontFamily();
                }             
            }
        }
        return null;
    }
	
    // Get the font name from a char style
    public String getFontName(String sStyleName) {
        return getFontName(ofr.getTextStyle(sStyleName));
    }

    /** <p>Apply hard character formatting (no inheritance).</p>
     *  <p>This is used in sections and {foot|end}notes</p>
     *  @param style the style to use
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to
     */
    public void applyHardCharFormatting(StyleWithProperties style, BeforeAfter ba) {
        palette.getI18n().applyLanguage(style,true,false,ba);
        applyFont(style,true,false,ba,new Context());
        if (!ba.isEmpty()) { ba.add(" ",""); }
    }

    /** <p>Apply all font attributes (family, series, shape, size and color).</p>
     *  @param style the ODF style to read attributesfrom
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFont(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (style==null) { return; }
        applyNfssSize(style,bDecl,bInherit,ba,context);
        palette.getI18n().applyFontFamily(style,bDecl,bInherit,ba,context);
        applyNfssSeries(style,bDecl,bInherit,ba,context);
        applyNfssShape(style,bDecl,bInherit,ba,context);
        palette.getColorCv().applyColor(style,bDecl,bInherit,ba,context);
    }
	
    /** <p>Reset to normal font, size and color.</p>
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyNormalFont(BeforeAfter ba) {
        ba.add("\\normalfont\\normalsize","");
        palette.getColorCv().applyNormalColor(ba);
    }

    /** <p>Apply font effects (position, underline, cross out, change case.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFontEffects(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style==null) { return; }
        applyTextPosition(style, bInherit, ba);
        applyUnderline(style, bInherit, ba);
        applyCrossout(style, bInherit, ba);
        applyChangeCase(style, bInherit, ba);
    }
	
    // Remaining methods are private

    /** <p>Apply font series.</p>
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyNfssSeries(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (style!=null && !bIgnoreAll) {
        	String sSeries = nfssSeries(style.getProperty(XMLString.FO_FONT_WEIGHT,bInherit));
        	if (sSeries!=null) {
        		// Temporary: Support text-attribute style maps for this particular case
        		// TODO: Reimplement the CharStyleConverter to properly support this...
            	if (!bDecl && "bf".equals(sSeries) && config.getTextAttributeStyleMap().contains("bold")) {
            		ba.add(config.getTextAttributeStyleMap().getBefore("bold"),
            			   config.getTextAttributeStyleMap().getAfter("bold"));
            	}
            	else {
            		if (style.isAutomatic()) { // optimize hard formatting
            			if (sSeries.equals(nfssSeries(context.getFontWeight()))) { return; }
            			if (context.getFontWeight()==null && sSeries.equals("md")) { return; }
            		}
            		if (bDecl) { ba.add("\\"+sSeries+"series",""); }
            		else { ba.add("\\text"+sSeries+"{","}"); }
            	}
        	}
        }
    }

    /** <p>Apply font shape.</p>
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyNfssShape(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (style!=null && !bIgnoreAll) {
        	String sVariant = style.getProperty(XMLString.FO_FONT_VARIANT, bInherit);
        	String sStyle = style.getProperty(XMLString.FO_FONT_STYLE, bInherit);
        	String sShape = nfssShape(sVariant,sStyle);
        	if (sShape!=null) {
        		// Temporary: Support text-attribute style maps for this particular case
        		// TODO: Reimplement the CharStyleConverter to properly support this...
            	if (!bDecl && "sc".equals(sShape) && config.getTextAttributeStyleMap().contains("small-caps")) {
            		ba.add(config.getTextAttributeStyleMap().getBefore("small-caps"),
            			   config.getTextAttributeStyleMap().getAfter("small-caps"));
            	}
            	else if (!bDecl && "it".equals(sShape) && config.getTextAttributeStyleMap().contains("italic")) {
            		ba.add(config.getTextAttributeStyleMap().getBefore("italic"),
             			   config.getTextAttributeStyleMap().getAfter("italic"));
            	}
            	else {
            		if (style.isAutomatic()) { // optimize hard formatting
            			if (sShape.equals(nfssShape(context.getFontVariant(),context.getFontStyle()))) return;
            			if (context.getFontVariant()==null && context.getFontStyle()==null && sShape.equals("up")) return;
            		}
            		if (bDecl) ba.add("\\"+sShape+"shape","");
            		else ba.add("\\text"+sShape+"{","}");
            	}
        	}
        }
    }
        
    /** <p>Apply font size.</p>
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyNfssSize(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (style==null|| bIgnoreFontsize || (bIgnoreHardFontsize && style.isAutomatic())) { return; }
        if (style.getProperty(XMLString.FO_FONT_SIZE, bInherit)==null) { return; }
        String sSize = nfssSize(style.getAbsoluteProperty(XMLString.FO_FONT_SIZE));
        if (sSize==null) { return; }
        if (sSize.equals(nfssSize(context.getFontSize()))) { return; } 
        if (bDecl) { ba.add(sSize,""); }
        else { ba.add("{"+sSize+" ","}"); }
    }

    // Remaining methods are not context-sensitive

    /** <p>Apply text position.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyTextPosition(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null && !bIgnoreAll) {
        	String s = textPosition(style.getProperty(XMLString.STYLE_TEXT_POSITION, bInherit));
    		// Temporary: Support text-attribute style maps for this particular case
    		// TODO: Reimplement the CharStyleConverter to properly support this...
        	if (config.getTextAttributeStyleMap().contains("superscript") && "\\textsuperscript".equals(s)) {
        		ba.add(config.getTextAttributeStyleMap().getBefore("superscript"),
        			   config.getTextAttributeStyleMap().getAfter("superscript"));
        	}
        	else if (config.getTextAttributeStyleMap().contains("subscript") && "\\textsubscript".equals(s)) {
        		ba.add(config.getTextAttributeStyleMap().getBefore("subscript"),
        			   config.getTextAttributeStyleMap().getAfter("subscript"));
        	}
        	else if (s!=null) {
        		ba.add(s+"{","}");
        	}
        }
    }
	
    /** <p>Apply text underline.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyUnderline(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style==null || !bUseUlem) { return; }
        if (bIgnoreAll) { return; }
        String sTag = ofr.isOpenDocument() ?
            XMLString.STYLE_TEXT_UNDERLINE_STYLE :
            XMLString.STYLE_TEXT_UNDERLINE; 
        String s = underline(style.getProperty(sTag, bInherit));
        if (s!=null) { bNeedUlem = true; ba.add(s+"{","}"); }
    }

    /** <p>Apply text cross out.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyCrossout(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style==null || !bUseUlem) { return; }
        if (bIgnoreAll) { return; }
        String sTag = ofr.isOpenDocument() ?
            XMLString.STYLE_TEXT_LINE_THROUGH_STYLE :
            XMLString.STYLE_TEXT_CROSSING_OUT; 
        String s = crossout(style.getProperty(sTag, bInherit));
        if (s!=null) { bNeedUlem = true; ba.add(s+"{","}"); }
    }

    /** <p>Apply change case.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyChangeCase(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style==null) { return; }
        if (bIgnoreAll) { return; }
        String s = changeCase(style.getProperty(XMLString.FO_TEXT_TRANSFORM));
        if (s!=null) { ba.add(s+"{","}"); }
    }

    // The remaining methods are static helpers to convert single style properties

    // Font change. These methods return the declaration form if the paramater
    // bDecl is true, and otherwise the command form
    
    private static final String nfssSeries(String sFontWeight){
        if (sFontWeight==null) return null;
        if ("bold".equals(sFontWeight)) return "bf";
        else return "md";
    }
    
    private static final String nfssShape(String sFontVariant, String sFontStyle){
        if (sFontVariant==null && sFontStyle==null) return null;
        if ("small-caps".equals(sFontVariant)) return "sc";
        else if ("italic".equals(sFontStyle)) return "it";
        else if ("oblique".equals(sFontStyle)) return "sl";
        else return "up";
    }
    
    private static final String nfssSize(String sFontSize){
        if (sFontSize==null) return null;
        return "\\fontsize{"+sFontSize+"}{"+Calc.multiply("120%",sFontSize)+"}\\selectfont";
    }
    
    // other character formatting
        
    private final String textPosition(String sTextPosition){
        if (sTextPosition==null) return null;
        if (sTextPosition.startsWith("super")) return "\\textsuperscript";
        if (sTextPosition.startsWith("sub") || sTextPosition.startsWith("-")) {
            bNeedSubscript = true;
            return "\\textsubscript";
        }
        if (sTextPosition.startsWith("0%")) return null;
        return "\\textsuperscript";
    }
    
    private static final String underline(String sUnderline) {
        if (sUnderline==null) { return null; }
        if (sUnderline.equals("none")) { return null; }
        if (sUnderline.indexOf("wave")>=0) { return "\\uwave"; }
        return "\\uline";
    }
	
    private static final String crossout(String sCrossout) {
        if (sCrossout==null) { return null; }
        if (sCrossout.equals("X")) { return "\\xout"; }
        if (sCrossout.equals("slash")) { return "\\xout"; }
        return "\\sout";
    }
	
    private static final String changeCase(String sTextTransform){
        if ("lowercase".equals(sTextTransform)) return "\\MakeLowercase";
        if ("uppercase".equals(sTextTransform)) return "\\MakeUppercase";
        return null;
    }
    
}
