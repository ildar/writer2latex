/************************************************************************
 *
 *  BibTeXDocument.java
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
 *  Version 1.6 (2015-05-05)
 *
 */

package writer2latex.bibtex;

import java.util.Hashtable;
import java.util.Enumeration;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import writer2latex.api.ConverterFactory;
import writer2latex.api.MIMETypes;
import writer2latex.api.OutputFile;
import writer2latex.latex.LaTeXConfig;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.i18n.I18n;
import writer2latex.util.ExportNameCollection;
import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;;

/**
 * <p>Class representing a BibTeX document.</p>
 *
 */
public class BibTeXDocument implements OutputFile {
    private static final String FILE_EXTENSION = ".bib";
	
    private String sName;
    private Hashtable<String, BibMark> entries = new Hashtable<String, BibMark>();
    private ExportNameCollection exportNames = new ExportNameCollection("",true,"_-:");
    private I18n i18n;
    
    private boolean bIsMaster;

    /**
     * <p>Constructs a new BibTeX Document.</p>
     *
     * <p>This new document is empty. Bibliographic data must added
     *    using the <code>put</code> method.</p>
     *
     * @param   sName    The name of the <code>BibTeXDocument</code>.
     */
    public BibTeXDocument(String sName, boolean bIsMaster) {
        this.sName = trimDocumentName(sName);
        this.bIsMaster = bIsMaster;
        // Use default config (only ascii, no extra font packages)
        i18n = new ClassicI18n(new LaTeXConfig());
    }
    
    /**
     * <p>Returns the <code>Document</code> name with no file extension.</p>
     *
     * @return  The <code>Document</code> name with no file extension.
     */
    public String getName() {
        return sName;
    }
    
    
    /**
     * <p>Returns the <code>Document</code> name with file extension.</p>
     *
     * @return  The <code>Document</code> name with file extension.
     */
    @Override public String getFileName() {
        return new String(sName + FILE_EXTENSION);
    }
    
	@Override public String getMIMEType() {
		return MIMETypes.BIBTEX;
	}
	
	@Override public boolean isMasterDocument() {
		return bIsMaster;
	}
	
	@Override public boolean containsMath() {
		return false;
	}

    /**
     * <p>Writes out the <code>Document</code> content to the specified
     * <code>OutputStream</code>.</p>
     *
     * <p>This method may not be thread-safe.
     * Implementations may or may not synchronize this
     * method.  User code (i.e. caller) must make sure that
     * calls to this method are thread-safe.</p>
     *
     * @param  os  <code>OutputStream</code> to write out the
     *             <code>Document</code> content.
     *
     * @throws  IOException  If any I/O error occurs.
     */
    public void write(OutputStream os) throws IOException {
        // BibTeX files are plain ascii
        OutputStreamWriter osw = new OutputStreamWriter(os,"ASCII");
        osw.write("%% This file was converted to BibTeX by Writer2BibTeX ver. "+ConverterFactory.getVersion()+".\n");
        osw.write("%% See http://writer2latex.sourceforge.net for more info.\n");
        osw.write("\n");
        Enumeration<BibMark> enumeration = entries.elements();
        while (enumeration.hasMoreElements()) {
            BibMark entry = enumeration.nextElement();
            osw.write("@");
            osw.write(entry.getEntryType().toUpperCase());
            osw.write("{");
            osw.write(exportNames.getExportName(entry.getIdentifier()));
            osw.write(",\n");
            for (EntryType entryType : EntryType.values()) {
                String sValue = entry.getField(entryType);
                if (sValue!=null) {
                    if (entryType==EntryType.author || entryType==EntryType.editor) {
                        // OOo uses ; to separate authors and editors - BibTeX uses and
                        sValue = sValue.replaceAll(";" , " and ");
                    }
                    osw.write("    ");
                    osw.write(BibTeXEntryMap.getFieldName(entryType).toUpperCase());
                    osw.write(" = {");
                    for (int j=0; j<sValue.length(); j++) {
                        String s = i18n.convert(Character.toString(sValue.charAt(j)),false,"en");
                        if (s.charAt(0)=='\\') { osw.write("{"); }
                        osw.write(s);
                        if (s.charAt(0)=='\\') { osw.write("}"); }
                    }
                    osw.write("},\n");
                }
            }
            osw.write("}\n\n");
        }
        osw.flush();
        osw.close();
    }	

    /*
     * <p>Check if this entry exists</p>
     */
    public boolean containsKey(String sIdentifier) {
        return entries.containsKey(sIdentifier);
    }

    /*
     * <p>Add an entry</p>
     */
    public void put(BibMark entry) {
        entries.put(entry.getIdentifier(),entry);
        exportNames.addName(entry.getIdentifier());
    }

    /*
     * <p>Get export name for an identifier</p>
     */
    public String getExportName(String sIdentifier) {
        return exportNames.getExportName(sIdentifier);
    }

    /*
     * Utility method to make sure the document name is stripped of any file
     * extensions before use.
     */
    private String trimDocumentName(String name) {
        String temp = name.toLowerCase();
        
        if (temp.endsWith(FILE_EXTENSION)) {
            // strip the extension
            int nlen = name.length();
            int endIndex = nlen - FILE_EXTENSION.length();
            name = name.substring(0,endIndex);
        }

        return name;
    }

}
