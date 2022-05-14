/************************************************************************
 *
 *  WriterBibTeXManager.java
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
 *  Version 2.0 (2022-05-14)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.bibtex;

import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.openoffice.da.comp.writer2latex.Messages;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.NoSuchElementException;
import com.sun.star.container.XEnumeration;
import com.sun.star.container.XEnumerationAccess;
import com.sun.star.container.XIndexAccess;
import com.sun.star.container.XNamed;
import com.sun.star.frame.XFrame;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.IndexOutOfBoundsException;
import com.sun.star.lang.WrappedTargetException;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.text.XDependentTextField;
import com.sun.star.text.XDocumentIndex;
import com.sun.star.text.XDocumentIndexesSupplier;
import com.sun.star.text.XText;
import com.sun.star.text.XTextContent;
import com.sun.star.text.XTextDocument;
import com.sun.star.text.XTextField;
import com.sun.star.text.XTextFieldsSupplier;
import com.sun.star.text.XTextViewCursor;
import com.sun.star.text.XTextViewCursorSupplier;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;

import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;

/** This class manages insertion and update of citations in the current Writer document
 * 
 */
public class WriterBibTeXManager {
	
	private XFrame xFrame = null;
	
	public WriterBibTeXManager(XFrame xFrame) {
		this.xFrame = xFrame;
	}
	
    // Update all bibliographic fields in the document
    public Set<String> update(BibTeXReader[] readers) {
    	Set<String> notUpdated = new HashSet<>();
    	if (xFrame!=null) {
	    	// Collect identifiers of fields that were not updated (to inform the user)
	    	
	    	// Traverse all text fields and update all bibliography fields
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
							XServiceInfo xInfo = UnoRuntime.queryInterface(XServiceInfo.class, xTextField);
							if (xInfo.supportsService("com.sun.star.text.TextField.Bibliography")) { //$NON-NLS-1$
								String sId = updateBibField(xTextField, readers);
								if (sId!=null) {
									notUpdated.add(sId);
								}
							}
						}
					}
				} catch (NoSuchElementException e) {
				} catch (WrappedTargetException e) {
				}
			}
			
			// Traverse all indexes and update bibliographies
			XDocumentIndexesSupplier xIndexSupplier = (XDocumentIndexesSupplier) UnoRuntime.queryInterface(
					XDocumentIndexesSupplier.class, xFrame.getController().getModel());
			XIndexAccess xIndexAccess = xIndexSupplier.getDocumentIndexes();
			
			int nIndexCount = xIndexAccess.getCount();
			for (int i=0; i<nIndexCount; i++) {
				try {
					Object indexElm = xIndexAccess.getByIndex(i);
					if (AnyConverter.isObject(indexElm)) {
						XDocumentIndex xDocumentIndex = (XDocumentIndex) AnyConverter.toObject(XDocumentIndex.class, indexElm);
						if (xDocumentIndex!=null) {
							if ("com.sun.star.text.Bibliography".equals(xDocumentIndex.getServiceName())) { //$NON-NLS-1$
								xDocumentIndex.update();
							}
						}
					}
				} catch (IndexOutOfBoundsException e) {
				} catch (WrappedTargetException e) {
				}
			}
			
    	}
		return notUpdated;
    }
    
    // Update a bibliography field, returning the identifier on failure and null on success(!)
    private String updateBibField(XTextField xTextField, BibTeXReader[] readers) {
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
				System.err.println(e.getMessage());
			} catch (WrappedTargetException e) {
				System.err.println(e.getMessage());
			}
        }
        return null;
    }
    
    // Get the currently selected text
    public String getSelectedText() {
        // To access the selected text we need to get the XTextViewCursor from the controller
        XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier) UnoRuntime.queryInterface(
                XTextViewCursorSupplier.class, xFrame.getController());
        if (xViewCursorSupplier!=null) {
	        XTextViewCursor xViewCursor = xViewCursorSupplier.getViewCursor();
	        return xViewCursor.getString();
        }
        return "";
    }
    
    // Insert a bibliographic reference from a BibMark
    public void insertReference(BibMark bibMark, String sType, String sPrefix, String sSuffix) {
    	if (xFrame!=null) {
	        try {
	        	// To be able to manipulate the text we need to get the XText interface of the model
	        	XTextDocument xTextDoc = (XTextDocument) UnoRuntime.queryInterface(
	        			XTextDocument.class, xFrame.getController().getModel());
	        	XText xText = xTextDoc.getText();
	
	            // To locate the current position, we need to get the XTextViewCursor from the controller
	            XTextViewCursorSupplier xViewCursorSupplier = (XTextViewCursorSupplier) UnoRuntime.queryInterface(
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
	   
	            // Use the service factory to create a reference mark
	            XNamed xRefMark = (XNamed) UnoRuntime.queryInterface(XNamed.class, 
	            		xDocFactory.createInstance("com.sun.star.text.ReferenceMark"));
	            JSONObject obj = new JSONObject();
	            obj.put("key", bibMark.getIdentifier());
	            obj.put("type", sType);
	            obj.put("prefix", sPrefix);
	            obj.put("suffix", sSuffix);
	            // TODO: Add unique number to avoid two identical reference marks names
                xRefMark.setName("Writer2LaTeX_cite "+obj.toString());
	            XTextContent xRefMarkContent = (XTextContent) UnoRuntime.queryInterface(XTextContent.class, xRefMark);

	            // Decorate the prefix and the suffix depending on the citation type
	            String sDisplayType = Messages.getString("BibTeXDialog."+sType);
	            String sFinalPrefix = sPrefix.length()>0 ? sPrefix+" " : "";
	            String sFinalSuffix = sSuffix.length()>0 ? ", "+sSuffix : "";
	            if (sType.equals("nocite")) { // No affix for nocite
	            	sFinalPrefix = "";
	            	sFinalSuffix = "";
	            	
	            } else if (sType.equals("autocite")) { // Normal citation
	            	if (sFinalPrefix.length()>0 || sFinalSuffix.length()>0) {
		            	sFinalPrefix="["+sFinalPrefix;
		            	sFinalSuffix= sFinalSuffix+"]";
	            	}
	            } else if (sType.equals("textcite")) { // Text citation
	            	if (sFinalPrefix.length()>0 || sFinalSuffix.length()>0) {
		            	sFinalPrefix = sDisplayType + " [" + sFinalPrefix;
		            	sFinalSuffix = sFinalSuffix+"]";
	            	} else {
	            		sFinalPrefix = sDisplayType + " ";
	            	}
	            } else { // Author, title, year, date, URL and nocite (deliberately no space after)
	            	sFinalPrefix = sFinalPrefix + sDisplayType;
	            }
	            // Insert reference in document
	            xViewCursor.setString(""); // Delete selection
	            xText.insertString(xViewCursor.getEnd(), sFinalPrefix, false); // Insert prefix
	            xText.insertTextContent(xViewCursor.getEnd(), xBibField, false); // Insert Bibliography field
	            xText.insertString(xViewCursor.getEnd(), sFinalSuffix, false); // Insert suffix
	            int nLength = sFinalPrefix.length()+1+sFinalSuffix.length();
	            if (nLength>1) { // Insert reference mark if we have any text surrounding the bibliography field 
		            xViewCursor.goLeft((short)nLength, true); // Select the newly inserted text
		            xText.insertTextContent(xViewCursor, xRefMarkContent, true); // Insert reference mark, absorbing the text
		            xViewCursor.goRight((short)nLength, false); // Deselect the text again and place the cursor at the end
	            }
	        } catch (Exception e) {
	        	System.out.println(e.getMessage());
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
        fields[1].Value=getBibliographicType(bibMark.getEntryType());
        
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
    	if ("ARTICLE".equals(s)) { return (short)0; }
    	else if ("BOOK".equals(s)) { return (short)1; }
    	else if ("BOOKLET".equals(s)) { return (short)2; }
    	else if ("CONFERENCE".equals(s)) { return (short)3;	}
    	else if ("INBOOK".equals(s)) { return (short)4;	}
    	else if ("INCOLLECTION".equals(s)) { return (short)5; }
    	else if ("INPROCEEDINGS".equals(s)) { return (short)6; }
    	else if ("JOURNAL".equals(s)) { return (short)7; }
    	else if ("MANUAL".equals(s)) { return (short)8;	}
    	else if ("MASTERSTHESIS".equals(s)) { return (short)9; }
    	else if ("MISC".equals(s)) { return (short)10; }
    	else if ("PHDTHESIS".equals(s)) { return (short)11;	}
    	else if ("PROCEEDINGS".equals(s)) { return (short)12; }
    	else if ("TECHREPORT".equals(s)) { return (short)13; }
    	else if ("UNPUBLISHED".equals(s)) { return (short)14; }
    	else if ("EMAIL".equals(s)) { return (short)15;	}
    	else if ("WWW".equals(s)) { return (short)16; }
    	else if ("CUSTOM1".equals(s)) { return (short)17; }
    	else if ("CUSTOM2".equals(s)) { return (short)18; }
    	else if ("CUSTOM3".equals(s)) { return (short)19; }
    	else if ("CUSTOM4".equals(s)) { return (short)20; }
    	else if ("CUSTOM5".equals(s)) { return (short)21; }
    	else { return (short)10; } // Use misc for unknown types
    }


}
