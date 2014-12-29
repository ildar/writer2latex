/************************************************************************
 *
 *  BibTeXDialog.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2014-12-27)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import com.sun.star.awt.XDialog;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.jbibtex.ParseException;
import org.openoffice.da.comp.w2lcommon.helper.DialogBase;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;

import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;

/** This class provides a UNO dialog to insert a BibTeX bibliographic reference
 */
public class BibTeXDialog extends DialogBase implements com.sun.star.lang.XInitialization {

    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.BibTeXDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.BibTeXDialog";

    /** Return the name of the library containing the dialog
     */
    public String getDialogLibraryName() {
        return "W4LDialogs";
    }
    
    // The current frame (passed at initialization)
    XFrame xFrame = null;

    // The BibTeX directory (passed at initialization)
    File bibTeXDirectory = null;
    
    // Cache of BibTeX files in the BibTeX directory
    File[] files = null;
    
    // Cache of the current BibTeX file
    BibTeXReader currentFile = null;
    
    /** Return the name of the dialog within the library
     */
    public String getDialogName() {
        return "BibTeXEntry";
    }
	
    public void initialize() {
    	refresh();
    }
	
    public void endDialog() {
    }

    /** Create a new BibTeXDialog */
    public BibTeXDialog(XComponentContext xContext) {
        super(xContext);
    }
	
    // Implement com.sun.star.lang.XInitialization
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

   // Implement XDialogEventHandler
    public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
    	if (sMethod.equals("FileChange")) {
    		fileChange();
    	}
    	else if (sMethod.equals("EntryChange")) {
    		entryChange();
    	}
    	else if (sMethod.equals("InsertReference")) {
    		insertReference();
    	}
    	else if (sMethod.equals("Edit")) {
    		edit();
    	}
    	else if (sMethod.equals("Refresh")) {
    		refresh();
    	}
        return true;
    }
	
    public String[] getSupportedMethodNames() {
        String[] sNames = { "FileChange", "EntryChange", "InsertReference", "Edit", "Refresh" };
        return sNames;
    }
    
    // (Re)load the list of BibTeX files
    private void refresh() {
    	// Remember current file selection, if any
    	String sFile = null;
    	short nFile = getListBoxSelectedItem("File");
    	if (nFile>=0 && files[nFile]!=null) {
    		sFile = getListBoxStringItemList("File")[nFile];
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
    		nFile = 0;
    		for (short i=0; i<nFileCount; i++) {
    			sFileNames[i] = files[i].getName();
    			if (sFileNames[i].equals(sFile)) { nFile = i; }
    		}

    		setListBoxStringItemList("File", sFileNames);
    		setListBoxSelectedItem("File",(short)nFile);
    		
    		if (nFileCount>0) {
	    		setControlEnabled("FileLabel",true);
	    		setControlEnabled("File",true);
	    		setControlEnabled("EntryLabel",true);
	    		setControlEnabled("Entry",true);
	    		setControlEnabled("Edit",true);
	    		setControlEnabled("Insert",true);

	    		fileChange();
	    		
	    		return;
    		}
    	}
    	
    	// The directory did not contain any BibTeX files
    	setControlEnabled("FileLabel",false);
		setControlEnabled("File",false);
		setControlEnabled("EntryLabel",false);
		setControlEnabled("Entry",false);
		setControlEnabled("Edit",false);
		setControlEnabled("Insert",false);
		setLabelText("EntryInformation","No BibTeX files were found");
    }
    
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
				currentFile = new BibTeXReader(files[nFile]);
			} catch (IOException e) {
				currentFile = null;
			} catch (ParseException e) {
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
					setControlEnabled("EntryLabel",true);
					setControlEnabled("Entry",true);
		    		setControlEnabled("Insert",true);
					entryChange();
				}
				else { // No entries, disable controls
		    		setControlEnabled("EntryLabel",false);
		    		setControlEnabled("Entry",false);
		    		setControlEnabled("Insert",false);
		    		setLabelText("EntryInformation","This file does not contain any entries");
				}
				setControlEnabled("Edit",true);
			}
			else { // Failed to parse, disable controls
				setListBoxStringItemList("Entry", new String[0]);
				setControlEnabled("EntryLabel",false);
				setControlEnabled("Entry",false);
				setControlEnabled("Edit",false);
				setControlEnabled("Insert",false);
				setLabelText("EntryInformation","There was an error reading this file");
		    }
    	}
    }
    
    // Update the entry information based on the current selection in the entry list 
    private void entryChange() {
    	BibMark bibMark = getCurrentEntry();
    	if (bibMark!=null) {
    		String sAuthor = bibMark.getField(EntryType.author);
    		if (sAuthor==null) { sAuthor = ""; }
    		String sTitle = bibMark.getField(EntryType.title);
    		if (sTitle==null) { sTitle = ""; }
    		String sPublisher = bibMark.getField(EntryType.publisher);
    		if (sPublisher==null) { sPublisher = ""; }
    		String sYear = bibMark.getField(EntryType.year);
    		if (sYear==null) { sYear = ""; }
    		setLabelText("EntryInformation", sAuthor+"\n"+sTitle+"\n"+sPublisher+"\n"+sYear);
    	}
    	else {
    		setLabelText("EntryInformation", "No information");    		
    	}
    }
    
    // Insert the currently selected entry as a reference in the text document
    private void insertReference() {
        if (xFrame!=null) {        	
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX","This feature has not been implemented yet");
        }
    }
    
    // Get the currently selected entry, or null if none is selected
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
    
    // Edit the current BibTeX file
    private void edit() {
    	int nFile = getListBoxSelectedItem("File");
    	if (nFile>=0) {
	        if (files[nFile].exists()) {
		        // Open the file in the default application on this system (if any)
		        if (Desktop.isDesktopSupported()) {
		            Desktop desktop = Desktop.getDesktop();
		            try {
						desktop.open(files[nFile]);
					} catch (IOException e) {
						System.err.println(e.getMessage());
					}
		        }        
		        else if (xFrame!=null) {        	
		            MessageBox msgBox = new MessageBox(xContext, xFrame);
		            msgBox.showMessage("Writer2LaTeX","Error: No BibTeX editor was found");
		        }
		    }
    	}
    }

}