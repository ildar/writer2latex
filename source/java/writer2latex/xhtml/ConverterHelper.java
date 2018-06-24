/************************************************************************
 *
 *  ConverterHelper.java
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
 *  Version 2.0 (2018-04-02)
 *
 */

package writer2latex.xhtml;

import org.w3c.dom.Element;

import writer2latex.office.OfficeReader;
import writer2latex.util.Calc;

/** A <code>ConverterHelper</code> is responsible for conversion of some specific content into XHTML. 
 */
class ConverterHelper {
	
	// Member variables providing our content (set in constructor)
	OfficeReader ofr;
    XhtmlConfig config;
    Converter converter;
    
    // Scaling and unit transformation to use
    private String sScale;
    private String sColScale;
    private int nUnits;
	
    /** Construct a new converter helper based on a 
     * 
     * @param ofr the office reader used to access the source document
     * @param config the configuration to use
     * @param converter the main converter to which the helper belongs
     */
    ConverterHelper(OfficeReader ofr, XhtmlConfig config, Converter converter) {
        this.ofr = ofr;
        this.config = config;
        this.converter = converter;
        
        sScale = config.scaling();
        sColScale = config.columnScaling();
        nUnits = config.units();
    }
    
    // Convenience accessor methods to other converter helpers (only needed to save some typing)

    StyleConverter getStyleCv() { return converter.getStyleCv(); }

    TextStyleConverter getTextSc() { return converter.getStyleCv().getTextSc(); }
	
    ParStyleConverter getParSc() { return converter.getStyleCv().getParSc(); }
	
    HeadingStyleConverter getHeadingSc() { return converter.getStyleCv().getHeadingSc(); }
	
    ListStyleConverter getListSc() { return converter.getStyleCv().getListSc(); }
	
    SectionStyleConverter getSectionSc() { return converter.getStyleCv().getSectionSc(); }
	
    TableStyleConverter getTableSc() { return converter.getStyleCv().getTableSc(); }
	
    RowStyleConverter getRowSc() { return converter.getStyleCv().getRowSc(); }
	
    CellStyleConverter getCellSc() { return converter.getStyleCv().getCellSc(); }
	
    FrameStyleConverter getFrameSc() { return converter.getStyleCv().getFrameSc(); }
	
    PresentationStyleConverter getPresentationSc() { return converter.getStyleCv().getPresentationSc(); }
	
    PageStyleConverter getPageSc() { return converter.getStyleCv().getPageSc(); }
    
    TextConverter getTextCv() { return converter.getTextCv(); }
	
    TableConverter getTableCv() { return converter.getTableCv(); }

    DrawConverter getDrawCv() { return converter.getDrawCv(); }

    MathConverter getMathCv() { return converter.getMathCv(); }
	
    /** Apply style information to an XHTML node
     * 
     * @param info the style to apply
     * @param hnode the XHTML node
     */
    void applyStyle(StyleInfo info, Element hnode) {
        if (info.sClass!=null) {
            hnode.setAttribute("class",info.sClass);
        }
        if (!info.props.isEmpty()) {
            hnode.setAttribute("style",info.props.toString());
        }
        if (info.sLang!=null) {
            // polyglot HTML5 requires both
            hnode.setAttribute("xml:lang",info.sLang);
            hnode.setAttribute("lang",info.sLang);
        }
        if (info.sDir!=null) {
            hnode.setAttribute("dir",info.sDir);
        }
    }
    
    protected String scale(String s) {
    	switch (nUnits) {
    	case XhtmlConfig.PX:
            return Calc.length2px(Calc.multiply(sScale,s));
    	case XhtmlConfig.REM:
            return Calc.length2rem(Calc.multiply(sScale,s));
    	case XhtmlConfig.ORIGINAL:
    	default:
            return Calc.multiply(sScale,s);
        }
    }
	
    protected String colScale(String s) {
        return scale(Calc.multiply(sColScale,s));
    }
	

}
