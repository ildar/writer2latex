/************************************************************************
 *
 *  TeXify.java
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
 *  Version 2.0 (2022-05-09)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.latex;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.openoffice.da.comp.writer2latex.Messages;


import com.sun.star.uno.XComponentContext;
       
/** This class builds LaTeX documents into DVI, Postscript or PDF and displays
 *  the result.
 */
public final class TeXify {
	
    /** Backend format generic (dvi) */
    public static final short GENERIC = 1;

    /** Backend format dvips (postscript) */
    public static final short DVIPS = 2;

    /** Backend format pdfTeX (pdf) */
    public static final short PDFTEX = 3;
    
    /** Backend format XeTeX (also pdf, usually) */
    public static final short XETEX = 4;

    // Define the applications to run for each backend
    private static final String[] genericTexify = {
        ExternalApps.LATEX, ExternalApps.BIBER, ExternalApps.MAKEINDEX,
        ExternalApps.LATEX, ExternalApps.MAKEINDEX, ExternalApps.LATEX };
    private static final String[] pdfTexify = {
        ExternalApps.PDFLATEX, ExternalApps.BIBER, ExternalApps.MAKEINDEX,
        ExternalApps.PDFLATEX, ExternalApps.MAKEINDEX, ExternalApps.PDFLATEX };
    private static final String[] dvipsTexify = {
        ExternalApps.LATEX, ExternalApps.BIBER, ExternalApps.MAKEINDEX,
        ExternalApps.LATEX, ExternalApps.MAKEINDEX, ExternalApps.LATEX,
        ExternalApps.DVIPS };
    private static final String[] xeTexify = {
        ExternalApps.XELATEX, ExternalApps.BIBER, ExternalApps.MAKEINDEX,
        ExternalApps.XELATEX, ExternalApps.MAKEINDEX, ExternalApps.XELATEX };

    // Global objects
    private ExternalApps externalApps; 
	
    public TeXify(XComponentContext xContext) {
        externalApps = new ExternalApps(xContext);
    }
	
    /** Process a document. This will either (depending on the registry settings) do nothing, build with LaTeX
     *  or build with LaTeX and preview
     *  
     *  @param file the LaTeX file to process
     *  @param sBibinputs value for the BIBINPUTS environment variable (or null if it should not be extended)
     *  @param nBackend the desired backend format (generic, dvips, pdftex)
     *  @param bView set the true if the result should be displayed in the viewer
     *  @throws IOException if the document cannot be read
     *  @return true if the first LaTeX run was successful
     */
    public boolean process(File file, String sBibinputs, short nBackend, boolean bView) throws IOException {
        // Remove extension from file
        if (file.getName().endsWith(".tex")) { //$NON-NLS-1$
            file = new File(file.getParentFile(),
                   file.getName().substring(0,file.getName().length()-4));
        }
        // Update external apps from registry
        externalApps.load();

        // Process LaTeX document
        if (externalApps.getProcessingLevel()>=ExternalApps.BUILD) {
        	boolean bPreview = externalApps.getProcessingLevel()>=ExternalApps.PREVIEW;
	        boolean bResult = false;
	        if (nBackend==GENERIC) {
	            bResult = doTeXify(genericTexify, file, sBibinputs);
	            if (!bResult) return false;
	            if (bPreview && externalApps.execute(ExternalApps.DVIVIEWER,
	                new File(file.getParentFile(),file.getName()+".dvi").getPath(), //$NON-NLS-1$
	                file.getParentFile(), null, false)>0) {
	                throw new IOException(Messages.getString("TeXify.dviviewerror")); //$NON-NLS-1$
	            }
	        }
	        else if (nBackend==PDFTEX) {
	        	bResult = doTeXify(pdfTexify, file, sBibinputs);
	            if (!bResult) return false;
	            if (bPreview && externalApps.execute(ExternalApps.PDFVIEWER,
	                new File(file.getParentFile(),file.getName()+".pdf").getPath(), //$NON-NLS-1$
	                file.getParentFile(), null, false)>0) {
	                throw new IOException(Messages.getString("TeXify.pdfviewerror")); //$NON-NLS-1$
	            }
	        }
	        else if (nBackend==DVIPS) {
	        	bResult = doTeXify(dvipsTexify, file, sBibinputs);
	            if (!bResult) return false;
	            if (bPreview && externalApps.execute(ExternalApps.POSTSCRIPTVIEWER,
	                new File(file.getParentFile(),file.getName()+".ps").getPath(), //$NON-NLS-1$
	                file.getParentFile(), null, false)>0) {
	                throw new IOException(Messages.getString("TeXify.psviewerror")); //$NON-NLS-1$
	            }
	        }
	        else if (nBackend==XETEX) {
	        	bResult = doTeXify(xeTexify, file, sBibinputs);
	            if (!bResult) return false;
	            if (bPreview && externalApps.execute(ExternalApps.PDFVIEWER,
	                    new File(file.getParentFile(),file.getName()+".pdf").getPath(), //$NON-NLS-1$
	                    file.getParentFile(), null, false)>0) {
	                    throw new IOException(Messages.getString("TeXify.pdfviewerror")); //$NON-NLS-1$
	                }
	        }
	        return bResult;
        }
        return true;
    }
	
    private boolean doTeXify(String[] sAppList, File file, String sBibinputs) throws IOException {
    	// Remove the .aux file first (to avoid potential error messages)
        File aux = new File(file.getParentFile(), file.getName()+".aux"); //$NON-NLS-1$
        aux.delete();
        for (int i=0; i<sAppList.length; i++) {
            // Execute external application
        	Map<String,String> env =null;
        	if (ExternalApps.BIBER.equals(sAppList[i]) && sBibinputs!=null) {
        		env = new HashMap<String,String>();
        		env.put("BIBINPUTS", sBibinputs); //$NON-NLS-1$
        	}
            int nReturnCode = externalApps.execute(
                sAppList[i], file.getName(), file.getParentFile(), env, true);
            if (i==0 && nReturnCode>0) {
            	return false;
                //throw new IOException("Error executing "+sAppList[i]);
            }
        }
                
        return true;
    }

}