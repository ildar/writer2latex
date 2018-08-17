/************************************************************************
 *
 *  Html5FilterDialog.java
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
 *  Version 2.0 (2018-08-17)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.html5;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.writer2latex.W2LRegistration;
import org.openoffice.da.comp.writer2latex.base.FilterDialogBase;
import org.openoffice.da.comp.writer2latex.util.PropertyHelper;

import writer2latex.api.MIMETypes;

/** This class provides a uno component which implements a filter ui for the
 *  HTML5 export
 */
public class Html5FilterDialog extends FilterDialogBase {
	
    // Translate list box items to configuration option values 
    private static final String[] SIZE_VALUES =
        { "auto", "relative", "none" };
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.Html5FilterDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = Html5FilterDialog.class.getName();
	
    public String getDialogLibraryName() { return "W2HDialogs"; }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2LaTeX.Options/XhtmlOptions";
    }

    protected String getMIME() {
    	return MIMETypes.HTML5;
    }

    /** Create a new XhtmlOptionsDialog */
    public Html5FilterDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2LRegistration.xMultiServiceFactory;
    }
	
    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // General
        loadListBoxOption(xProps, "Units");
        int nScaling = loadNumericOption(xProps, "Scaling");
        if (nScaling<=1) { // Workaround for an obscure bug in the extension manager
        	setNumericFieldValue("Scaling",100);
        }
        loadCheckBoxOption(xProps, "Multilingual");

        // Files
        loadCheckBoxOption(xProps, "Split");
        loadListBoxOption(xProps, "SplitLevel");
        loadListBoxOption(xProps, "RepeatLevels");
        loadCheckBoxOption(xProps, "SaveImagesInSubdir");
        
        // Special content
        loadCheckBoxOption(xProps, "Notes");
        loadCheckBoxOption(xProps, "UseDublinCore");
			
        // Figures, tables and formulas
        loadListBoxOption(xProps, "ImageSize");
        loadCheckBoxOption(xProps, "EmbedSVG");
        loadCheckBoxOption(xProps, "EmbedImg");
        loadListBoxOption(xProps, "TableSize");
        int nColumnScaling = loadNumericOption(xProps, "ColumnScaling");
        if (nColumnScaling<=1) {
        	setNumericFieldValue("ColumnScaling",100);
        }
        loadCheckBoxOption(xProps, "UseMathjax");
		
        // AutoCorrect
        loadCheckBoxOption(xProps, "IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps, "IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps, "IgnoreDoubleSpaces");
        
        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper filterData) {
        // General
        short nUnits = saveListBoxOption(xProps, "Units");
        if (!isLocked("units")) {
	    	switch (nUnits) {
	    	case 0: filterData.put("units", "original"); break;
	    	case 1: filterData.put("units", "px"); break;
	    	case 2:
	    	default: filterData.put("units", "rem");
	    	}
        }
        saveNumericOptionAsPercentage(xProps, filterData, "Scaling", "scaling");
        saveCheckBoxOption(xProps, filterData, "Multilingual", "multilingual");
        
        // Files
        boolean bSplit = saveCheckBoxOption(xProps, "Split");
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        short nRepeatLevels = saveListBoxOption(xProps, "RepeatLevels");
        if (!isLocked("split_level")) {
            if (bSplit) {
               filterData.put("split_level",Integer.toString(nSplitLevel+1));
               filterData.put("repeat_levels",Integer.toString(nRepeatLevels));
            }
            else {
                filterData.put("split_level","0");
            }
        }    		
        saveCheckBoxOption(xProps, filterData, "SaveImagesInSubdir", "save_images_in_subdir");

        // Special content
        saveCheckBoxOption(xProps, filterData, "Notes", "notes");
        saveCheckBoxOption(xProps, filterData, "UseDublinCore", "use_dublin_core");
  		
        // Figures, tables and formulas
        saveListBoxOption(xProps, filterData, "ImageSize", "image_size", SIZE_VALUES);
        saveCheckBoxOption(xProps, filterData, "EmbedSVG","embed_svg");
        saveCheckBoxOption(xProps, filterData, "EmbedImg","embed_img");
        saveListBoxOption(xProps, filterData, "TableSize", "table_size", SIZE_VALUES);
        saveNumericOptionAsPercentage(xProps, filterData, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, filterData, "UseMathjax", "use_mathjax");

        // AutoCorrect
        saveCheckBoxOption(xProps, filterData, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, filterData, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, filterData, "IgnoreDoubleSpaces", "ignore_double_spaces");
    }
	
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
        	updateParameters();
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("ParameterNameChange")) {
        	parameterNameChange();
        }
        else if (sMethod.equals("ParameterValueChange")) {
        	parameterValueChange();
        }
        else if (sMethod.equals("SplitChange")) {
            enableSplitLevel();
        }
        return true;
    }

    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "ParameterNameChange", "ParameterValueChange", "SplitChange" };
        return sNames;
    }
	
    private void enableControls() {
        // General
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Units",!isLocked("units"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("Multilingual",!isLocked("multilingual"));

        // Files
        boolean bSplit = getCheckBoxStateAsBoolean("Split");
        setControlEnabled("Split",!isLocked("split_level"));
        setControlEnabled("SplitLevelLabel",!isLocked("split_level") && bSplit);
        setControlEnabled("SplitLevel",!isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevelsLabel",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("RepeatLevels",!isLocked("repeat_levels") && !isLocked("split_level") && bSplit);
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));
        
        // Special content
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("UseDublinCore",!isLocked("use_dublin_core"));

        // Figures, tables and formulas
        setControlEnabled("ImageSize",!isLocked("image_size") && !isLocked("original_image_size"));
        setControlEnabled("EmbedSVG",!isLocked("embed_svg"));
        setControlEnabled("EmbedImg",!isLocked("embed_img"));
        setControlEnabled("TableSize",!isLocked("table_size"));
        setControlEnabled("ColumnScalingLabel",!isLocked("column_scaling"));
        setControlEnabled("ColumnScaling",!isLocked("column_scaling"));
        setControlEnabled("UseMathjax",!isLocked("use_mathjax"));

        // AutoCorrect
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));
    }
	
    private void enableSplitLevel() {
        if (!isLocked("split_level")) {
            boolean bState = getCheckBoxStateAsBoolean("Split");
            setControlEnabled("SplitLevelLabel",bState);
            setControlEnabled("SplitLevel",bState);
            if (!isLocked("repeat_levels")) {
                setControlEnabled("RepeatLevelsLabel",bState);
                setControlEnabled("RepeatLevels",bState);
            }
        }
    }

	
}
