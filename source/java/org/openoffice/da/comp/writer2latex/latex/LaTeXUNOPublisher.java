/************************************************************************
 *
 *  LaTeXUNOPublisher.java
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
 *  Version 2.0 (2022-05-28)
 *  
 */
package org.openoffice.da.comp.writer2latex.latex;

import java.io.File;
import java.io.IOException;

import org.openoffice.da.comp.writer2latex.Messages;
import org.openoffice.da.comp.writer2latex.base.ConverterHelper;
import org.openoffice.da.comp.writer2latex.base.UNOPublisher;
import org.openoffice.da.comp.writer2latex.bibtex.BibliographyDialog;
import org.openoffice.da.comp.writer2latex.util.MessageBox;
import org.openoffice.da.comp.writer2latex.util.PropertyHelper;
import org.openoffice.da.comp.writer2latex.util.RegistryHelper;
import org.openoffice.da.comp.writer2latex.util.XPropertySetHelper;

import writer2latex.api.Config;
import writer2latex.api.ConverterFactory;
import writer2latex.api.MIMETypes;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.util.CSVList;
import writer2latex.util.Misc;

import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertySet;
import com.sun.star.frame.XFrame;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.Exception;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

public class LaTeXUNOPublisher extends UNOPublisher {
	
	// The TeXifier and associated data
    private TeXify texify = null;
	private String sBibinputs=null;
	private String sBackend = "generic"; //$NON-NLS-1$
	
    public LaTeXUNOPublisher(XComponentContext xContext, XFrame xFrame) {
    	super(xContext, xFrame);
    }
    
    protected String getTargetExtension() {
    	return ".tex"; //$NON-NLS-1$
    }
    
    protected String getDialogName() { 
    	return org.openoffice.da.comp.writer2latex.latex.LaTeXFilterDialog.class.getName();
	}

    protected  String getFilterName() {
    	return "org.openoffice.da.writer2latex"; //$NON-NLS-1$
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
		return getDirectory(XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXLocation"), //$NON-NLS-1$
				XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir")); //$NON-NLS-1$
    }
    
    /** Make a file name LaTeX friendly
     */
    @Override protected String filterFileName(String sFileName) {
    	return Misc.makeTeXFriendly(sFileName,"writer2latex"); //$NON-NLS-1$
    }

    /** Post process the filter data: Set bibliography options and
     *  determine the backend and the BIBINPUTS directory
     */
    @Override protected PropertyValue[] postProcessMediaProps(PropertyValue[] mediaProps) {
        sBibinputs = null;

        PropertyHelper mediaHelper = new PropertyHelper(mediaProps);
        Object filterData = mediaHelper.get("FilterData"); //$NON-NLS-1$
        if (filterData instanceof PropertyValue[]) {
        	PropertyHelper filterHelper = new PropertyHelper((PropertyValue[])filterData);
        	
        	// Get the backend
        	sBackend = "generic"; //$NON-NLS-1$
            Object backend = filterHelper.get("backend"); //$NON-NLS-1$
            if (backend instanceof String) {
                sBackend = (String) backend;
            }
            else { // The backend is not set directly in the filter data, try to get from configuration
	            Object cfg = filterHelper.get("ConfigURL");
	            if (cfg!=null && AnyConverter.isString(cfg)) {
	            	Config config = ConverterFactory.createConfig(MIMETypes.LATEX);
	            	ConverterHelper converterHelper = new ConverterHelper(xContext);
	                converterHelper.readConfig(AnyConverter.toString(cfg),config);
	                sBackend = config.getOption("backend");
	            }
            }
            
            // Set the bibliography options according to the settings
        	RegistryHelper registry = new RegistryHelper(xContext);
        	try {
        		Object view = registry.getRegistryView(BibliographyDialog.REGISTRY_PATH, false);
        		XPropertySet xProps = (XPropertySet) UnoRuntime.queryInterface(XPropertySet.class,view);
        		String sBibTeXFiles = getFileList(XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXLocation"), //$NON-NLS-1$
        				XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir")); //$NON-NLS-1$
        		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "UseExternalBibTeXFiles")) { //$NON-NLS-1$
        			filterHelper.put("external_bibtex_files", sBibTeXFiles); //$NON-NLS-1$
        			filterHelper.put("bibtex_encoding", ClassicI18n.writeInputenc(  //$NON-NLS-1$
        					XPropertySetHelper.getPropertyValueAsShort(xProps, "BibTeXEncoding")));  //$NON-NLS-1$
            		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertZoteroCitations")) { //$NON-NLS-1$
            			filterHelper.put("zotero_bibtex_files", sBibTeXFiles); //$NON-NLS-1$
            		}
            		if (XPropertySetHelper.getPropertyValueAsBoolean(xProps, "ConvertJabRefCitations")) { //$NON-NLS-1$
            			filterHelper.put("jabref_bibtex_files", sBibTeXFiles); //$NON-NLS-1$
            		}
        		}
    			filterHelper.put("include_original_citations", //$NON-NLS-1$
    					Boolean.toString(XPropertySetHelper.getPropertyValueAsBoolean(xProps, "IncludeOriginalCitations"))); //$NON-NLS-1$
        		String sBibTeXDir = XPropertySetHelper.getPropertyValueAsString(xProps, "BibTeXDir"); //$NON-NLS-1$
        		if (sBibTeXDir.length()>0) {
        			// The separator character in BIBINPUTS is OS specific
        			sBibinputs = sBibTeXDir+File.pathSeparatorChar;
        		}
        		
        		// TODO: This should be handled by the filter dialog
    			String sBibLaTeX = null;
                Object biblatex = filterHelper.get("biblatex_options"); //$NON-NLS-1$
                if (biblatex instanceof String) {
                    sBibLaTeX = (String) biblatex;
                    if (sBibLaTeX.length()==0) {
            			filterHelper.put("biblatex_options", XPropertySetHelper.getPropertyValueAsString(xProps, "BibLaTeXOptions")); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                
        		mediaHelper.put("FilterData",filterHelper.toArray()); //$NON-NLS-1$
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
    protected void postProcess(String sURL) {
        if (texify==null) { texify = new TeXify(xContext); }
        File file = new File(Misc.urlToFile(getTargetPath()),getTargetFileName());
        
        boolean bResult = true;
        
        try {
            if ("pdftex".equals(sBackend)) { //$NON-NLS-1$
                bResult = texify.process(file, sBibinputs, TeXify.PDFTEX, true);
            }
            else if ("dvips".equals(sBackend)) { //$NON-NLS-1$
            	bResult = texify.process(file, sBibinputs, TeXify.DVIPS, true);
            }
            else if ("xetex".equals(sBackend)) { //$NON-NLS-1$
            	bResult = texify.process(file, sBibinputs, TeXify.XETEX, true);
            }
            else if ("generic".equals(sBackend)) { //$NON-NLS-1$
            	bResult = texify.process(file, sBibinputs, TeXify.GENERIC, true);
            }
        }
        catch (IOException e) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX",Messages.getString("LaTeXUNOPublisher.error")+": "+e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
        }
        
        if (!bResult) {
            MessageBox msgBox = new MessageBox(xContext, xFrame);
            msgBox.showMessage("Writer2LaTeX",Messages.getString("LaTeXUNOPublisher.error")+": "+Messages.getString("LaTeXUNOPublisher.failedlatex"));        	 //$NON-NLS-1$ //$NON-NLS-2$
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
    	CSVList filelist = new CSVList(","); //$NON-NLS-1$
    	if (files!=null) {
    		for (File file : files) {
    			if (file.isFile() && file.getName().endsWith(".bib")) { //$NON-NLS-1$
    				filelist.addValue(Misc.removeExtension(file.getName()));
    			}
    		}
    	}
    	return filelist.toString();
	}

}
