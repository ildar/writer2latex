/************************************************************************
 *
 *  TeXDetectService.java
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
 *  Version 2.0 (2018-08-17)
 *
 */ 

package org.openoffice.da.comp.writer2latex.latex;

import java.io.IOException;


import com.sun.star.lib.uno.adapter.XInputStreamToInputStreamAdapter;
import com.sun.star.lib.uno.helper.WeakBase;

import com.sun.star.beans.PropertyValue;
import com.sun.star.document.XExtendedFilterDetection;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.io.XInputStream;
import com.sun.star.ucb.XSimpleFileAccess2;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;



/** This class provides detect services for TeX documents
 *  It is thus an implementation of the service com.sun.star.document.ExtendedTypeDetection
 */

public class TeXDetectService extends WeakBase implements XExtendedFilterDetection, XServiceInfo {
	
	// Constants
	
	// Identify this service
	public static final String __implementationName = TeXDetectService.class.getName();
	public static final String __serviceName = "com.sun.star.document.ExtendedTypeDetection"; 
	private static final String[] m_serviceNames = { __serviceName };
	
	// The type names
	private static final String LATEX_FILE = "org.openoffice.da.writer2latex.LaTeX_File";
	private static final String XELATEX_FILE = "org.openoffice.da.writer2latex.XeLaTeX_File";
	
	// From constructor+initialization
private final XComponentContext m_xContext;

	/** Construct a new <code>TeXDetectService</code>
	 * 
	 * @param xContext The Component Context
	 */
	public TeXDetectService( XComponentContext xContext ) {
		m_xContext = xContext;
	}
	
	// Implement com.sun.star.lang.XServiceInfo:
	public String getImplementationName() {
		return __implementationName;
	}
	
	public boolean supportsService( String sService ) {
		int len = m_serviceNames.length;

		for(int i=0; i < len; i++) {
			if (sService.equals(m_serviceNames[i]))
				return true;
		}
		return false;
	}

	public String[] getSupportedServiceNames() {
		return m_serviceNames;
	}
		
	// Implement XExtendedFilterDetection
	public String detect(PropertyValue[][] mediaDescriptor) {
		// Read the media properties
		String sURL = null;
		String sTypeName = null;
		if (mediaDescriptor.length>0) {
			int nLength = mediaDescriptor[0].length;
			for (int i=0; i<nLength; i++) {
				try {
					if (mediaDescriptor[0][i].Name.equals("URL")) {
						sURL = AnyConverter.toString(mediaDescriptor[0][i].Value);
					}
					else if (mediaDescriptor[0][i].Name.equals("TypeName")) {
						sTypeName = AnyConverter.toString(mediaDescriptor[0][i].Value);
					}
				}
				catch (com.sun.star.lang.IllegalArgumentException e) {
					// AnyConverter failed to convert; ignore this
				}
			}
		}
		
		// If there's no URL, we cannot verify the type (this should never happen on proper use of the service)
		if (sURL==null) {
			return "";
		}
		
		// Also, we can only verify LaTeX and XeLaTeX
		if (sTypeName==null || !(sTypeName.equals(LATEX_FILE) || sTypeName.equals(XELATEX_FILE))) {
			return "";
		}

		// Initialise the file access
		XSimpleFileAccess2 sfa2 = null;
		try {
			Object sfaObject = m_xContext.getServiceManager().createInstanceWithContext(
					"com.sun.star.ucb.SimpleFileAccess", m_xContext);
			sfa2 = (XSimpleFileAccess2) UnoRuntime.queryInterface(XSimpleFileAccess2.class, sfaObject);
		}
		catch (com.sun.star.uno.Exception e) {
			// failed to get SimpleFileAccess service (should not happen)
			return "";
		}
		
		// Get the input stream
		XInputStreamToInputStreamAdapter is = null;
		try {
			XInputStream xis = sfa2.openFileRead(sURL);
			is = new XInputStreamToInputStreamAdapter(xis);
		}
		catch (com.sun.star.ucb.CommandAbortedException e) {
			// Failed to create input stream, cannot verify the type
			return "";
		}
		catch (com.sun.star.uno.Exception e) {
			// Failed to create input stream, cannot verify the type
			return "";
		}

		// Ask the deTeXtive
		DeTeXtive deTeXtive = new DeTeXtive();
		try {
			String sType = deTeXtive.deTeXt(is);
			if ("LaTeX".equals(sType)) {
				return LATEX_FILE;
			}
			else if ("XeLaTeX".equals(sType)) {
				return XELATEX_FILE;
			}
			else {
				return "";
			}
		}
		catch (IOException e) {
			// Failed to read the stream, cannot verify the type
			return "";
		}
				
	}


}
