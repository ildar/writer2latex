/************************************************************************
 *
 *  ColorConverter.java
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
 *  Version 2.0 (2018-07-23)
 *
 */

package writer2latex.latex;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import writer2latex.latex.util.BeforeAfter;
import writer2latex.latex.util.Context;
import writer2latex.office.OfficeReader;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;


/** This class converts color using <code>xcolor.sty</code>
 */
public class ColorConverter extends ConverterHelper {
	
	// TODO: Add option to support dvipsnames, svgnames and x11names
	
	// Do we use color at all?
	private boolean bUseColor;
    
	// Pattern to syntax check color values
    private Matcher colorMatcher;

    // Map of named colors in xcolor.sty
    private Map<String,String> namedColors = new HashMap<>();
    
    // Map of automatic named colors
    private Map<String,String> autoNamedColors = new LinkedHashMap<>();

    /** Constructs a new <code>ColorConverter</code>
     * 
     * @param ofr the office reader for the source document
     * @param config the converter configuration to use
     * @param palette the converter palette to access other helpers
     */
    public ColorConverter(OfficeReader ofr, LaTeXConfig config, ConverterPalette palette) {
        super(ofr,config,palette);
        
        // Create matcher to check that color values are valid
        Pattern colorPattern = Pattern.compile("#[A-Fa-f0-9]{6}");
        colorMatcher = colorPattern.matcher("");

        // We use color if requested in the configuration, however ignoring
        // all formatting overrides this
        bUseColor = config.useXcolor() && config.formatting()>LaTeXConfig.IGNORE_ALL;
        
        // Create map of the 19 standard colors provided by xcolor
    	// These are given as decimal rgb values in xcolor.sty and converted to hex using the
        // formula round (f*255) as specified in the xcolor documentation        
		namedColors.put("#FF0000","red");		// 1,0,0
		namedColors.put("#00FF00","green"); 	// 0,1,0
		namedColors.put("#0000FF","blue"); 		// 0,0,1
		namedColors.put("#BF8040","brown"); 	// .75,.5,.25
		namedColors.put("#BFFF00","lime"); 		// .75,1,0
		namedColors.put("#FF8000","orange"); 	// 1,.5,0
		namedColors.put("#FFBFBF","pink"); 		// 1,.75,.75
		namedColors.put("#BF0040","purple"); 	// .75,0,.25
		namedColors.put("#008080","teal"); 		// 0,.5,.5
		namedColors.put("#800080","violet"); 	// .5,0,.5
		namedColors.put("#00FFFF","cyan"); 		// 0,1,1
		namedColors.put("#FF00FF","magenta"); 	// 1,0,1
		namedColors.put("#FFFF00","yellow"); 	// 1,1,0
		namedColors.put("#808000","olive"); 	// .5,.5,0
		namedColors.put("#000000","black"); 	// 0,0,0
		namedColors.put("#404040","darkgray"); 	// .25,.25,.25
		namedColors.put("#808080","gray"); 		// .5,.5,.5
		namedColors.put("#BFBFBF","lightgray"); // .75,.75,.75
		namedColors.put("#FFFFFF","white"); 	// 1,1,1
    }

    public void appendDeclarations(LaTeXPacman pacman, LaTeXDocumentPortion decl) {
	    if (bUseColor) {
	    	pacman.usepackage("xcolor");
            for (String sColor : autoNamedColors.keySet()) {
            	decl.append("\\definecolor{")
            	    .append(autoNamedColors.get(sColor)).append("}{HTML}{")
            	    .append(sColor.substring(1)).append("}").nl();
            }
        }
    }
	
    public void setNormalColor(String sColor, LaTeXDocumentPortion ldp) {
        if (bUseColor && sColor!=null) {
            ldp.append("\\renewcommand\\normalcolor{\\color")
               .append(color(sColor,false)).append("}").nl();
        }
    }
	
    public void applyNormalColor(BeforeAfter ba) {
        if (bUseColor) { ba.add("\\normalcolor",""); }
    }
	
    /** <p>Apply foreground color.</p>
     *  @param style the ODF style to read attributes from
     *  @param bDecl true if declaration form is required (if bDecl is true, nothing will be put in the "after" part of ba)
     *  @param bInherit true if inherited properties should be used
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     *  @param context the current context
     */
    public void applyColor(StyleWithProperties style, boolean bDecl, boolean bInherit, BeforeAfter ba, Context context) {
        if (bUseColor && style!=null) {
            String sColor = style.getProperty(XMLString.FO_COLOR,bInherit);
            if (sColor!=null) {
                if (!sColor.equals(context.getFontColor())) {
                    // Convert color if it differs from the current font color
                    context.setFontColor(sColor);
                    applyColor(sColor, bDecl, ba, context);
                }
            }
            else if (context.getFontColor()==null) {
                // No color; maybe automatic color?
                String sAutomatic = style.getProperty(XMLString.STYLE_USE_WINDOW_FONT_COLOR,bInherit);
                if (sAutomatic==null && bInherit) {
                    // We may need to inherit this property from the default style
                    StyleWithProperties defaultStyle = ofr.getDefaultParStyle();
                    if (defaultStyle!=null) {
                        sAutomatic = defaultStyle.getProperty(XMLString.STYLE_USE_WINDOW_FONT_COLOR,bInherit);
                    }
                }
                if ("true".equals(sAutomatic)) {
                    // Automatic color based on background
                    if (context.getBgColor()!=null) { applyAutomaticColor(ba,bDecl,context); } 
                }
            }
        }
    }
	
    /** <p>Apply a specific foreground color.</p>
     *  @param sColor the rgb color to use
     *  @param bDecl true if declaration form is required
     *  @param ba the <code>BeforeAfter</code> to add LaTeX code to.
     */
    public void applyColor(String sColor, boolean bDecl, BeforeAfter ba, Context context) {
        // Note: if bDecl is true, nothing will be put in the "after" part of ba.
        if (bUseColor && sColor!=null) {
            // If there's a background color, allow all colors
            String s = color(sColor, context.getBgColor()!=null);
            if (s!=null) {
                if (bDecl) { ba.add("\\color"+s,""); }
                else { ba.add("\\textcolor"+s+"{","}"); }
            }
        }
    }
	
    public void applyBgColor(String sCommand, String sColor, BeforeAfter ba, Context context) {
        // Note: Will only fill "before" part of ba
        if (sColor!=null && !"transparent".equals(sColor)) {
            String s = color(sColor, true);
            if (bUseColor && s!=null) {
                context.setBgColor(sColor);
                ba.add(sCommand+s,"");
            }
        }
    }
	
    public void applyAutomaticColor(BeforeAfter ba, boolean bDecl, Context context) {
        String s = automaticColor(context.getBgColor());
        if (s!=null) {
            if (bDecl) { ba.add("\\color"+s,""); }
            else { ba.add("\\textcolor"+s+"{","}"); }
        }
    }
    
    public boolean applyNamedColor(String sColor, String sProperty, CSVList props) {
    	if (bUseColor && sColor!=null && isValidColor(sColor)) {
    		String sColor1 = sColor.toUpperCase();
    		if (namedColors.containsKey(sColor1)) {
    			props.addValue(sProperty, namedColors.get(sColor1));
    		}
    		else {
    			if (!autoNamedColors.containsKey(sColor1)) {
    				autoNamedColors.put(sColor1, "color"+Misc.int2roman(autoNamedColors.size()+1));
    			}
    			props.addValue(sProperty, autoNamedColors.get(sColor1));
    		}
    		return true;
   		}
    	return false;
    }
    
    /** Add a color property to a longfbox.sty property list
     * 
     * @param sColor the color to apply
     * @param sProperty the longfbox property to set
     * @param props the property to which the property is to be added
     * @return true if a color was applied
     */
    public boolean applyLongfboxColor(String sColor, String sProperty, CSVList props) {
    	if (bUseColor && sColor!=null && isValidColor(sColor)) {
    		String sColor1 = sColor.toUpperCase();
    		if (namedColors.containsKey(sColor1)) {
    			props.addValue(sProperty, namedColors.get(sColor1));
    		}
    		else {
    			props.addValue(sProperty, "\\#"+sColor1.substring(1));    			
    		}
    		return true;
   		}
    	return false;
    }
    
    // Methods to create xcolor color expressions
    private final String automaticColor(String sBgColor) {
        if (sBgColor!=null && isValidColor(sBgColor)) {
            if (getLuminance(sBgColor)<0.3) { // Dark background
                return "{white}";
            }
        }
        return null;
    }
    
    private final String color(String sColor, boolean bFullColors) {
    	if (sColor!=null && isValidColor(sColor)) {
    		String sColor1 = sColor.toUpperCase();
    		if (!bFullColors) {
	            // avoid very bright colors (on white background):
	            if (getLuminance(sColor1)>0.85) {
	            	return "{black}";
	            }
    		}
    		if (namedColors.containsKey(sColor1)) {
    			return "{"+namedColors.get(sColor1)+"}";
    		}
    		return "[HTML]{"+sColor1.substring(1)+"}";
    	}
    	return null;
    }

    private boolean isValidColor(String sColor) {
    	return colorMatcher.reset(sColor).matches();
    }
    
    // Calclulate the percieved brightness
    private static final float getLuminance(String sColor) {
        float fRed = (float)Misc.getIntegerFromHex(sColor.substring(1,3),0)/255;
        float fGreen = (float)Misc.getIntegerFromHex(sColor.substring(3,5),0)/255;
        float fBlue = (float)Misc.getIntegerFromHex(sColor.substring(5,7),0)/255;
    	return 0.3F*fRed+0.59F*fGreen+0.11F*fBlue;
    }

}
