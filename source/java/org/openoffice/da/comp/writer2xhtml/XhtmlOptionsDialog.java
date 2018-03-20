/************************************************************************
 *
 *  XhtmlOptionsDialog.java
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
 *  Version 2.0 (2018-03-18)
 *
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.filter.OptionsDialogBase;

/** This class provides a uno component which implements a filter ui for the
 *  HTML5 export
 */
public class XhtmlOptionsDialog extends OptionsDialogBase {
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2xhtml.XhtmlOptionsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2xhtml.XhtmlOptionsDialog";
	
    public String getDialogLibraryName() { return "W2XDialogs"; }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "XhtmlOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2xhtml.Options/XhtmlOptions";
    }
	
    /** Create a new XhtmlOptionsDialog */
    public XhtmlOptionsDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2XRegistration.xMultiServiceFactory;
    }
	
    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // General
        loadConfig(xProps);
        int nScaling = loadNumericOption(xProps, "Scaling");
        if (nScaling<=1) { // Workaround for an obscure bug in the extension manager
        	setNumericFieldValue("Scaling",100);
        }
        loadCheckBoxOption(xProps, "ConvertToPx");
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
        loadCheckBoxOption(xProps, "OriginalImageSize");
        loadCheckBoxOption(xProps, "EmbedSVG");
        loadCheckBoxOption(xProps, "EmbedImg");
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
    protected void saveSettings(XPropertySet xProps, PropertyHelper helper) {
        // General
        saveConfig(xProps, helper);
        saveNumericOptionAsPercentage(xProps, helper, "Scaling", "scaling");
        saveCheckBoxOption(xProps, helper, "ConvertToPx", "convert_to_px");
        saveCheckBoxOption(xProps, helper, "Multilingual", "multilingual");
        
        // Files
        boolean bSplit = saveCheckBoxOption(xProps, "Split");
        short nSplitLevel = saveListBoxOption(xProps, "SplitLevel");
        short nRepeatLevels = saveListBoxOption(xProps, "RepeatLevels");
        if (!isLocked("split_level")) {
            if (bSplit) {
               helper.put("split_level",Integer.toString(nSplitLevel+1));
               helper.put("repeat_levels",Integer.toString(nRepeatLevels));
            }
            else {
                helper.put("split_level","0");
            }
        }    		
        saveCheckBoxOption(xProps, helper, "SaveImagesInSubdir", "save_images_in_subdir");

        // Special content
        saveCheckBoxOption(xProps, helper, "Notes", "notes");
        saveCheckBoxOption(xProps, helper, "UseDublinCore", "use_dublin_core");
  		
        // Figures, tables and formulas
        saveCheckBoxOption(xProps, "OriginalImageSize");
        // TODO: Support "relative"
        helper.put("image_size", getCheckBoxStateAsBoolean("OriginalImageSize") ? "none" : "absolute");
        saveCheckBoxOption(xProps, helper, "EmbedSVG","embed_svg");
        saveCheckBoxOption(xProps, helper, "EmbedImg","embed_img");
        saveNumericOptionAsPercentage(xProps, helper, "ColumnScaling", "column_scaling");
        saveCheckBoxOption(xProps, helper, "UseMathjax", "use_mathjax");

        // AutoCorrect
        saveCheckBoxOption(xProps, helper, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, helper, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, helper, "IgnoreDoubleSpaces", "ignore_double_spaces");
    }
	
	
    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange")) {
            updateLockedOptions();
            enableControls();
        }
        else if (sMethod.equals("SplitChange")) {
            enableSplitLevel();
        }
        return true;
    }

    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "SplitChange" };
        return sNames;
    }
	
    private void enableControls() {
        // General
        setControlEnabled("ScalingLabel",!isLocked("scaling"));
        setControlEnabled("Scaling",!isLocked("scaling"));
        setControlEnabled("ConvertToPx",!isLocked("convert_to_px"));
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
        setControlEnabled("OriginalImageSize",!isLocked("image_size") && !isLocked("original_image_size"));
        setControlEnabled("EmbedSVG",!isLocked("embed_svg"));
        setControlEnabled("EmbedImg",!isLocked("embed_img"));
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
