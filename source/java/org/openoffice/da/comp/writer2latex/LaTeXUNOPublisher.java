/************************************************************************
 *
 *  LaTeXUNOPublisher.java
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

import java.io.File;
import java.io.IOException;

import org.openoffice.da.comp.w2lcommon.filter.UNOPublisher;
import org.openoffice.da.comp.w2lcommon.helper.MessageBox;
import org.openoffice.da.comp.w2lcommon.helper.PropertyHelper;
import org.openoffice.da.comp.w2lcommon.helper.RegistryHelper;
import org.openoffice.da.comp.w2lcommon.helper.XPropertySetHelper;

import writer2latex.util.CSVList;
import writer2latex.util.Misc;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class LaTeXUNOPublisher extends UNOPublisher {
	
	// The TeXifier and associated data
    private TeXify texify = null;
	private String sBibinputs=null;
	private String sBackend = "generic";
	
    public LaTeXUNOPublisher(XComponentContext xContext, XFrame xFrame, String sAppName) {
    	super(xContext, xFrame, sAppName);
    }
    
    /** Get the directory containing the BibTeX files (as defined in the registry)
     * 
     * @return the directory
     */
    public File getBibTeXDirectory() {
        // Get the BibTeX settings from the registry
    	RegistryHelper registry = new RegistryHelper(xContext);
		Object view;
		try {
			view = registry.getRegistryView(BibliographyDialog.REGISTRY_PATH, false);
		} catch (Exception e) {
			// Failed to get registry settings
			return null;
		}
		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
		return getDirectory(XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXLocation"),
				XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir"));
    }
    
    /** Make a file name LaTeX friendly
     */
    @Override protected String filterFileName(String sFileName) {
        String sResult = "";
        for (int i=0; i<sFileName.length(); i++) {
            char c = sFileName.charAt(i);
            if ((c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9')) {
                sResult += Character.toString(c);
            }
            else {
            	switch (c) {
            	case '.': sResult += "."; break;
            	case '-': sResult += "-"; break;
            	case ' ' : sResult += "-"; break;
            	case '_' : sResult += "-"; break;
            	// Replace accented and national characters
            	case '\u00c0' : sResult += "A"; break;
            	case '\u00c1' : sResult += "A"; break;
            	case '\u00c2' : sResult += "A"; break;
            	case '\u00c3' : sResult += "A"; break;
            	case '\u00c4' : sResult += "AE"; break;
            	case '\u00c5' : sResult += "AA"; break;
            	case '\u00c6' : sResult += "AE"; break;
            	case '\u00c7' : sResult += "C"; break;
            	case '\u00c8' : sResult += "E"; break;
            	case '\u00c9' : sResult += "E"; break;
            	case '\u00ca' : sResult += "E"; break;
            	case '\u00cb' : sResult += "E"; break;
            	case '\u00cc' : sResult += "I"; break;
            	case '\u00cd' : sResult += "I"; break;
            	case '\u00ce' : sResult += "I"; break;
            	case '\u00cf' : sResult += "I"; break;
            	case '\u00d0' : sResult += "D"; break;
            	case '\u00d1' : sResult += "N"; break;
            	case '\u00d2' : sResult += "O"; break;
            	case '\u00d3' : sResult += "O"; break;
            	case '\u00d4' : sResult += "O"; break;
            	case '\u00d5' : sResult += "O"; break;
            	case '\u00d6' : sResult += "OE"; break;
            	case '\u00d8' : sResult += "OE"; break;
            	case '\u00d9' : sResult += "U"; break;
            	case '\u00da' : sResult += "U"; break;
            	case '\u00db' : sResult += "U"; break;
            	case '\u00dc' : sResult += "UE"; break;
            	case '\u00dd' : sResult += "Y"; break;
            	case '\u00df' : sResult += "sz"; break;
            	case '\u00e0' : sResult += "a"; break;
            	case '\u00e1' : sResult += "a"; break;
            	case '\u00e2' : sResult += "a"; break;
            	case '\u00e3' : sResult += "a"; break;
            	case '\u00e4' : sResult += "ae"; break;
            	case '\u00e5' : sResult += "aa"; break;
            	case '\u00e6' : sResult += "ae"; break;
            	case '\u00e7' : sResult += "c"; break;
            	case '\u00e8' : sResult += "e"; break;
            	case '\u00e9' : sResult += "e"; break;
            	case '\u00ea' : sResult += "e"; break;
            	case '\u00eb' : sResult += "e"; break;
            	case '\u00ec' : sResult += "i"; break;
            	case '\u00ed' : sResult += "i"; break;
            	case '\u00ee' : sResult += "i"; break;
            	case '\u00ef' : sResult += "i"; break;
            	case '\u00f0' : sResult += "d"; break;
            	case '\u00f1' : sResult += "n"; break;
            	case '\u00f2' : sResult += "o"; break;
            	case '\u00f3' : sResult += "o"; break;
            	case '\u00f4' : sResult += "o"; break;
            	case '\u00f5' : sResult += "o"; break;
            	case '\u00f6' : sResult += "oe"; break;
            	case '\u00f8' : sResult += "oe"; break;
            	case '\u00f9' : sResult += "u"; break;
            	case '\u00fa' : sResult += "u"; break;
            	case '\u00fb' : sResult += "u"; break;
            	case '\u00fc' : sResult += "ue"; break;
            	case '\u00fd' : sResult += "y"; break;
            	case '\u00ff' : sResult += "y"; break;
            	}
            }
        }
        if (sResult.length()==0) { return "writer2latex"; }
        else { return sResult; }
    }

    /** Post process the filter data: Set bibliography options and
     *  determine the backend and the BIBINPUTS directory
     */
    @Override protected PropertyValue[] postProcessMediaProps(PropertyValue[] mediaProps) {
        sBackend = "generic";
        sBibinputs = null;

        PropertyHelper mediaHelper = new PropertyHelper(mediaProps);
        Object filterData = mediaHelper.get("FilterData");
        if (filterData instanceof PropertyValue[]) {
        	PropertyHelper filterHelper = new PropertyHelper((PropertyValue[])filterData);
        	
            // Get the backend
            Object backend = filterHelper.get("backend");
            if (backend instanceof String) {
                sBackend = (String) backend;
            }
            
            // Set the bibliography options according to the settings
        	RegistryHelper registry = new RegistryHelper(xContext);
        	try {
        		Object view = registry.getRegistryView(BibliographyDialog.REGISTRY_PATH, false);
        		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
        		String sBibTeXFiles = getFileList(XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXLocation"),
        				XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir"));
        		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertZoteroCitations")) {
        			filterHelper.put("zotero_bibtex_files", sBibTeXFiles);
        		}
        		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertJabRefCitations")) {
        			filterHelper.put("jabref_bibtex_files", sBibTeXFiles);
        		}
        		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "UseExternalBibTeXFiles")) {
        			filterHelper.put("external_bibtex_files", sBibTeXFiles);
        		}
    			filterHelper.put("include_original_citations",
    					Boolean.toString(XPropertySetHelper.getPropertyValueAsBoolean(xProps, "IncludeOriginalCitations")));
        		String sBibTeXDir = XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir");
        		if (sBibTeXDir.length()>0) {
        			// The separator character in BIBINPUTS is OS specific
        			sBibinputs = sBibTeXDir+File.pathSeparatorChar;
        		}
    			filterHelper.put("use_natbib", Boolean.toString(XPropertySetHelper.getPropertyValueAsBoolean(xProps, "UseNatbib")));
    			filterHelper.put("natbib_options", XPropertySetHelper.getPropertyValueAsString(xProps, "NatbibOptions"));

        		mediaHelper.put("FilterData",filterHelper.toArray());
                PropertyValue[] newMediaProps = mediaHelper.toArray();
            	registry.disposeRegistryView(view);
            	return newMediaProps;
        	}
        	catch (Exception e) {
        		// Failed to get registry view; return original media props
        		return mediaProps;
        	}
        }
    	// No filter data; return original media props
		return mediaProps;
    }
    
	/** Postprocess the converted document with LaTeX and display the result
	 */
    @Override protected void postProcess(String sURL) {
        if (texify==null) { texify = new TeXify(xContext); }
        File file = new File(Misc.urlToFile(getTargetPath()),getTargetFileName());
        
        boolean bResult = true;
        
        try {
            if (sBackend=="pdftex") {
                bResult = texify.process(file, sBibinputs, TeXify.PDFTEX, true);
            }
            else if (sBackend=="dvips") {
            	bResult = texify.process(file, sBibinputs, TeXify.DVIPS, true);
            }
            else if (sBackend=="xetex") {
            	bResult = texify.process(file, sBibinputs, TeXify.XETEX, true);
            }
            else if (sBackend=="generic") {
            	bResult = texify.process(file, sBibinputs, TeXify.GENERIC, true);
            }
        }
        catch (IOException e) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX","Error: "+e.getMessage());
        }
        
        if (!bResult) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX","Error: Failed to execute LaTeX - see log for details");        	
        }
    }
    
    private File getDirectory(short nType, String sDirectory) {
    	switch (nType) {
    	case 0: // absolute path
        	return new File(sDirectory);
    	case 1: // relative path
    		return new File(Misc.urlToFile(getTargetPath()),sDirectory);
    	default: // document directory
    		return Misc.urlToFile(getTargetPath());
    	}
    }

    private String getFileList(short nType, String sDirectory) {
    	File dir = getDirectory(nType,sDirectory);
    	File[] files;
    	if (dir.isDirectory()) {
    		files = dir.listFiles();
    	}
    	else {
    		return null;
    	}
    	CSVList filelist = new CSVList(",");
    	if (files!=null) {
    		for (File file : files) {
    			if (file.isFile() && file.getName().endsWith(".bib")) {
    				filelist.addValue(Misc.removeExtension(file.getName()));
    			}
    		}
    	}
    	return filelist.toString();
	}

}
