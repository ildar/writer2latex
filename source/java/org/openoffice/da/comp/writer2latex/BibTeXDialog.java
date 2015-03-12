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
 *  Copyright: 2002-2015 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2015-02-18)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XText;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.ui.dialogs.ExecutableDialogResults;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import org.jbibtex.ParseException;
import org.openoffice.da.comp.w2lcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2lcommon.helper.DialogBase;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;

import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;
import writer2latex.util.Misc;

/** This class provides a UNO dialog to insert a BibTeX bibliographic reference
 */
public class BibTeXDialog extends DialogBase implements com.sun.star.lang.XInitialization {
	
	// **** Data used for component registration

    /** The component will be registered under this service name
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.BibTeXDialog";

    /** The implementation name of the component
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.BibTeXDialog";

    // **** Member variables
    
    // The current frame (passed at initialization)
    XFrame xFrame = null;

    // The BibTeX directory (passed at initialization)
    File bibTeXDirectory = null;
    
    // Cache of BibTeX files in the BibTeX directory
    File[] files = null;
    
    // Cache of the current BibTeX file
    BibTeXReader currentFile = null;
    
    // **** Implement com.sun.star.lang.XInitialization
    
    // We expect to get the current frame and a comma separated list of BibTeX files to use
    @Override public void initialize( Object[] objects )
        throws com.sun.star.uno.Exception {
        for (Object object : objects) {
        	if (object instanceof XFrame) {
        		xFrame = UnoRuntime.queryInterface(XFrame.class, object);
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
    }
	
    /** Return the name of the library containing the dialog
     */
    @Override public String getDialogLibraryName() {
        return "W2LDialogs2";
    }
    
    /** Return the name of the dialog within the library
     */
    @Override public String getDialogName() {
        return "BibTeXEntry";
    }
	
    @Override public void initialize() {
    	reload(null);
    }
	
    @Override public void endDialog() {
    }

   // **** Implement XDialogEventHandler
    
    @Override public boolean callHandlerMethod(XDialog xDialog, Object event, String sMethod) {
    	if (sMethod.equals("FileChange")) {
    		// The user has selected another BibTeX file
    		fileChange();
    	}
    	else if (sMethod.equals("EntryChange")) {
    		// The user has selected another BibTeX entry
    		entryChange();
    	}
    	else if (sMethod.equals("New")) {
    		// Create a new BibTeX file
    		newFile();
    	}
    	else if (sMethod.equals("Edit")) {
    		// Edit the current BibTeX file
    		edit();
    	}
    	else if (sMethod.equals("Reload")) {
    		// Reload the BibTeX files in the dialog
    		reload(null);
    	}
    	else if (sMethod.equals("InsertReference")) {
    		// Insert a reference to the current BibTeX entry
    		insertReference();
    	}
    	else if (sMethod.equals("Update")) {
    		// Update all reference in the document
    		update();
    	}
        return true;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "FileChange", "EntryChange", "New", "Edit", "Reload", "InsertReference", "Update" };
        return sNames;
    }
    
    // **** Implement the UI functions
    
    // (Re)load the list of BibTeX files
    private void reload(String sSelectedFileName) {
    	String sFile = null;
    	if (sSelectedFileName!=null) {
    		// Select a new file name
    		sFile = sSelectedFileName;
    	}
    	else {
    		// Remember the previous selection, if any
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
    		
    		if (nFileCount>0) {
	    		setControlEnabled("FileLabel",true);
	    		setControlEnabled("File",true);
	    		setControlEnabled("EntryLabel",true);
	    		setControlEnabled("Entry",true);
	    		setControlEnabled("Edit",true);
	    		setControlEnabled("Insert",true);
	    		setControlEnabled("Update",true);

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
		setControlEnabled("Update",false);
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
				System.out.println(e.getMessage());
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
    	insertReference(getCurrentEntry());
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
			            msgBox.showMessage("Writer2LaTeX","The file "+sFileName+" already exists");
			    	}
					reload(sFileName);
				} catch (IOException e) {
				}
    		}
    		else if (xFrame!=null) {
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            msgBox.showMessage("Writer2LaTeX","The file name is empty");
    		}
	    }
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
	
	// Get the new dialog (reused from the configuration dialog)
	protected XDialog getNewDialog() {
		XMultiComponentFactory xMCF = xContext.getServiceManager();
	   	try {
	   		Object provider = xMCF.createInstanceWithContext("com.sun.star.awt.DialogProvider2", xContext);
	   		XDialogProvider2 xDialogProvider = (XDialogProvider2)
	   		UnoRuntime.queryInterface(XDialogProvider2.class, provider);
	   		String sDialogUrl = "vnd.sun.star.script:"+getDialogLibraryName()+".NewDialog?location=application";
	   		return xDialogProvider.createDialog(sDialogUrl);
	   	}
	   	catch (Exception e) {
	   		return null;
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
    
    // **** Implement core functions
        
    // Edit a BibTeX files using the systems default application, if any
    private void edit(File file) {
        if (Desktop.isDesktopSupported()) {
            Desktop desktop = Desktop.getDesktop();
            try {
				desktop.open(file);
			} catch (IOException e) {
		        if (xFrame!=null) {        	
		            MessageBox msgBox = new MessageBox(xContext, xFrame);
		            msgBox.showMessage("Writer2LaTeX","Error: Failed to open file with BibTeX editor");
		        }				
			}
        }        
        else if (xFrame!=null) {        	
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX","Error: No BibTeX editor was found");
        }
    }
    
    // Update all bibliographic fields in the document
    private void update() {
    	if (xFrame!=null) {
	    	BibTeXReader[] readers = parseAllBibTeXFiles();
	    	
	    	// Collect identifiers of fields that were not updated (to inform the user)
	    	Set<String> notUpdated = new HashSet<String>();
	    	
	    	// Traverse all text fields
			XTextFieldsSupplier xSupplier = (XTextFieldsSupplier) UnoRuntime.queryInterface(
					XTextFieldsSupplier.class, xFrame.getController().getModel());
			XEnumerationAccess fields = xSupplier.getTextFields();
			XEnumeration enumeration = fields.createEnumeration();
			while (enumeration.hasMoreElements()) {
				try {
					Object elm = enumeration.nextElement();
					if (AnyConverter.isObject(elm)) {
						XTextField xTextField = (XTextField) AnyConverter.toObject(XTextField.class, elm);
						if (xTextField!=null) {
							String sId = updateTextField(xTextField, readers);
							if (sId!=null) {
								notUpdated.add(sId);
							}
						}
					}
				} catch (NoSuchElementException e) {
				} catch (WrappedTargetException e) {
				}
			}
	
			// Inform the user about the result
			if (xFrame!=null) {        	
	            MessageBox msgBox = new MessageBox(xContext, xFrame);
	            if (notUpdated.isEmpty()) {
	            	msgBox.showMessage("Writer2LaTeX","All bibliography fields were updated");
	            }
	            else {
	            	msgBox.showMessage("Writer2LaTeX","The following bibliography fields were not updated:\n"+notUpdated.toString());
	            }
	        }				
    	}
    }
    
    private BibTeXReader[] parseAllBibTeXFiles() {
    	int nFiles = files.length;
    	BibTeXReader[] readers = new BibTeXReader[nFiles];
    	for (int i=0; i<nFiles; i++) {
    		try {
				readers[i] = new BibTeXReader(files[i]);
			} catch (IOException e) {
				readers[i] = null;
			} catch (ParseException e) {
				readers[i] = null;
 			}
    	}
    	return readers;
    }
    
    // Update a text field, returning the identifier on failure and null on success(!)
    private String updateTextField(XTextField xTextField, BibTeXReader[] readers) {
        XPropertySet xPropSet = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class, xTextField);
        if (xPropSet!=null) {
			try {
				Object fieldsObj = xPropSet.getPropertyValue("Fields");
				if (fieldsObj!=null && fieldsObj instanceof PropertyValue[]) {
					PropertyValue[] props = (PropertyValue[]) fieldsObj;
					for (PropertyValue prop : props) {
						if ("Identifier".equals(prop.Name)) {
							if (prop.Value instanceof String) {
								String sIdentifier = (String)prop.Value;
								for (BibTeXReader reader : readers) {
									if (reader.getEntries().keySet().contains(sIdentifier)) {
										BibMark bibMark = reader.getEntries().get(sIdentifier);
										try {
											xPropSet.setPropertyValue("Fields", createBibliographyFields(bibMark));
											return null;
										} catch (IllegalArgumentException e) {
										} catch (PropertyVetoException e) {
										}
									}
								}
								return sIdentifier;
							}
						}
					}
				}
			} catch (UnknownPropertyException e) {
			} catch (WrappedTargetException e) {
			}
        }
        return null;
    }
    
    // Insert a bibliographic reference from a BibMark
    private void insertReference(BibMark bibMark) {
    	if (xFrame!=null) {
	        try {
	        	// To be able to manipulate the text we need to get the XText interface of the model
	        	XTextDocument xTextDoc = UnoRuntime.queryInterface(
	        			XTextDocument.class, xFrame.getController().getModel());
	        	XText xText = xTextDoc.getText();
	
	            // To locate the current position, we need to get the XTextViewCursor from the controller
	            XTextViewCursorSupplier xViewCursorSupplier = UnoRuntime.queryInterface(
	                    XTextViewCursorSupplier.class, xFrame.getController());
	            XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
	            
	        	// To create a new bibliographic field, we need to get the document service factory
	        	XMultiServiceFactory xDocFactory = UnoRuntime.queryInterface(
	        			XMultiServiceFactory.class, xFrame.getController().getModel());
	   
	            // Use the service factory to create a bibliography field
	            XDependentTextField xBibField = UnoRuntime.queryInterface (
	                XDependentTextField.class, xDocFactory.createInstance("com.sun.star.text.textfield.Bibliography"));
	            
	            // Create a field master for the field
	            XPropertySet xMasterPropSet = UnoRuntime.queryInterface(
	                XPropertySet.class, xDocFactory.createInstance("com.sun.star.text.fieldmaster.Bibliography"));
	            
	            // Populate the bibliography field
	            XPropertySet xPropSet = UnoRuntime.queryInterface(
	                    XPropertySet.class, xBibField);
	            PropertyValue[] fields = createBibliographyFields(bibMark);
	            xPropSet.setPropertyValue("Fields", fields);
	            
	            // Attach the field master to the bibliography field
	            xBibField.attachTextFieldMaster(xMasterPropSet);
	   
	         	// Finally, insert the field at the end of the cursor
	            xText.insertTextContent(xViewCursor.getEnd(), xBibField, false);
	        } catch (Exception e) {
	        }
    	}
    }

    // Create fields from a BibMark
    private PropertyValue[] createBibliographyFields(BibMark bibMark) {
        EntryType[] entryTypes = EntryType.values();
        PropertyValue[] fields = new PropertyValue[entryTypes.length+2];
        
        fields[0] = new PropertyValue();
        fields[0].Name="Identifier";
        fields[0].Value=bibMark.getIdentifier();
        fields[1] = new PropertyValue();
        fields[1].Name="BibiliographicType"; // sic! (API typo)
        fields[1].Value=new Short(getBibliographicType(bibMark.getEntryType()));
        
        int i=1;
        for (EntryType entryType : entryTypes) {
        	fields[++i] = new PropertyValue();
        	fields[i].Name = getFieldName(entryType);
        	String sValue = bibMark.getField(entryType);
        	fields[i].Value = sValue!=null ? bibMark.getField(entryType) : "";
        }
        
        return fields;
    }
    
    // Translate entry type to field name
    private String getFieldName(EntryType entryType) {
    	switch(entryType) {
    	case address: return "Address";
    	case annote: return "Annote";
    	case author: return "Author";
    	case booktitle: return "Booktitle";
    	case chapter : return "Chapter";
    	case edition: return "Edition";
    	case editor: return "Editor";
    	case howpublished: return "Howpublished";
    	case institution: return "Institution";
		case journal: return "Journal";
    	case month: return "Month";
    	case note: return "Note";
    	case number: return "Number";
    	case organizations: return "Organizations";
    	case pages: return "Pages";
    	case publisher: return "Publisher";
    	case school: return "School";
    	case series: return "Series";
    	case title: return "Title";
    	case report_type: return "Report_Type";
    	case volume: return "Volume";
    	case year: return "Year";
    	case url: return "URL";
    	case custom1: return "Custom1";
    	case custom2: return "Custom2";
    	case custom3: return "Custom3";
    	case custom4: return "Custom4";
    	case custom5: return "Custom5";
    	case isbn: return "ISBN";
    	default: return null;
    	}
    }
    
    // Translate bibliographic type to internal code
    private short getBibliographicType(String sBibType) {
    	String s = sBibType.toUpperCase();
    	if ("ARTICLE".equals(s)) {
    		return (short)0;
    	}
    	else if ("BOOK".equals(s)) {
    		return (short)1;
    	}
    	else if ("BOOKLET".equals(s)) {
    		return (short)2;
    	}
    	else if ("CONFERENCE".equals(s)) {
    		return (short)3;
    	}
    	else if ("INBOOK".equals(s)) {
    		return (short)4;
    	}
    	else if ("INCOLLECTION".equals(s)) {
    		return (short)5;
    	}
    	else if ("INPROCEEDINGS".equals(s)) {
    		return (short)6;
    	}
    	else if ("JOURNAL".equals(s)) {
    		return (short)7;
    	}
    	else if ("MANUAL".equals(s)) {
    		return (short)8;
    	}
    	else if ("MASTERSTHESIS".equals(s)) {
    		return (short)9;
    	}
    	else if ("MISC".equals(s)) {
    		return (short)10;
    	}
    	else if ("PHDTHESIS".equals(s)) {
    		return (short)11;
    	}
    	else if ("PROCEEDINGS".equals(s)) {
    		return (short)12;
    	}
    	else if ("TECHREPORT".equals(s)) {
    		return (short)13;
    	}
    	else if ("UNPUBLISHED".equals(s)) {
    		return (short)14;
    	}
    	else if ("EMAIL".equals(s)) {
    		return (short)15;
    	}
    	else if ("WWW".equals(s)) {
    		return (short)16;
    	}
    	else if ("CUSTOM1".equals(s)) {
    		return (short)17;
    	}
    	else if ("CUSTOM2".equals(s)) {
    		return (short)18;
    	}
    	else if ("CUSTOM3".equals(s)) {
    		return (short)19;
    	}
    	else if ("CUSTOM4".equals(s)) {
    		return (short)20;
    	}
    	else if ("CUSTOM5".equals(s)) {
    		return (short)21;
    	}
    	else {
    		return (short)10; // Use misc for unknown types
    	}
    }
    
}
