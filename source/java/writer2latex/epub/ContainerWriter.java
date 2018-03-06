/************************************************************************
 *
 *  ContainerWriter.java
 *
 *  Copyright: 2001-2014 by Henrik Just
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
 *  version 1.4 (2014-08-26)
 *
 */

package writer2latex.epub;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;

import writer2latex.base.DOMDocument;

/** This class creates the required META-INF/container.xml file for an EPUB package 
 *  (see http://www.idpf.org/ocf/ocf1.0/download/ocf10.htm).
 */
public class ContainerWriter extends DOMDocument {
	
	public ContainerWriter() {
		super("container", "xml");
		
        // create DOM
        Document contentDOM = null;
        try {
            DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = builderFactory.newDocumentBuilder();
            DOMImplementation domImpl = builder.getDOMImplementation();
            DocumentType doctype = domImpl.createDocumentType("container","",""); 
            contentDOM = domImpl.createDocument("urn:oasis:names:tc:opendocument:xmlns:container","container",doctype);
        }
        catch (ParserConfigurationException t) { // this should never happen
            throw new RuntimeException(t);
        }
        
        // Populate the DOM tree
        Element container = contentDOM.getDocumentElement();
        container.setAttribute("version", "1.0");
        container.setAttribute("xmlns","urn:oasis:names:tc:opendocument:xmlns:container");
        
        Element rootfiles = contentDOM.createElement("rootfiles");
        container.appendChild(rootfiles);
        
        Element rootfile = contentDOM.createElement("rootfile");
        rootfile.setAttribute("full-path", "OEBPS/book.opf");
        rootfile.setAttribute("media-type", "application/oebps-package+xml");
        rootfiles.appendChild(rootfile);
        
        setContentDOM(contentDOM);
	}

}
