/************************************************************************
 *
 *  CharStyleConverter.java
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
 *  Version 2.0 (2022-04-27)
 *
 */

package writer2latex.latex;

import java.util.Map;

import writer2latex.util.*;
import writer2latex.office.*;
import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.latex.util.StyleMapItem;

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
    private boolean bIgnoreFont;
    private boolean bIgnoreAll;
    private boolean bUseUlem;
    // Do we need actually use ulem.sty?
    private boolean bNeedUlem = false;
    
    /** <p>Constructs a new <code>CharStyleConverter</code>.</p>
     */
    public CharStyleConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);

        bUseUlem = config.useUlem();

        // No character formatting at all:
        bIgnoreAll = config.formatting()==LaTeXConfig.IGNORE_ALL;
        // No font family:
        bIgnoreFont = config.formatting()<=LaTeXConfig.IGNORE_MOST;
        // No fontsize:
        bIgnoreFontsize = config.formatting()<=LaTeXConfig.CONVERT_BASIC;
        // No hard fontsize
        bIgnoreHardFontsize = config.formatting()<=LaTeXConfig.CONVERT_MOST;
    }
	
    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
        if (bNeedUlem) {
        	pacman.usepackage("normalem", "ulem");
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
            Map<String,StyleMapItem> sm = config.getTextStyleMap();
            if (sm.containsKey(sDisplayName)) {
                ba.add(sm.get(sDisplayName).getBefore(),sm.get(sDisplayName).getAfter());
                boolean bVerbatim = sm.containsKey(sDisplayName) && sm.get(sDisplayName).getVerbatim();
                context.setVerbatim(bVerbatim);
                context.setNoLineBreaks(bVerbatim);
            }
            return;
        }

        // Style already converted?
        if (styleMap.containsKey(sName)) {
            ba.add(styleMap.get(sName).getBefore(),styleMap.get(sName).getAfter());
            context.updateFormattingFromStyle(ofr.getTextStyle(sName));
            // it's verbatim if specified as such in the configuration
            Map<String,StyleMapItem> sm = config.getTextStyleMap();
            boolean bIsVerbatim = sm.containsKey(sDisplayName) && sm.get(sDisplayName).getVerbatim(); 
            context.setVerbatim(bIsVerbatim);
            context.setNoLineBreaks(bIsVerbatim);
            return;
        }

        // The style may already be declared in the configuration:
        Map<String,StyleMapItem> sm = config.getTextStyleMap();
        if (sm.containsKey(sDisplayName)) {
            styleMap.put(sName,new StyleMapItem(sName,sm.get(sDisplayName).getBefore(),sm.get(sDisplayName).getAfter()));
            applyTextStyle(sName,ba,context);
            return;
        }
		
        // Get the style, if it exists:
        StyleWithProperties style = ofr.getTextStyle(sName);
        if (style==null) {
            styleMap.put(sName,new StyleMapItem(sName,"",""));
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
        if (!baText.isEmpty()) { // Declare the text style (\newcommand)
	        String sTeXName = styleNames.getExportName(ofr.getTextStyles().getDisplayName(sName));
	        styleMap.put(sName,new StyleMapItem(sName,"\\textstyle"+sTeXName+"{","}"));
	        declarations.append("\\newcommand\\textstyle")
	            .append(sTeXName).append("[1]{")
	            .append(baText.getBefore()).append("#1").append(baText.getAfter())
	            .append("}").nl();
        }
        else { // Empty definition, ignore
        	styleMap.put(sName, new StyleMapItem(sName,"",""));        	
        }
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

    /** Apply all font attributes (family, series, shape, size and color), disregarding the setting of the
     * formatting attribute
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFullFont(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (style!=null) {
        	applyNfssSize(style,bDecl,bInherit,ba,context);
	        palette.getI18n().applyFontFamily(style,bDecl,bInherit,ba,context);
	        applyNfssSeries(style,bDecl,bInherit,ba,context);
	        applyNfssShape(style,bDecl,bInherit,ba,context);
	        palette.getColorCv().applyColor(style,bDecl,bInherit,ba,context);
        }
    }
	
    /** Apply all font attributes (family, series, shape, size and color), taking the formatting attribute into account
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFont(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
    	// What gets converted depends on the formatting attribute
        if (style!=null) {
	        if (!(bIgnoreFontsize || (bIgnoreHardFontsize && style.isAutomatic()))) {
	        	applyNfssSize(style,bDecl,bInherit,ba,context);
	        }
	        if (!bIgnoreFont) {
	        	palette.getI18n().applyFontFamily(style,bDecl,bInherit,ba,context);
	        }
	        if (!bIgnoreAll) {
	        	applyNfssSeries(style,bDecl,bInherit,ba,context);
	        	applyNfssShape(style,bDecl,bInherit,ba,context);
		        palette.getColorCv().applyColor(style,bDecl,bInherit,ba,context);
	        }
        }
    }
	
    /** <p>Reset to normal font, size and color.</p>
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyNormalFont(BeforeAfter ba) {
        ba.add("\\normalfont\\normalsize","");
        palette.getColorCv().applyNormalColor(ba);
    }

    /** <p>Apply font effects (position, underline, cross out, change case, disregarding the formatting property</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFullFontEffects(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null) {
        	applyTextPosition(style, bInherit, ba);
        	applyUnderline(style, bInherit, ba);
        	applyCrossout(style, bInherit, ba);
        	applyChangeCase(style, bInherit, ba);
        	palette.getMicrotypeCv().applyLetterspace(style, bInherit, ba);
        }
    }
	
    /** <p>Apply font effects (position, underline, cross out, change case</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyFontEffects(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (!bIgnoreAll) {
        	applyFullFontEffects(style, bInherit, ba);
        }
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
        if (style!=null) {
        	String sSeries = nfssSeries(style.getProperty(XMLString.FO_FONT_WEIGHT,bInherit));
        	if (sSeries!=null) {
        		// Temporary: Support text-attribute style maps for this particular case
        		// TODO: Reimplement the CharStyleConverter to properly support this...
            	if (!bDecl && "bf".equals(sSeries) && config.getTextAttributeStyleMap().containsKey("bold")) {
            		ba.add(config.getTextAttributeStyleMap().get("bold").getBefore(),
            			   config.getTextAttributeStyleMap().get("bold").getAfter());
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
        if (style!=null) {
        	String sVariant = style.getProperty(XMLString.FO_FONT_VARIANT, bInherit);
        	String sStyle = style.getProperty(XMLString.FO_FONT_STYLE, bInherit);
        	String sShape = nfssShape(sVariant,sStyle);
        	if (sShape!=null) {
        		// Temporary: Support text-attribute style maps for this particular case
        		// TODO: Reimplement the CharStyleConverter to properly support this...
            	if (!bDecl && "sc".equals(sShape) && config.getTextAttributeStyleMap().containsKey("small-caps")) {
            		ba.add(config.getTextAttributeStyleMap().get("small-caps").getBefore(),
            			   config.getTextAttributeStyleMap().get("small-caps").getAfter());
            	}
            	else if (!bDecl && "it".equals(sShape) && config.getTextAttributeStyleMap().containsKey("italic")) {
            		ba.add(config.getTextAttributeStyleMap().get("italic").getBefore(),
             			   config.getTextAttributeStyleMap().get("italic").getAfter());
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
        if (style!=null) {
	        if (style.getProperty(XMLString.FO_FONT_SIZE, bInherit)==null) { return; }
	        String sSize = nfssSize(style.getAbsoluteProperty(XMLString.FO_FONT_SIZE));
	        if (sSize==null) { return; }
	        if (sSize.equals(nfssSize(context.getFontSize()))) { return; } 
	        if (bDecl) { ba.add(sSize,""); }
	        else { ba.add("{"+sSize+" ","}"); }
        }
    }

    // Remaining methods are not context-sensitive

    /** <p>Apply text position.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyTextPosition(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null) {
        	String s = textPosition(style.getProperty(XMLString.STYLE_TEXT_POSITION, bInherit));
    		// Temporary: Support text-attribute style maps for this particular case
    		// TODO: Reimplement the CharStyleConverter to properly support this...
        	if (config.getTextAttributeStyleMap().containsKey("superscript") && "\\textsuperscript".equals(s)) {
        		ba.add(config.getTextAttributeStyleMap().get("superscript").getBefore(),
        			   config.getTextAttributeStyleMap().get("superscript").getAfter());
        	}
        	else if (config.getTextAttributeStyleMap().containsKey("subscript") && "\\textsubscript".equals(s)) {
        		ba.add(config.getTextAttributeStyleMap().get("subscript").getBefore(),
        			   config.getTextAttributeStyleMap().get("subscript").getAfter());
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
        if (style!=null && bUseUlem) {
	        String sStyle = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_STYLE,bInherit);
	        String sType = style.getProperty(XMLString.STYLE_TEXT_UNDERLINE_TYPE,bInherit);
	        String s = underline(sStyle, sType);
	        if (s!=null) {
	        	bNeedUlem = true; ba.add(s+"{","}");
	        }
        }
    }

    /** <p>Apply text cross out.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyCrossout(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null && bUseUlem) {
	        String sStyle = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_STYLE,bInherit);
	        String sText = style.getProperty(XMLString.STYLE_TEXT_LINE_THROUGH_TEXT,bInherit);
	        String s = crossout(sStyle, sText);
	        if (s!=null) {
	        	bNeedUlem = true; ba.add(s+"{","}");
	        }
        }
    }

    /** <p>Apply change case.</p>
     *  @param style the ODF style to read attributes from
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    private void applyChangeCase(StyleWithProperties style, boolean bInherit, BeforeAfter ba) {
        if (style!=null) {
	        String s = changeCase(style.getProperty(XMLString.FO_TEXT_TRANSFORM));
	        if (s!=null) { ba.add(s+"{","}"); }
        }
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
    	if (sTextPosition!=null && !sTextPosition.isEmpty()) {
    		// Value is <relative position> [<relative size>]
    		String[] sArguments = sTextPosition.split(" ");
    		if (sArguments.length==1 || !sArguments[1].equals("100%")) {
    			// Only export if font size is unspecified or different from 100%
		        if (sArguments[0].equals("sub") || sArguments[0].startsWith("-")) {
		        	// sub or any negative percentage implies subscript
		            return "\\textsubscript";
		        }
		        else if (sArguments[0].equals("super") || !sArguments[0].equals("0%")) {
		        	// super or any positive percentage implies superscript
		        	return "\\textsuperscript";
		        }
    		}
    	}
    	return null;
    }
    
    private static final String underline(String sStyle, String sType) {
    	if (sStyle!=null) {
    		switch(sStyle) {
        	case "wave":
        		return "\\uwave";
        	case "dash":
        	case "long-dash":
        	case "dot-dash":
        		return "\\dashuline";
        	case "dot-dot-dash":
        	case "dotted":
        		return "\\dotuline";
        	case "solid":
        		if ("double".equals(sType)) {
        			return "\\uuline";
        		}
        		else {
        			return "\\uline";
        		}
        	case "none":
        	default:
        		return null;
        	}
    	}
    	return null;
    }
    	
    private static final String crossout(String sStyle, String sText) {
    	if (sStyle!=null && sStyle!="none") {
    		return sText!=null ? "\\xout" : "\\sout";
    	}
    	return null;
    }
	
    private static final String changeCase(String sTextTransform){
        if ("lowercase".equals(sTextTransform)) return "\\MakeLowercase";
        if ("uppercase".equals(sTextTransform)) return "\\MakeUppercase";
        return null;
    }
    
}
