/************************************************************************
 *
 *  LaTeXFilterDialog.java
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
 *  Version 2.0 (2022-05-06)
 *  
 */

package org.openoffice.da.comp.writer2latex.latex;

import com.sun.star.awt.XDialog;
import com.sun.star.beans.XPropertySet;
import com.sun.star.uno.XComponentContext;

import org.openoffice.da.comp.writer2latex.W2LRegistration;
import org.openoffice.da.comp.writer2latex.base.FilterDialogBase;
import org.openoffice.da.comp.writer2latex.util.PropertyHelper;

import writer2latex.api.MIMETypes;

/** This class provides a UNO component which implements a filter ui for the
 *  LaTeX export
 */
public class LaTeXFilterDialog extends FilterDialogBase {

    // Translate list box items to configuration option values 
    private static final String[] BACKEND_VALUES =
        { "generic", "pdftex", "dvips", "xetex", "unspecified" };
    private static final String[] INPUTENCODING_VALUES =
        { "ascii", "latin1", "latin2", "iso-8859-7", "cp1250", "cp1251", "koi8-r", "utf8" };
    private static final String[] SCRIPT_VALUES =
        { "western", "ctl", "cjk" };
    private static final String[] NOTES_VALUES =
        { "ignore", "comment", "marginpar", "pdfannotation" };
    private static final String[] FLOATOPTIONS_VALUES =
        { "", "tp", "bp", "htp", "hbp" };
    
    // UI names and configuration option values for fonts
    private static final String[] FONT_VALUES = 
    	{ "default", "cmbright", "ccfonts", "ccfonts-euler",
    		"iwona", "kurier", "anttor", "kmath-kerkis",
    		"fouriernc",
    		"pxfonts", "mathpazo", "mathpple",
    		"txfonts", "mathptmx",
    		"arev",
    		"charter-mathdesign", "utopia-mathdesign", "fourier" };
    private static final String[] FONT_NAMES = 
    	{ "Default (Computer Modern)", "CM Bright", "Concrete", "Concrete + Euler Math",
    		"Iwona", "Kurier", "Antykwa Toru\u0144ska", "Kerkis",
    		"New Century Schoolbook + Fourier Math",
    		"Palatino + PXfonts Math", "Palatino + Pazo Math", "Palatino + Euler Math",
    		"Times + TXfonts Math", "Times + Symbol",
    		"Arev Sans + Arev Math",
    		"Bitstream Charter + Math Design", "Utopia + Math Design", "Utopia + Fourier Math" };
    private static final String[] FONTSPEC_VALUES =
    	{ "original", "original+math", "default" };
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.LaTeXFilterDialog";

    /** The component should also have an implementation name.
     *  The subclass should override this with a suitable name
     */
    public static String __implementationName = LaTeXFilterDialog.class.getName();

    public String getDialogLibraryName() { return "W2LDialogs"; }
	

    /** Create a new LaTeXOptionsDialog */
    public LaTeXFilterDialog(XComponentContext xContext) {
        super(xContext);
        xMSF = W2LRegistration.xMultiServiceFactory;
    }
	
    /** Return the name of the dialog within the library
     */
    public String getDialogName() { return "LaTeXOptions"; }

    /** Return the name of the registry path
     */
    public String getRegistryPath() {
        return "/org.openoffice.da.Writer2LaTeX.Options/LaTeXOptions";
    }
    
    protected String getMIME() {
    	return MIMETypes.LATEX;
    }

    /** Load settings from the registry to the dialog */
    protected void loadSettings(XPropertySet xProps) {
        // General
        loadListBoxOption(xProps,"Backend");
        loadListBoxOption(xProps,"Inputencoding");
        loadListBoxOption(xProps,"Script");
        loadCheckBoxOption(xProps,"Multilingual");
        setListBoxStringItemList("Font", FONT_NAMES);
        loadListBoxOption(xProps,"Font");
        loadListBoxOption(xProps,"Fontspec");
        loadCheckBoxOption(xProps,"GreekMath");
		
        // Bibliography
        loadCheckBoxOption(xProps,"UseBibtex");
        loadComboBoxOption(xProps,"BibtexStyle");
		
        // Files
        loadCheckBoxOption(xProps,"WrapLines");
        loadNumericOption(xProps,"WrapLinesAfter");
        loadCheckBoxOption(xProps,"SplitLinkedSections");
        loadCheckBoxOption(xProps,"SplitToplevelSections");
        loadCheckBoxOption(xProps,"SaveImagesInSubdir");
		
        // Special content
        loadListBoxOption(xProps,"Notes");
        loadCheckBoxOption(xProps,"Metadata");
        loadCheckBoxOption(xProps,"DisplayHiddenText");
		
        // Figures, tables and indexes
        loadCheckBoxOption(xProps,"OriginalImageSize");
        loadCheckBoxOption(xProps,"OptimizeSimpleTables");
        loadNumericOption(xProps,"SimpleTableLimit");
        loadCheckBoxOption(xProps,"FloatTables");
        loadCheckBoxOption(xProps,"FloatFigures");
        loadListBoxOption(xProps,"FloatOptions");
        loadCheckBoxOption(xProps,"ConvertIndexNames");

        // AutoCorrect
        loadCheckBoxOption(xProps,"IgnoreHardPageBreaks");
        loadCheckBoxOption(xProps,"IgnoreHardLineBreaks");
        loadCheckBoxOption(xProps,"IgnoreEmptyParagraphs");
        loadCheckBoxOption(xProps,"IgnoreDoubleSpaces");
		
        updateLockedOptions();
        enableControls();
    }
	
    /** Save settings from the dialog to the registry and create FilterData */
    protected void saveSettings(XPropertySet xProps, PropertyHelper filterData) {
        // General
        saveListBoxOption(xProps, filterData, "Backend", "backend", BACKEND_VALUES );
        saveListBoxOption(xProps, filterData, "Inputencoding", "inputencoding", INPUTENCODING_VALUES);
        saveListBoxOption(xProps, filterData, "Script", "script", SCRIPT_VALUES);
        saveCheckBoxOption(xProps, filterData, "Multilingual", "multilingual");
        saveListBoxOption(xProps, filterData, "Font", "font", FONT_VALUES);
        saveListBoxOption(xProps, filterData, "Fontspec", "fontspec", FONTSPEC_VALUES);
        saveCheckBoxOption(xProps, filterData, "GreekMath", "greek_math");
		
        // Bibliography
        saveCheckBoxOption(xProps, filterData, "UseBibtex", "use_bibtex");
        saveComboBoxOption(xProps, filterData, "BibtexStyle", "bibtex_style");
		
        // Files
        boolean bWrapLines = saveCheckBoxOption(xProps, "WrapLines");
        int nWrapLinesAfter = saveNumericOption(xProps, "WrapLinesAfter");
        if (!isLocked("wrap_lines_after")) {
            if (bWrapLines) {
                filterData.put("wrap_lines_after",Integer.toString(nWrapLinesAfter));
            }
            else {
                filterData.put("wrap_lines_after","0");
            }
        }
		
        saveCheckBoxOption(xProps, filterData, "SplitLinkedSections", "split_linked_sections");
        saveCheckBoxOption(xProps, filterData, "SplitToplevelSections", "split_toplevel_sections");
        saveCheckBoxOption(xProps, filterData, "SaveImagesInSubdir", "save_images_in_subdir");
		
        // Special content
        saveListBoxOption(xProps, filterData, "Notes", "notes", NOTES_VALUES);
        saveCheckBoxOption(xProps, filterData, "Metadata", "metadata");
        saveCheckBoxOption(xProps, filterData, "DisplayHiddenText", "display_hidden_text");

        // Figures and tables
        saveCheckBoxOption(xProps, filterData, "OriginalImageSize", "original_image_size");
		
        boolean bOptimizeSimpleTables = saveCheckBoxOption(xProps,"OptimizeSimpleTables");
        int nSimpleTableLimit = saveNumericOption(xProps,"SimpleTableLimit");
        if (!isLocked("simple_table_limit")) {
            if (bOptimizeSimpleTables) {
                filterData.put("simple_table_limit",Integer.toString(nSimpleTableLimit));
            }
            else {
                filterData.put("simple_table_limit","0");
            }
        }

        saveCheckBoxOption(xProps, filterData, "FloatTables", "float_tables");
        saveCheckBoxOption(xProps, filterData, "FloatFigures", "float_figures");
        saveListBoxOption(xProps, filterData, "FloatOptions", "float_options", FLOATOPTIONS_VALUES);
        saveCheckBoxOption(xProps, filterData, "ConvertIndexNames", "convert_index_names");

        // AutoCorrect
        saveCheckBoxOption(xProps, filterData, "IgnoreHardPageBreaks", "ignore_hard_page_breaks");
        saveCheckBoxOption(xProps, filterData, "IgnoreHardLineBreaks", "ignore_hard_line_breaks");
        saveCheckBoxOption(xProps, filterData, "IgnoreEmptyParagraphs", "ignore_empty_paragraphs");
        saveCheckBoxOption(xProps, filterData, "IgnoreDoubleSpaces", "ignore_double_spaces");
    }
	

    // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
        if (sMethod.equals("ConfigChange") || sMethod.equals("BackendChange")) {
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
        else if (sMethod.equals("ScriptChange")) {
        	enableMultilingual();
        }
        else if (sMethod.equals("UseBibtexChange")) {
            enableBibtexStyle();
        }
        else if (sMethod.equals("WrapLinesChange")) {
            enableWrapLinesAfter();
        }
        else if (sMethod.equals("OptimizeSimpleTablesChange")) {
            enableSimpleTableLimit();
        }
        else if (sMethod.equals("FloatTablesChange")) {
            enableFloatOptions();
        }
        else if (sMethod.equals("FloatFiguresChange")) {
            enableFloatOptions();
        }
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "ConfigChange", "ParameterNameChange", "ParameterValueChange",
        		"ScriptChange", "UseBibtexChange", "WrapLinesChange",
        		"OptimizeSimpleTablesChange", "FloatTablesChange", "FloatFiguresChange" };
        return sNames;
    }
	
    protected boolean isLocked(String sOptionName) {
        if ("inputencoding".equals(sOptionName)) {
        	// backend=xetex locks the encoding to utf8
        	return getListBoxSelectedItem("Backend")==3 || super.isLocked(sOptionName);
        }
        else if ("script".equals(sOptionName)) {
        	// backend!=xetex locks the script to western
        	return getListBoxSelectedItem("Backend")!=3 || super.isLocked(sOptionName);
        }
        else if ("font".equals(sOptionName)) {
        	// backend=xetex does not (currently) use the font option
        	return getListBoxSelectedItem("Backend")==3 || super.isLocked(sOptionName);
        }
        else if ("fontspec".equals(sOptionName)) {
        	return super.isLocked(sOptionName);
        }
        else if ("greek_math".equals(sOptionName)) {
        	return super.isLocked(sOptionName);
        }
        else {
            return super.isLocked(sOptionName); 
        }
    }
	
    private void enableControls() {
        // General
        setControlEnabled("BackendLabel",!isLocked("backend"));
        setControlEnabled("Backend",!isLocked("backend"));
        setControlEnabled("InputencodingLabel",!isLocked("inputencoding"));
        setControlEnabled("Inputencoding",!isLocked("inputencoding"));
        setControlEnabled("ScriptLabel",!isLocked("script"));
        setControlEnabled("Script",!isLocked("script"));
        setControlEnabled("Multilingual",!isLocked("multilingual") && getListBoxSelectedItem("Script")==0);
        setControlEnabled("FontLabel",!isLocked("font") || !isLocked("fontspec"));
        setControlEnabled("Font",!isLocked("font"));
        setControlEnabled("Fontspec",!isLocked("fontspec"));
        setControlEnabled("GreekMath",!isLocked("greek_math"));

        // Bibliography
        setControlEnabled("UseBibtex",!isLocked("use_bibtex"));
        boolean bUseBibtex = getCheckBoxStateAsBoolean("UseBibtex");
        setControlEnabled("BibtexStyleLabel",!isLocked("bibtex_style") && bUseBibtex);
        setControlEnabled("BibtexStyle",!isLocked("bibtex_style") && bUseBibtex);

        // Files
        setControlEnabled("WrapLines",!isLocked("wrap_lines_after"));
        boolean bWrapLines = getCheckBoxStateAsBoolean("WrapLines");
        setControlEnabled("WrapLinesAfterLabel",!isLocked("wrap_lines_after") && bWrapLines);
        setControlEnabled("WrapLinesAfter",!isLocked("wrap_lines_after") && bWrapLines);
        setControlEnabled("SplitLinkedSections",!isLocked("split_linked_sections"));
        setControlEnabled("SplitToplevelSections",!isLocked("split_toplevel_sections"));
        setControlEnabled("SaveImagesInSubdir",!isLocked("save_images_in_subdir"));

        // Special content
        setControlEnabled("NotesLabel",!isLocked("notes"));
        setControlEnabled("Notes",!isLocked("notes"));
        setControlEnabled("Metadata",!isLocked("metadata"));
        setControlEnabled("DisplayHiddenText",!isLocked("display_hidden_text"));

        // Figures, tables and indexes
        setControlEnabled("OriginalImageSize",!isLocked("original_image_size"));
        setControlEnabled("OptimizeSimpleTables",!isLocked("simple_table_limit"));
        boolean bOptimizeSimpleTables = getCheckBoxStateAsBoolean("OptimizeSimpleTables");
        setControlEnabled("SimpleTableLimitLabel",!isLocked("simple_table_limit") && bOptimizeSimpleTables);
        setControlEnabled("SimpleTableLimit",!isLocked("simple_table_limit") && bOptimizeSimpleTables);
        setControlEnabled("FloatTables",!isLocked("float_tables"));
        setControlEnabled("FloatFigures",!isLocked("float_figures"));
        boolean bFloat = getCheckBoxStateAsBoolean("FloatFigures") ||
            getCheckBoxStateAsBoolean("FloatTables");
        setControlEnabled("FloatOptionsLabel",!isLocked("float_options") && bFloat);
        setControlEnabled("FloatOptions",!isLocked("float_options") && bFloat);
        setControlEnabled("ConvertIndexNames",!isLocked("convert_index_names"));

        // AutoCorrect
        setControlEnabled("IgnoreHardPageBreaks",!isLocked("ignore_hard_page_breaks"));
        setControlEnabled("IgnoreHardLineBreaks",!isLocked("ignore_hard_line_breaks"));
        setControlEnabled("IgnoreEmptyParagraphs",!isLocked("ignore_empty_paragraphs"));
        setControlEnabled("IgnoreDoubleSpaces",!isLocked("ignore_double_spaces"));
        
        // Visibility of controls
        boolean bXeTeX = getListBoxSelectedItem("Backend")==3; 
		setControlVisible("InputencodingLabel",!bXeTeX);
		setControlVisible("Inputencoding",!bXeTeX);
		setControlVisible("Font",!bXeTeX);
		setControlVisible("ScriptLabel",bXeTeX);
		setControlVisible("Script",bXeTeX);
		setControlVisible("Fontspec",bXeTeX);
    }
    
    private void enableMultilingual() {
    	if (!isLocked("multilingual")) {
    		// Currently multilingual text is only support if the main script is western
    		setControlEnabled("Multilingual",getListBoxSelectedItem("Script")==0);
    	}
    }
	
    private void enableBibtexStyle() {
        if (!isLocked("bibtex_style")) {
            boolean bState = getCheckBoxStateAsBoolean("UseBibtex");
            setControlEnabled("BibtexStyleLabel",bState);
            setControlEnabled("BibtexStyle",bState);
        }
    }

    private void enableWrapLinesAfter() {
        if (!isLocked("wrap_lines_after")) {
            boolean bState = getCheckBoxStateAsBoolean("WrapLines");
            setControlEnabled("WrapLinesAfterLabel",bState);
            setControlEnabled("WrapLinesAfter",bState);
        }
    }

    private void enableSimpleTableLimit() {
        if (!isLocked("simple_table_limit")) {
            boolean bState = getCheckBoxStateAsBoolean("OptimizeSimpleTables");
            setControlEnabled("SimpleTableLimitLabel",bState);
            setControlEnabled("SimpleTableLimit",bState);
        }
    }

    private void enableFloatOptions() {
        if (!isLocked("float_options")) {
            boolean bState = getCheckBoxStateAsBoolean("FloatFigures") ||
                getCheckBoxStateAsBoolean("FloatTables");
            setControlEnabled("FloatOptionsLabel",bState);
            setControlEnabled("FloatOptions",bState);
        }
    }
	
}



