/************************************************************************
 *
 *  StyleWithProperties.java
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
 *  Version 2.0 (2018-06-25)
 */
 
package writer2latex.office;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Node;

import writer2latex.util.Calc;
import writer2latex.util.Misc;

/** <p> Class representing an ODF style which contains a style:properties element </p> 
  */
public class StyleWithProperties extends OfficeStyle {
    public final static int TEXT = 0;
    public final static int PAR = 1;
    public final static int SECTION = 2;
    public final static int TABLE = 3;
    public final static int COLUMN = 4;
    public final static int ROW = 5;
    public final static int CELL = 6;
    public final static int GRAPHIC = 7;
    public final static int PAGE = 8;
    private final static int COUNT = 9;
    
    // Map LO specific text properties to (proposed) ODF properties
    private static Map<String,String> loextMap;
    static {
    	loextMap = new HashMap<>();
    	loextMap.put(XMLString.LOEXT_BORDER,XMLString.FO_BORDER);
    	loextMap.put(XMLString.LOEXT_BORDER_LEFT,XMLString.FO_BORDER_LEFT);
    	loextMap.put(XMLString.LOEXT_BORDER_RIGHT,XMLString.FO_BORDER_RIGHT);
    	loextMap.put(XMLString.LOEXT_BORDER_TOP,XMLString.FO_BORDER_TOP);
    	loextMap.put(XMLString.LOEXT_BORDER_BOTTOM,XMLString.FO_BORDER_BOTTOM);
    	loextMap.put(XMLString.LOEXT_PADDING,XMLString.FO_PADDING);
    	loextMap.put(XMLString.LOEXT_PADDING_LEFT,XMLString.FO_PADDING_LEFT);
    	loextMap.put(XMLString.LOEXT_PADDING_RIGHT,XMLString.FO_PADDING_RIGHT);
    	loextMap.put(XMLString.LOEXT_PADDING_TOP,XMLString.FO_PADDING_TOP);
    	loextMap.put(XMLString.LOEXT_PADDING_BOTTOM,XMLString.FO_PADDING_BOTTOM);
    	loextMap.put(XMLString.LOEXT_SHADOW,XMLString.STYLE_SHADOW);
    }
	
    private PropertySet[] properties = new PropertySet[COUNT];

    private PropertySet backgroundImageProperties = new PropertySet();

    private int nColCount = 0;
    private PropertySet columnSepProperties = new PropertySet();
    private PropertySet[] columnProperties;
    
    private boolean bHasFootnoteSep = false;
    private PropertySet footnoteSep = new PropertySet();
    
    public StyleWithProperties() {
        for (int i=0; i<COUNT; i++) {
            properties[i] = new PropertySet();
        }
    }

    public void loadStyleFromDOM(Node node) {
        super.loadStyleFromDOM(node);
        // read the properties of the style, if any
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {
                String sName = child.getNodeName();
                if (XMLString.STYLE_TEXT_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(TEXT,child);
                }
                else if (XMLString.STYLE_PARAGRAPH_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAR,child);
                }
                else if (XMLString.STYLE_SECTION_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(SECTION,child);
                }
                else if (XMLString.STYLE_TABLE_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(TABLE,child);
                }
                else if (XMLString.STYLE_TABLE_COLUMN_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(COLUMN,child);
                }
                else if (XMLString.STYLE_TABLE_ROW_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(ROW,child);
                }
                else if (XMLString.STYLE_TABLE_CELL_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(CELL,child);
                }
                else if (XMLString.STYLE_GRAPHIC_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(GRAPHIC,child);
                }
                else if (XMLString.STYLE_PAGE_LAYOUT_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAGE,child);
                }
                else if (XMLString.STYLE_DRAWING_PAGE_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(PAGE,child);
                }
                // LO specifies graphic properties on paragraph styles with a private name space
                // This is mapped to the standard graphic properties
                else if (XMLString.LOEXT_GRAPHIC_PROPERTIES.equals(sName)) {
                    loadPropertiesFromDOM(GRAPHIC,child);
                }
            }
            child = child.getNextSibling();
        }
    }
	
    private void loadPropertiesFromDOM(int nIndex,Node node) {
        properties[nIndex].loadFromDOM(node);
        // Handle LO specific text properties
        if (nIndex==TEXT) {
	        for (String sLoext : loextMap.keySet()) {
	        	if (properties[nIndex].containsProperty(sLoext)) {
	        		properties[nIndex].setProperty(loextMap.get(sLoext), properties[nIndex].getProperty(sLoext));
	        	}
	        }
        }
        // Several property sets may contain these complex properties, but only one per style:
        Node child = node.getFirstChild();
        while (child!=null) {
            if (child.getNodeType()==Node.ELEMENT_NODE) {        
                String sName = child.getNodeName();
                if (XMLString.STYLE_BACKGROUND_IMAGE.equals(sName)) {    
                    backgroundImageProperties.loadFromDOM(child);
                }
                else if (XMLString.STYLE_COLUMNS.equals(sName)) {    
                    nColCount = Misc.getPosInteger(Misc.getAttribute(child,
                                XMLString.FO_COLUMN_COUNT),1);
                    columnProperties = new PropertySet[nColCount];
                    for (int i=0; i<nColCount; i++) { columnProperties[i]=new PropertySet(); }
                    Node grandChild = child.getFirstChild();
                    int i=0;
                    while (grandChild!=null) {
                    	String sName2 = grandChild.getNodeName();
                    	if (XMLString.STYLE_COLUMN_SEP.equals(sName2)) {
                    		columnSepProperties.loadFromDOM(grandChild);
                    	}
                    	else if (XMLString.STYLE_COLUMN.equals(sName2) && i<nColCount) {
                    		columnProperties[i++].loadFromDOM(grandChild); 
                    	}
                    	grandChild = grandChild.getNextSibling();
                    }
                }
                else if (XMLString.STYLE_FOOTNOTE_SEP.equals(sName)) {
                    bHasFootnoteSep = true; 
                    footnoteSep.loadFromDOM(child);
                }
            }
            child = child.getNextSibling();
        }
    }
	
    /** Get a property value
     * 
     * @param nIndex the property type
     * @param sName the name of the property
     * @param bInherit true if the value can be inherited from parent style
     * @return the value, or null if the property is not set
     */
    public String getProperty(int nIndex, String sName, boolean bInherit) {
    	if (0<=nIndex && nIndex<=COUNT) {
	        if (properties[nIndex].containsProperty(sName)) {
	            String sValue = properties[nIndex].getProperty(sName);
	            return Calc.truncateLength(sValue);
	        }
	        else if (bInherit && getParentName()!=null) {
	            StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
	            if (parentStyle!=null) {
	                return parentStyle.getProperty(nIndex,sName,bInherit);
	            }
	        }
    	}
        return null; // no value
    }
	
    public String getTextProperty(String sName, boolean bInherit) {
        return getProperty(TEXT,sName,bInherit);
    }

    public String getParProperty(String sName, boolean bInherit) {
        return getProperty(PAR,sName,bInherit);
    }

    public String getSectionProperty(String sName, boolean bInherit) {
        return getProperty(SECTION,sName,bInherit);
    }

    public String getTableProperty(String sName, boolean bInherit) {
        return getProperty(TABLE,sName,bInherit);
    }

    public String getColumnProperty(String sName, boolean bInherit) {
        return getProperty(COLUMN,sName,bInherit);
    }

    public String getRowProperty(String sName, boolean bInherit) {
        return getProperty(ROW,sName,bInherit);
    }

    public String getCellProperty(String sName, boolean bInherit) {
        return getProperty(CELL,sName,bInherit);
    }

    public String getGraphicProperty(String sName, boolean bInherit) {
        return getProperty(GRAPHIC,sName,bInherit);
    }
    
    /** Get a property value which can be set as either of two names
     * 
     * @param nIndex the property type
     * @param sName1 the name of the first possible property (this has first priority)
     * @param sName2 the name of the alternative possible property
     * @param bInherit true if the value can be inherited from parent style
     * @return the value, or null if neither of the properties are set
     */
    public String getAlternativeProperty(int nIndex, String sName1, String sName2, boolean bInherit) {
    	if (0<=nIndex && nIndex<=COUNT) {
	        if (properties[nIndex].containsProperty(sName1)) {
	            return properties[nIndex].getProperty(sName1);
	        }
	        else if (properties[nIndex].containsProperty(sName2)) {
	            return properties[nIndex].getProperty(sName2);
	        }
	        else if (bInherit && getParentName()!=null) {
	            StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
	            if (parentStyle!=null) {
	                return parentStyle.getAlternativeProperty(nIndex,sName1,sName2,bInherit);
	            }
	        }
    	}
        return null; // no value    	
    }

    // TODO: Remove this method
    public String getProperty(String sProperty, boolean bInherit){
        String sValue;
        for (int i=0; i<COUNT; i++) {
            sValue = getProperty(i,sProperty,bInherit);
            if (sValue!=null) { return sValue; }
        }
        return null; // no value
    }

    // TODO: Remove this method
    public String getProperty(String sProperty){
        return getProperty(sProperty,true); 
    }
	
    /** Get the value of a property which can be either relative (%) or absolute. If the value
     *  is relative, it is resolved to an absolute value using the parent style(s).
     * 
     * @param nIndex the property type
     * @param sProperty the name of the property
     * @return the absolute value, or null if the property is not set
     */
    public String getAbsoluteProperty(int nIndex, String sProperty){
    	if (0<=nIndex && nIndex<=COUNT) {
	        if (properties[nIndex].containsProperty(sProperty)){
	            String sValue=(String) properties[nIndex].getProperty(sProperty);
	            if (sValue.endsWith("%")) {
	                StyleWithProperties parentStyle 
		                = (StyleWithProperties) family.getStyle(getParentName());
	                if (parentStyle!=null) {
	                    String sParentValue = parentStyle.getAbsoluteProperty(nIndex,sProperty);
	                    if (sParentValue!=null) { return Calc.multiply(sValue,sParentValue); }
	                }
	                else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
	                    StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
	                    String sDefaultValue=(String) style.getProperty(nIndex,sProperty,false);
	                    if (sValue !=null) { return Calc.multiply(sValue,sDefaultValue); }
	                }
	            }
	            else {
	                return Calc.truncateLength(sValue);
	            }
	        }
	        else if (getParentName()!=null){
	            StyleWithProperties parentStyle 
	                = (StyleWithProperties) family.getStyle(getParentName());
	            if (parentStyle!=null) {
	                return parentStyle.getAbsoluteProperty(nIndex,sProperty);
	            }
	        }
	        else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
	            StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
	            String sValue=(String) style.getProperty(nIndex,sProperty,false);
	            if (sValue !=null) { return sValue; }
	        }
    	}
        // no value!
        return null;
    }
	
    public String getAbsoluteTextProperty(String sName) {
        return getAbsoluteProperty(TEXT,sName);
    }
	
    public String getAbsoluteParProperty(String sName) {
        return getAbsoluteProperty(PAR,sName);
    }
	
    public String getAbsoluteSectionProperty(String sName) {
        return getAbsoluteProperty(SECTION,sName);
    }
	
    public String getAbsoluteTableProperty(String sName) {
        return getAbsoluteProperty(TABLE,sName);
    }
	
    public String getAbsoluteColumnProperty(String sName) {
        return getAbsoluteProperty(COLUMN,sName);
    }
	
    public String getAbsoluteRowProperty(String sName) {
        return getAbsoluteProperty(ROW,sName);
    }
	
    public String getAbsoluteCellProperty(String sName) {
        return getAbsoluteProperty(CELL,sName);
    }
	
    public String getAbsoluteGraphicProperty(String sName) {
        return getAbsoluteProperty(GRAPHIC,sName);
    }

    // TODO: Remove this method
    public String getAbsoluteProperty(String sProperty){
        String sValue;
        for (int i=0; i<COUNT; i++) {
            sValue = getAbsoluteProperty(i,sProperty);
            if (sValue!=null) { return sValue; }
        }
        return null; // no value
    }
    
    /** Get the font size. This is a special case which combines the two properties
     *  <code>fo:font-size</code> and <code>style:font-size-rel</code>.
     *  This method resolves the font size to an absolute size
     * 
     * @return the absolute value, or null if the property is not set
     */
    public String getAbsoluteFontSize(){
        if (properties[TEXT].containsProperty(XMLString.STYLE_FONT_SIZE_REL)) {
        	// Size specified as e.g. +3pt or -2pt
        	String sValue= properties[TEXT].getProperty(XMLString.STYLE_FONT_SIZE_REL);
        	String sParentValue = getAbsoluteParentFontSize();
       		if (sParentValue!=null) {
       			return Calc.add(Calc.truncateLength(sValue),sParentValue);
       		}
        }
        else if (properties[TEXT].containsProperty(XMLString.FO_FONT_SIZE)) {
          	String sValue=(String) properties[TEXT].getProperty(XMLString.FO_FONT_SIZE);
            if (sValue.endsWith("%")) {
            	// Size specified as a percentage
                String sParentValue = getAbsoluteParentFontSize();
                if (sParentValue!=null) {
                	return Calc.multiply(sValue,sParentValue);
                }
            }
            else {
            	// Absolute size
                return Calc.truncateLength(sValue);
            }
        }
        // If we failed, return the parent size
        return getAbsoluteParentFontSize();
    }

    private String getAbsoluteParentFontSize() {
        StyleWithProperties parentStyle = (StyleWithProperties) family.getStyle(getParentName());
        if (parentStyle!=null) {
            return parentStyle.getAbsoluteFontSize();
        }
        else if (getFamily()!=null && getFamily().getDefaultStyle()!=null) {
            StyleWithProperties style = (StyleWithProperties) getFamily().getDefaultStyle();
            return Calc.truncateLength(style.getProperty(TEXT,XMLString.FO_FONT_SIZE,false));
        }
        return null;
    }
    	
    // Get a length property that defaults to 0cm
    public String getAbsoluteLength(String sProperty) {
        String s = getAbsoluteProperty(sProperty);
        if (s==null) { return "0cm"; }
        else { return s; }
    }
	
    public String getBackgroundImageProperty(String sName) {
        return backgroundImageProperties.getProperty(sName);
    }
	
    public int getColCount() { return nColCount; }
	
    public boolean hasFootnoteSep() { return bHasFootnoteSep; }

    public String getFootnoteProperty(String sPropName) {
        return footnoteSep.getProperty(sPropName);
    }
    
    public String getColumnSepProperty(String sPropName) {
    	return columnSepProperties.getProperty(sPropName);
    }
    
    public String getColumnProperty(int nIndex, String sPropName) {
    	if (0<=nIndex && nIndex<nColCount) {
    		return columnProperties[nIndex].getProperty(sPropName);
    	}
    	return null;
    }

}