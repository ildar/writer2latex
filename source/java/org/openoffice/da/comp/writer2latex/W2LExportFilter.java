/************************************************************************
 *
 *  W2LExportFilter.java
 *
 *  Copyright: 2002-2018 by Henrik Just
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
 *  Version 2.0 (2018-04-09)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import java.io.IOException;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.uno.Type;
import com.sun.star.uno.XComponentContext;
import com.sun.star.xml.XExportFilter;
import com.sun.star.xml.sax.XDocumentHandler;

import org.openoffice.da.comp.writer2latex.base.UNOConverter;
import org.openoffice.da.comp.writer2latex.util.MessageBox;

import writer2latex.util.SimpleDOMBuilder;


/** This class provides a UNO component which implements an XExportFilter.
 *  The filter is generic, the specific formats are handled by the UNOConverter
 */
public class W2LExportFilter implements
XExportFilter,						     
XServiceName,
XServiceInfo,
XDocumentHandler,  
XTypeProvider {

    /** Service name for the component */
    public static final String __serviceName = "org.openoffice.da.comp.writer2latex.W2LExportFilter";
	
    /** Implementation name for the component */
    public static final String __implementationName = "org.openoffice.da.comp.writer2latex.W2LExportFilter";
	
	/** Filter name to include in error messages */
	private static String DISPLAY_NAME = "Writer2LaTeX";

	private XComponentContext xComponentContext = null;
	private SimpleDOMBuilder domBuilder = new SimpleDOMBuilder(); 
	private UNOConverter converter = null;

	/** Construct a new W2LExportFilter from a given component context
	 * 
	 * @param xComponentContext1 the component context used to instantiate new UNO services
	 */
    public W2LExportFilter(XComponentContext xComponentContext1) {
		this.xComponentContext = xComponentContext1;
    }

    // ---------------------------------------------------------------------------
    // Implementation of XExportFilter:

	public boolean exporter(com.sun.star.beans.PropertyValue[] aSourceData, 
			java.lang.String[] msUserData) {
		// Create a suitable converter
		converter = new UNOConverter(aSourceData, xComponentContext);
		return true;
	}

	// ---------------------------------------------------------------------------
	// Implementation of XDocumentHandler:
	// A flat XML DOM tree is created by the SAX events and finally converted

	public void  startDocument () {
		//Do nothing
	}

	public void endDocument()throws com.sun.star.uno.RuntimeException {
		try{
			converter.convert(domBuilder.getDOM());
		}
		catch (IOException e){
			MessageBox msgBox = new MessageBox(xComponentContext);
			msgBox.showMessage(DISPLAY_NAME+": IO error in conversion",
					e.toString()+" at "+e.getStackTrace()[0].toString());
			throw new com.sun.star.uno.RuntimeException(e.getMessage());
		}
		catch (Exception e){
			MessageBox msgBox = new MessageBox(xComponentContext);
			msgBox.showMessage(DISPLAY_NAME+": Internal error in conversion",
					e.toString()+" at "+e.getStackTrace()[0].toString());
			throw new com.sun.star.uno.RuntimeException(DISPLAY_NAME+" Exception");
		}
	}

	public void startElement (String sTagName, com.sun.star.xml.sax.XAttributeList xAttribs) {
		domBuilder.startElement(sTagName);
		int nLen = xAttribs.getLength();
		for (short i=0;i<nLen;i++) {
			domBuilder.setAttribute(xAttribs.getNameByIndex(i), xAttribs.getValueByIndex(i));
		}
	}

	public void endElement(String sTagName){
		domBuilder.endElement();
	}

	public void characters(String sText){
		domBuilder.characters(sText);
	}

	public void ignorableWhitespace(String str){
	}

	public void processingInstruction(String aTarget, String aData){
	}

	public void setDocumentLocator(com.sun.star.xml.sax.XLocator xLocator){
	}

	// ---------------------------------------------------------------------------
	// Implement methods from interface XTypeProvider

	public com.sun.star.uno.Type[] getTypes() {
		Type[] typeReturn = {};

		try {
			typeReturn = new Type[] {
					new Type( XTypeProvider.class ),
					new Type( XExportFilter.class ),
					new Type( XServiceName.class ),
					new Type( XServiceInfo.class ) };
		}
		catch( Exception exception ) {

		}

		return( typeReturn );
	}


	public byte[] getImplementationId() {
		byte[] byteReturn = {};

		byteReturn = new String( "" + this.hashCode() ).getBytes();

		return( byteReturn );
	}

	// ---------------------------------------------------------------------------
	// Implement method from interface XServiceName
	public String getServiceName() {
		return( __serviceName );
	}

	// ---------------------------------------------------------------------------
	// Implement methods from interface XServiceInfo
	public boolean supportsService(String stringServiceName) {
		return( stringServiceName.equals( __serviceName ) );
	}

	public String getImplementationName() {
		return __implementationName;
	}

	public String[] getSupportedServiceNames() {
		String[] stringSupportedServiceNames = { __serviceName };
		return( stringSupportedServiceNames );
	}
	
}
