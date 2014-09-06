/************************************************************************
 *
 *  EPUBConverter.java
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
 *  Copyright: 2001-2014 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  version 1.4 (2014-09-06)
 *
 */

package writer2latex.epub;

import java.io.IOException;
import java.io.InputStream;

import writer2latex.api.ConverterResult;
import writer2latex.base.ConverterResultImpl;
import writer2latex.xhtml.Xhtml11Converter;


/** This class converts an OpenDocument file to an EPUB document.
 */
public final class EPUBConverter extends Xhtml11Converter {
                        
    // Constructor
    public EPUBConverter() {
        super();
        System.out.println("Creating epub converter");
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
    	epubResult.addDocument(new EPUBWriter(xhtmlResult,sTargetFileName,getXhtmlConfig()));
    	epubResult.setMetaData(xhtmlResult.getMetaData());
    	return epubResult;
    }

}