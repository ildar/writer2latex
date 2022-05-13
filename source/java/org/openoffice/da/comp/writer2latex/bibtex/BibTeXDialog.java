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
 *  Version 2.0 (2022-05-13)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.bibtex;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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
			"citeyear", "citedate", "citeurl" };
	
	// **** Data used for component registration

    /** The component will be registered under this service name
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.BibTeXDialog"; //$NON-NLS-1$

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
    
    // **** Implement com.sun.star.lang.XInitialization
    
    // We expect to get the current frame and a comma separated list of BibTeX files to use
    public void initialize( Object[] objects )
        throws com.sun.star.uno.Exception {
        for (Object object : objects) {
        	if (object instanceof XFrame) {
        		xFrame = (XFrame) UnoRuntime.queryInterface(XFrame.class, object);
        	}
            if (object instanceof String) {
                bibTeXDirectory = new File((String) object);
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
        	int nBibTeXEncoding = XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXEncoding"); //$NON-NLS-1$
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
        return "W2LDialogs"; //$NON-NLS-1$
    }
    
    /** Return the name of the dialog within the library
     */
    @Override public String getDialogName() {
        return "BibTeXEntry"; //$NON-NLS-1$
    }
	
    @Override public void initialize() {
        wbm = new WriterBibTeXManager(xFrame);
    	reload(null);
    	int nTypes = CITATION_TYPES.length;
    	String[] sTypes = new String[nTypes];
    	for (int i=0; i<nTypes; i++) {
    		sTypes[i] = Messages.getString("BibTeXDialog."+CITATION_TYPES[i]);
    	}
    	setListBoxStringItemList("Type", sTypes);
        setListBoxSelectedItem("Type",(short)0); // TODO: Should we remember the last setting? (Maybe not)
        setTextFieldText("Prefix",wbm.getSelectedText()); // We cannot know why the user has selected a text, but it might be useful as a prefix
    }
	
    @Override public void endDialog() {
    }

   // **** Implement XDialogEventHandler
    
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
    	clearUpdateLabel();
    	if (sMethod.equals("FileChange")) { //$NON-NLS-1$
    		// The user has selected another BibTeX file
    		fileChange();
    	}
    	else if (sMethod.equals("EntryChange")) { //$NON-NLS-1$
    		// The user has selected another BibTeX entry
    		entryChange();
    	}
    	else if (sMethod.equals("New")) { //$NON-NLS-1$
    		// Create a new BibTeX file
    		newFile();
    	}
    	else if (sMethod.equals("Edit")) { //$NON-NLS-1$
    		// Edit the current BibTeX file
    		edit();
    	}
    	else if (sMethod.equals("Reload")) { //$NON-NLS-1$
    		// Reload the BibTeX files in the dialog
    		reload(null);
    	}
    	else if (sMethod.equals("InsertReference")) { //$NON-NLS-1$
    		// Insert a reference to the current BibTeX entry
    		System.out.println("Insert reference");
    		insertReference();
    		System.out.println("Done inserting reference");
    	}
    	else if (sMethod.equals("Update")) { //$NON-NLS-1$
    		// Update all reference in the document
    		Set<String> notUpdated = wbm.update(parseAllBibTeXFiles());
    		
			// Inform the user about the result
            if (notUpdated.isEmpty()) {
    			setLabelText("UpdateLabel",Messages.getString("BibTeXDialog.allbibfieldsupdated")); //$NON-NLS-1$ //$NON-NLS-2$
            	//msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.allbibfieldsupdated")); //$NON-NLS-1$ //$NON-NLS-2$
            }
            else {
            	setLabelText("UpdateLabel",Messages.getString("BibTeXDialog.bibfieldsnotupdated")+":\n"+notUpdated.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            	//msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.bibfieldsnotupdated")+":\n"+notUpdated.toString()); //$NON-NLS-1$ //$NON-NLS-2$
            }

    	}
        return true;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "FileChange", "EntryChange", "New", "Edit", "Reload", "InsertReference", "Update" }; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$ //$NON-NLS-5$ //$NON-NLS-6$ //$NON-NLS-7$
        return sNames;
    }
    
    // **** Implement the UI functions
    
    // Clear the contents of the update info label
    private void clearUpdateLabel() {
    	setLabelText("UpdateLabel","");
    }
    
    // (Re)load the list of BibTeX files
    private void reload(String sSelectedFileName) {
    	String sFile = null;
    	if (sSelectedFileName!=null) {
    		// Select a new file name
    		sFile = sSelectedFileName;
    	}
    	else {
    		// Remember the previous selection, if any
	    	short nSelectedFile = getListBoxSelectedItem("File"); //$NON-NLS-1$
	    	if (nSelectedFile>=0 && files[nSelectedFile]!=null) {
	    		sFile = getListBoxStringItemList("File")[nSelectedFile]; //$NON-NLS-1$
	    	}
    	}
    	
    	if (bibTeXDirectory!=null && bibTeXDirectory.isDirectory()) {
        	// Populate the file list based on the BibTeX directory
    		files = bibTeXDirectory.listFiles(
    				new FilenameFilter() {
    					public boolean accept(File file, String sName) { return sName!=null && sName.endsWith(".bib"); } //$NON-NLS-1$
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

    		setListBoxStringItemList("File", sFileNames); //$NON-NLS-1$
    		setListBoxSelectedItem("File",(short)nFile); //$NON-NLS-1$
    		
    		if (nFileCount>0) {
	    		setControlEnabled("FileLabel",true); //$NON-NLS-1$
	    		setControlEnabled("File",true); //$NON-NLS-1$
	    		setControlEnabled("EntryLabel",true); //$NON-NLS-1$
	    		setControlEnabled("Entry",true); //$NON-NLS-1$
	    		setControlEnabled("Edit",true); //$NON-NLS-1$
	    		setControlEnabled("Insert",true); //$NON-NLS-1$
	    		setControlEnabled("Update",true); //$NON-NLS-1$

	    		fileChange();
	    		
	    		return;
    		}
    	}
    	
    	// The directory did not contain any BibTeX files
    	setControlEnabled("FileLabel",false); //$NON-NLS-1$
		setControlEnabled("File",false); //$NON-NLS-1$
		setControlEnabled("EntryLabel",false); //$NON-NLS-1$
		setControlEnabled("Entry",false); //$NON-NLS-1$
		setControlEnabled("Edit",false); //$NON-NLS-1$
		setControlEnabled("Insert",false); //$NON-NLS-1$
		setControlEnabled("Update",false); //$NON-NLS-1$
		setLabelText("EntryInformation",Messages.getString("BibTeXDialog.nobibtexfiles")); //$NON-NLS-1$ //$NON-NLS-2$
    }
    
    // Update the list of entries based on the current selection in the file list
    private void fileChange() {
    	// Remember current entry selection, if any
    	String sEntry = null;
    	short nEntry = getListBoxSelectedItem("Entry"); //$NON-NLS-1$
    	if (nEntry>=0) {
    		sEntry = getListBoxStringItemList("Entry")[nEntry]; //$NON-NLS-1$
    	}

    	// Parse the selected file
    	int nFile = getListBoxSelectedItem("File"); //$NON-NLS-1$
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
				setListBoxStringItemList("Entry", sCurrentKeys); //$NON-NLS-1$
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
					setListBoxSelectedItem("Entry",nEntry); //$NON-NLS-1$
					setControlEnabled("EntryLabel",true); //$NON-NLS-1$
					setControlEnabled("Entry",true); //$NON-NLS-1$
		    		setControlEnabled("Insert",true); //$NON-NLS-1$
					entryChange();
				}
				else { // No entries, disable controls
		    		setControlEnabled("EntryLabel",false); //$NON-NLS-1$
		    		setControlEnabled("Entry",false); //$NON-NLS-1$
		    		setControlEnabled("Insert",false); //$NON-NLS-1$
		    		setLabelText("EntryInformation",Messages.getString("BibTeXDialog.noentries")); //$NON-NLS-1$ //$NON-NLS-2$
				}
				setControlEnabled("Edit",true); //$NON-NLS-1$
			}
			else { // Failed to parse, disable controls
				setListBoxStringItemList("Entry", new String[0]); //$NON-NLS-1$
				setControlEnabled("EntryLabel",false); //$NON-NLS-1$
				setControlEnabled("Entry",false); //$NON-NLS-1$
				setControlEnabled("Edit",false); //$NON-NLS-1$
				setControlEnabled("Insert",false); //$NON-NLS-1$
				setLabelText("EntryInformation",Messages.getString("BibTeXDialog.errorreadingfile")); //$NON-NLS-1$ //$NON-NLS-2$
		    }
    	}
    }
    
    // Update the entry information based on the current selection in the entry list 
    private void entryChange() {
    	BibMark bibMark = getCurrentEntry();
    	if (bibMark!=null) {
    		String sAuthor = bibMark.getField(EntryType.author);
    		if (sAuthor==null) { sAuthor = ""; } //$NON-NLS-1$
    		String sTitle = bibMark.getField(EntryType.title);
    		if (sTitle==null) { sTitle = ""; } //$NON-NLS-1$
    		String sPublisher = bibMark.getField(EntryType.publisher);
    		if (sPublisher==null) { sPublisher = ""; } //$NON-NLS-1$
    		String sYear = bibMark.getField(EntryType.year);
    		if (sYear==null) { sYear = ""; } //$NON-NLS-1$
    		setLabelText("EntryInformation", sAuthor+"\n"+sTitle+"\n"+sPublisher+"\n"+sYear); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
    	}
    	else {
    		setLabelText("EntryInformation", Messages.getString("BibTeXDialog.noinformation"));    		 //$NON-NLS-1$ //$NON-NLS-2$
    	}
    }
    
    // Insert the currently selected entry as a reference in the text document
    private void insertReference() {
    	wbm.insertReference(getCurrentEntry(),
    		CITATION_TYPES[getListBoxSelectedItem("Type")],getTextFieldText("Prefix"),getTextFieldText("Suffix"));
    }
    
    // Create a new BibTeX file
    private void newFile() {
    	String sFileName = getFileName();
    	if (sFileName!=null) {
    		if (!sFileName.equals(".bib")) { //$NON-NLS-1$
	    		File file = new File(bibTeXDirectory,sFileName);
	    		try {
			    	if (!file.createNewFile() && xFrame!=null) {
			            MessageBox msgBox = new MessageBox(xContext, xFrame);
			            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.thefile")+" "+sFileName+" "+Messages.getString("BibTeXDialog.alreadyexists")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			    	}
					reload(sFileName);
				} catch (IOException e) {
				}
    		}
    		else if (xFrame!=null) {
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.filenameempty")); //$NON-NLS-1$ //$NON-NLS-2$
    		}
	    }
    }
    
    // Get a BibTeX file name from the user (possibly modified to a TeX friendly name)
	private String getFileName() {
	   	XDialog xDialog=getNewDialog();
	   	if (xDialog!=null) {
	   		DialogAccess ndlg = new DialogAccess(xDialog);
	   		ndlg.setListBoxStringItemList("Name", new String[0]); //$NON-NLS-1$
	   		String sResult = null;
	   		if (xDialog.execute()==ExecutableDialogResults.OK) {
	   			DialogAccess dlg = new DialogAccess(xDialog);
	   			sResult = dlg.getTextFieldText("Name"); //$NON-NLS-1$
	   		}
	   		xDialog.endExecute();
	   		if (sResult!=null && !sResult.toLowerCase().endsWith(".bib")) { //$NON-NLS-1$
	   			sResult = sResult+".bib"; //$NON-NLS-1$
	   		}
	   		return Misc.makeTeXFriendly(sResult,"bibliography"); //$NON-NLS-1$
	   	}
	   	return null;
	}
	
	// Get the new dialog (reused from the configuration dialog)
	protected XDialog getNewDialog() {
		XMultiComponentFactory xMCF = xContext.getServiceManager();
	   	try {
	   		Object provider = xMCF.createInstanceWithContext("com.sun.star.awt.DialogProvider2", xContext); //$NON-NLS-1$
	   		XDialogProvider2 xDialogProvider = (XDialogProvider2)
	   				UnoRuntime.queryInterface(XDialogProvider2.class, provider);
	   		String sDialogUrl = "vnd.sun.star.script:"+W2LDIALOGSCOMMON+".NewDialog?location=application"; //$NON-NLS-1$ //$NON-NLS-2$
	   		return xDialogProvider.createDialog(sDialogUrl);
	   	}
	   	catch (Exception e) {
	   		return null;
	   	}
	}

    // Edit the currently selected BibTeX file, if any
    private void edit() {
    	int nFile = getListBoxSelectedItem("File"); //$NON-NLS-1$
    	if (nFile>=0) {
	        if (files[nFile].exists()) {
	        	edit(files[nFile]);
	        }
    	}	
    }
    
    // Helper function: Get the currently selected entry, or null if none is selected
    private BibMark getCurrentEntry() {
    	BibMark bibMark = null;
    	int nEntry = getListBoxSelectedItem("Entry"); //$NON-NLS-1$
    	if (nEntry>=0) {
    		String[] sCurrentKeys = getListBoxStringItemList("Entry"); //$NON-NLS-1$
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
		            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.failedbibtexeditor")); //$NON-NLS-1$ //$NON-NLS-2$
		        }				
			}
        }        
        else if (xFrame!=null) {        	
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX",Messages.getString("BibTeXDialog.nobibtexeditor")); //$NON-NLS-1$ //$NON-NLS-2$
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
