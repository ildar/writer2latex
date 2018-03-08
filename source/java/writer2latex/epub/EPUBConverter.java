/************************************************************************
 *
 *  EPUBConverter.java
 *
 *  Copyright: 2002-2015 by Henrik Just
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
 *  version 1.6 (2015-01-15)
 *
 */

package writer2latex.epub;

import java.io.IOException;
import java.io.InputStream;

import writer2latex.api.ConverterResult;
import writer2latex.base.ConverterResultImpl;
import writer2latex.xhtml.Converter;


/** This class converts an OpenDocument file to an EPUB document.
 */
public final class EPUBConverter extends Converter {
                        
    // Constructor
    public EPUBConverter() {
        super();
    }
	
    @Override public ConverterResult convert(InputStream is, String sTargetFileName) throws IOException {
    	setOPS(true);
    	ConverterResult xhtmlResult = super.convert(is, "chapter");
    	return createPackage(xhtmlResult,sTargetFileName);
    }
    
    @Override public ConverterResult convert(org.w3c.dom.Document dom, String sTargetFileName, boolean bDestructive) throws IOException {
    	setOPS(true);
    	ConverterResult xhtmlResult = super.convert(dom, "chapter", bDestructive);
    	return createPackage(xhtmlResult,sTargetFileName);    	
    }
    
    private ConverterResult createPackage(ConverterResult xhtmlResult, String sTargetFileName) {
    	ConverterResultImpl epubResult = new ConverterResultImpl();
    	epubResult.addDocument(new EPUBWriter(xhtmlResult,sTargetFileName,2,getXhtmlConfig()));
    	epubResult.setMetaData(xhtmlResult.getMetaData());
    	return epubResult;
    }

}