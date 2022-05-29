/************************************************************************
 *
 *  BibTeXDialog.java
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
 *  Version 2.0 (2022-05-25)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.bibtex;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.jbibtex.ParseException;
import org.openoffice.da.comp.writer2latex.Messages;
import org.openoffice.da.comp.writer2latex.util.DialogAccess;
import org.openoffice.da.comp.writer2latex.util.DialogBase;
import org.openoffice.da.comp.writer2latex.util.MessageBox;
import org.openoffice.da.comp.writer2latex.util.RegistryHelper;
import org.openoffice.da.comp.writer2latex.util.XPropertySetHelper;

import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;
import writer2latex.util.Misc;

/** This class provides a UNO dialog to insert a BibTeX bibliographic reference
 */
public class BibTeXDialog extends DialogBase implements com.sun.star.lang.XInitialization {
	private final static String W2LDIALOGSCOMMON = "W2LDialogsCommon";
	
	private final static String[] CITATION_TYPES = { "autocite", "textcite", "citeauthor", "citeauthor*", "citetitle", "citetitle*",
			"citeyear", "citedate", "citeurl", "nocite" };
	
	// **** Data used for component registration

    /** The component will be registered under this service name
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.BibTeXDialog"; 

    /** The implementation name of the component
     */
    public static String __implementationName = BibTeXDialog.class.getName();

    // **** Member variables
    
    // The Writer manager
    WriterBibTeXManager wbm = null;
    
    // The current frame (passed at initialization)
    XFrame xFrame = null;

    // The BibTeX directory (passed at initialization)
    File bibTeXDirectory = null;
    
    // The encoding for BibTeX files (set in constructor from the registry)
    String sBibTeXJavaEncoding = null;
    
    // Cache of BibTeX files in the BibTeX directory
    File[] files = null;
    
    // Cache of the current BibTeX file
    BibTeXReader currentFile = null;
    
    // Currently added sources
    List<BibMark> sources = new ArrayList<>();
    
    // **** Implement com.sun.star.lang.XInitialization
    
    // We expect to get the current frame and a comma separated list of BibTeX files to use
    public void initialize( Object[] objects )
        throws com.sun.star.uno.Exception {
        for (Object object : objects) {
            if (object instanceof String) {
                bibTeXDirectory = new File((String) object);
            }
            else { // object instanceof XFrame will not always work, but this will
            	XFrame xFrame = UnoRuntime.queryInterface(XFrame.class, object);
            	if (xFrame!=null) {
            		this.xFrame = xFrame;
            	}
            }
        }
    }
    
    // **** Extend DialogBase
    
    /** Create a new BibTeXDialog */
    public BibTeXDialog(XComponentContext xContext) {
        super(xContext);
        sBibTeXJavaEncoding = getBibTeXJavaEncoding();
    }
    
    private String getBibTeXJavaEncoding() {
    	RegistryHelper registry = new RegistryHelper(xContext);
    	try {
    		Object view = registry.getRegistryView(BibliographyDialog.REGISTRY_PATH, false);
    		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
        	int nBibTeXEncoding = XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXEncoding"); 
        	registry.disposeRegistryView(view);
        	return ClassicI18n.writeJavaEncoding(nBibTeXEncoding);
    	}
    	catch (Exception e) {
    		// Failed to get registry view
    	}
    	return null;
    }
	
    /** Return the name of the library containing the dialog
     */
    @Override public String getDialogLibraryName() {
        return "W2LDialogs"; 
    }
    
    /** Return the name of the dialog within the library
     */
    @Override public String getDialogName() {
        return "BibTeXEntry"; 
    }
	
    @Override public void initialize() {
        wbm = new WriterBibTeXManager(xFrame);
    	int nTypes = CITATION_TYPES.length;
    	String[] sTypes = new String[nTypes];
    	for (int i=0; i<nTypes; i++) {
    		sTypes[i] = Messages.getString("BibTeXDialog."+CITATION_TYPES[i]);
    	}
    	setListBoxStringItemList("Type", sTypes);
    	setListBoxLineCount("Type", (short)nTypes);
        setListBoxSelectedItem("Type",(short)0); // We don't remember the previous selection as normal is (supposedly) the most common type
        setTextFieldText("Prefix",wbm.getSelectedText()); // We cannot know why the user has selected a text, but it might be useful as a prefix
        reload(null);
    }
	
    @Override public void endDialog() {
    }

   // **** Implement XDialogEventHandler
    
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
    	if (sMethod.equals("FileChange")) { // The user has selected another BibTeX file
    		fileChange();
    	}
    	else if (sMethod.equals("EntryChange")) { // The user has selected another BibTeX entry
    		entryChange();
    	}
    	else if (sMethod.equals("New")) { // Create a new BibTeX file
    		newFile();
    	}
    	else if (sMethod.equals("Edit")) { // Edit the current BibTeX file
    		edit();
    	}
    	else if (sMethod.equals("Reload")) { // Reload the BibTeX files in the dialog
    		reload(null);
    	}
    	else if (sMethod.equals("TypeChange")) { // The user has changed the citation type
    		typeChange();
    	}
    	else if (sMethod.equals("AddSource")) { // 
    		addSource();
    	}
    	else if (sMethod.equals("RemoveSource")) { //
    		removeSource();
    	}
    	else if (sMethod.equals("InsertReference")) { // Insert a reference to the current BibTeX entry
    		insertReference();
    	}
    	else if (sMethod.equals("Update")) { // Update all reference in the document
    		Set<String> notUpdated = wbm.update(parseAllBibTeXFiles());
			// Inform the user about the result
    		MessageBox msgBox = new MessageBox(xContext);
            if (notUpdated.isEmpty()) {
    			msgBox.showMessage("BibTeX",Messages.getString("BibTeXDialog.allbibfieldsupdated")); 
            } else {
            	msgBox.showMessage("BibTeX",Messages.getString("BibTeXDialog.bibfieldsnotupdated")+":\n"+notUpdated.toString()); 
            }
    	}
        return true;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames
        	= { "FileChange", "EntryChange", "New", "Edit", "Reload", "TypeChange", "AddSource", "RemoveSource", "InsertReference", "Update" };
        return sNames;
    }
    
    // **** Implement the UI functions
    
    // Update the list of entries based on the current selection in the file list
    private void fileChange() {
    	// Remember current entry selection, if any
    	String sEntry = null;
    	short nEntry = getListBoxSelectedItem("Entry"); 
    	if (nEntry>=0) {
    		sEntry = getListBoxStringItemList("Entry")[nEntry];
    	}

    	// Parse the selected file
    	int nFile = getListBoxSelectedItem("File"); 
    	if (nFile>=0) {
    		try {
				currentFile = new BibTeXReader(files[nFile],sBibTeXJavaEncoding);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				currentFile = null;
			} catch (ParseException e) {
				System.err.println(e.getMessage());
				currentFile = null;
			}
    		
    		if (currentFile!=null) {
		    	// Populate the entry list with the keys from the current file, if any
				String[] sCurrentKeys = currentFile.getEntries().keySet().toArray(new String[0]);
				setListBoxStringItemList("Entry", sCurrentKeys); 
				if (sCurrentKeys.length>0) {
					// Select either the first or the previous entry
					nEntry = 0;
					if (sEntry!=null) {
			    		int nEntryCount = sCurrentKeys.length;
			    		for (short i=0; i<nEntryCount; i++) {
			    			if (sEntry.equals(sCurrentKeys[i])) {
			    				nEntry = i;
			    			}
			    		}
					}
					setListBoxSelectedItem("Entry",nEntry);
					entryChange();
				}
				else { // No entries, disable controls
		    		setLabelText("EntryInformation",Messages.getString("BibTeXDialog.noentries")); 
				}
				setControlEnabled("Edit",true); 
			}
			else { // Failed to parse, disable controls
				setListBoxStringItemList("Entry", new String[0]);
				setControlEnabled("Edit",false); 
				setLabelText("EntryInformation",Messages.getString("BibTeXDialog.errorreadingfile")); 
		    }
    	}
    	enableEntrySelection();
    }
    
	// Update the entry information based on the current selection in the entry list 
    private void entryChange() {
    	BibMark bibMark = getCurrentEntry();
    	if (bibMark!=null) {
    		StringBuffer info = new StringBuffer();
    		if (!addInfo(bibMark,EntryType.author,info)) { addInfo(bibMark,EntryType.editor,info); }
    		addInfo(bibMark,EntryType.title,info);
    		addInfo(bibMark,EntryType.publisher,info);
    		addInfo(bibMark,EntryType.year,info);
    		setLabelText("EntryInformation", info.toString());
    	}
    	else {
    		setLabelText("EntryInformation", Messages.getString("BibTeXDialog.noinformation"));    		 
    	}
    	enableEntrySelection();
    }
    
    private boolean addInfo(BibMark bibMark, EntryType key, StringBuffer info) {
    	String s = bibMark.getField(key);
    	if (s!=null) {
    		if (info.length()>0) { info.append('\n'); }
    		info.append(s);
    		return true;
    	}
    	return false;
    }
    
    // Create a new BibTeX file
    private void newFile() {
    	String sFileName = getFileName();
    	if (sFileName!=null) {
    		if (!sFileName.equals(".bib")) { 
	    		File file = new File(bibTeXDirectory,sFileName);
	    		try {
			    	if (!file.createNewFile() && xFrame!=null) {
			            MessageBox msgBox = new MessageBox(xContext, xFrame);
			            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.thefile")+" "+sFileName+" "+Messages.getString("BibTeXDialog.alreadyexists"));  
			    	}
					reload(sFileName);
				} catch (IOException e) {
				}
    		}
    		else if (xFrame!=null) {
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.filenameempty")); 
    		}
	    }
    }
    
    // Edit the currently selected BibTeX file, if any
    private void edit() {
    	int nFile = getListBoxSelectedItem("File"); 
    	if (nFile>=0) {
	        if (files[nFile].exists()) {
	        	edit(files[nFile]);
	        }
    	}	
    }
    
    // (Re)load the list of BibTeX files
    private void reload(String sSelectedFileName) {
    	String sFile = null;
    	if (sSelectedFileName!=null) { // Select a new file name
    		sFile = sSelectedFileName;
    	}
    	else if (getListBoxStringItemList("File").length>0) { // Remember the previous selection, if any
	    	short nSelectedFile = getListBoxSelectedItem("File"); 
	    	if (nSelectedFile>=0 && files[nSelectedFile]!=null) {
	    		sFile = getListBoxStringItemList("File")[nSelectedFile]; 
	    	}
    	}
    	
    	if (bibTeXDirectory!=null && bibTeXDirectory.isDirectory()) {
    		// Populate the file list based on the BibTeX directory
    		files = bibTeXDirectory.listFiles(
				new FilenameFilter() {
					public boolean accept(File file, String sName) { return sName!=null && sName.endsWith(".bib"); } 
				}
    		);
    		int nFileCount = files.length;
    		String[] sFileNames = new String[nFileCount];

    		// Select either the first or the previous item
    		short nFile = 0;
    		for (short i=0; i<nFileCount; i++) {
    			sFileNames[i] = files[i].getName();
    			if (sFileNames[i].equals(sFile)) { nFile = i; }
    		}
    		setListBoxStringItemList("File", sFileNames); 
    		setListBoxSelectedItem("File",(short)nFile); 
    		
    		// Update the UI
    		if (nFileCount>0) {
    			enableFileSelection(true);
	    		fileChange();
	    		return;
    		}
    	}
    	
    	// The directory did not contain any BibTeX files, update the UI accordingly
    	enableFileSelection(false);
		setLabelText("EntryInformation",Messages.getString("BibTeXDialog.nobibtexfiles")); 
    }
    
    // Change the citation type
    private void typeChange() {
    	enableAffix(!CITATION_TYPES[getListBoxSelectedItem("Type")].equals("nocite"));
    }
    
    // Add currently selected entry as a source for the reference
    private void addSource() {
    	BibMark currentBibMark = getCurrentEntry();
    	if (currentBibMark!=null && !hasSource(currentBibMark)) {
	   		sources.add(currentBibMark);
	   		showSources();
	   		enableEntrySelection();
    	}
    }
    
    private boolean hasSource(BibMark mark) {
    	if (mark!=null) {
	    	for (BibMark bibMark : sources) { // A Map would be nicer here...
	    		if (mark.getIdentifier().equals(bibMark.getIdentifier())) {
	    			return true;
	    		}
	    	}
    	}
    	return false;
    }
    
    // Remove last entry from list of sources
    private void removeSource() {
    	if (!sources.isEmpty()) { // A stack would be nicer here...
    		sources.remove(sources.get(sources.size()-1));
    		showSources();
    	}
    	enableEntrySelection();
    }
    
    // Insert the currently selected entry as a reference in the text document
    private void insertReference() {
    	addSource();
    	wbm.insertReference(sources,
    		CITATION_TYPES[getListBoxSelectedItem("Type")],getTextFieldText("Prefix"),getTextFieldText("Suffix"));
    }
    
    // Helper to update the list of currently selected sources
    private void showSources() {
    	StringBuffer info = new StringBuffer();
    	for (BibMark bibMark : sources) {
    		if (info.length()>0) { info.append(", "); }
    		info.append('[').append(bibMark.getIdentifier()).append(']');
    	}
    	setLabelText("SourcesLabel",info.toString());
    }
    
    // Helpers to enable or disable UI controls
    
    private void enableFileSelection(boolean bEnabled) { // Can we select a BibTeX file?
	    setControlEnabled("FileLabel",bEnabled); 
		setControlEnabled("File",bEnabled); 
		setControlEnabled("Edit",bEnabled); 
		setControlEnabled("Update",bEnabled);
		enableEntrySelection();
    }
    
    private void enableEntrySelection() { // Can we select an entry in the BibTeX file?
    	BibMark mark = getCurrentEntry();
    	boolean bEnabled = mark!=null;
		setControlEnabled("EntryLabel",bEnabled); 
		setControlEnabled("Entry",bEnabled); 
		setControlEnabled("Insert",bEnabled || !sources.isEmpty());
		setControlEnabled("TypeLabel",bEnabled);
		setControlEnabled("Type",bEnabled);
		if (bEnabled) { typeChange(); } else  { enableAffix(false); }
		setControlEnabled("AddSource",bEnabled && !hasSource(mark));
		setControlEnabled("RemoveSource",!sources.isEmpty());
    }
    
    private void enableAffix(boolean bEnabled) { // Can we enter affixes to the citation?
    	setControlEnabled("PrefixLabel",bEnabled);
    	setControlEnabled("Prefix",bEnabled);
    	setControlEnabled("SuffixLabel",bEnabled);
    	setControlEnabled("Suffix",bEnabled);
    }

    // Get a BibTeX file name from the user (possibly modified to a TeX friendly name)
	private String getFileName() {
	   	XDialog xDialog=getNewDialog();
	   	if (xDialog!=null) {
	   		DialogAccess ndlg = new DialogAccess(xDialog);
	   		ndlg.setListBoxStringItemList("Name", new String[0]); 
	   		String sResult = null;
	   		if (xDialog.execute()==ExecutableDialogResults.OK) {
	   			DialogAccess dlg = new DialogAccess(xDialog);
	   			sResult = dlg.getTextFieldText("Name"); 
	   		}
	   		xDialog.endExecute();
	   		if (sResult!=null && !sResult.toLowerCase().endsWith(".bib")) { 
	   			sResult = sResult+".bib"; 
	   		}
	   		return Misc.makeTeXFriendly(sResult,"bibliography"); 
	   	}
	   	return null;
	}
	
	// Get the New dialog (reused from the configuration dialog)
	protected XDialog getNewDialog() {
		XMultiComponentFactory xMCF = xContext.getServiceManager();
	   	try {
	   		Object provider = xMCF.createInstanceWithContext("com.sun.star.awt.DialogProvider2", xContext); 
	   		XDialogProvider2 xDialogProvider = (XDialogProvider2)
	   				UnoRuntime.queryInterface(XDialogProvider2.class, provider);
	   		String sDialogUrl = "vnd.sun.star.script:"+W2LDIALOGSCOMMON+".NewDialog?location=application"; 
	   		return xDialogProvider.createDialog(sDialogUrl);
	   	}
	   	catch (Exception e) {
	   		return null;
	   	}
	}

    // Helper function: Get the currently selected entry, or null if none is selected
    private BibMark getCurrentEntry() {
    	BibMark bibMark = null;
    	int nEntry = getListBoxSelectedItem("Entry"); 
    	if (nEntry>=0) {
    		String[] sCurrentKeys = getListBoxStringItemList("Entry"); 
    		String sKey = sCurrentKeys[nEntry];
    		bibMark = currentFile.getEntries().get(sKey);
    	}
    	return bibMark;
    }
    
    // Helper function to access BibTeX files
        
    // Edit a BibTeX files using the systems default application, if any
    private void edit(File file) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
				desktop.open(file);
			} catch (IOException e) {
		        if (xFrame!=null) {        	
		            MessageBox msgBox = new MessageBox(xContext, xFrame);
		            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.failedbibtexeditor")); 
		        }				
			}
        }        
        else if (xFrame!=null) {        	
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.nobibtexeditor")); 
        }
    }
    
    // Load and parse all available BibTeX files
    private BibTeXReader[] parseAllBibTeXFiles() {
    	int nFiles = files.length;
    	BibTeXReader[] readers = new BibTeXReader[nFiles];
    	for (int i=0; i<nFiles; i++) {
    		try {
				readers[i] = new BibTeXReader(files[i],sBibTeXJavaEncoding);
			} catch (IOException e) {
				System.err.println(e.getMessage());
				readers[i] = null;
			} catch (ParseException e) {
				System.err.println(e.getMessage());
				readers[i] = null;
 			}
    	}
    	return readers;
    }
 
}
