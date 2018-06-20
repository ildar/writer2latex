/************************************************************************
 *
 *  L10n.java
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
 *  Version 2.0 (2018-06-20)
 *  
 */

package l10ntools;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

import writer2latex.base.DOMDocument;
import writer2latex.util.Misc;

/** This class is part of the l10n framework for Writer2LaTeX. The class extracts localized strings to a Java .properties file.
 *  The localized strings are from configuration files (<code>.xcu</code>).
 *  Furthermore, localized strings can be merged back in from a .properties file.
 */
public class L10n {
	private static final String OXTPATH = "/source/oxt/writer2latex/";
	
	private static String sSourcePath;
	private static String sLocale;
	private static String sLocaleXML;
	
	public L10n() {
	}

	/** Command line application, parameters are <command> <path> <locale>, where
	 *  <command> can be extract (to .properties file) or merge (from .properties file),
	 *  <path> is the root path to the w2l source tree, and <locale> is the locale to handle,
	 *  e.g. da_DK
	 * 
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		L10n l10n = new L10n();
		String sCommand = args[0];
		sSourcePath = args[1];
		sLocale = args[2];
		sLocaleXML = sLocale.replace("_", "-");
		
		Properties props = new Properties();

		if (sCommand.equals("extract")) {
			// Extract .properties file from .xcu files for given locale
			l10n.traverseXcuFiles(props, false);
			try {
				FileOutputStream fos = new FileOutputStream("xcu_"+sLocale+".properties"); 
				props.store(fos,null);
				fos.close();
			}
			catch (IOException e) {
				System.err.println("Error writing .properties file: "+e.getMessage());
			}
		}
		else if (sCommand.equals("merge")) {
			// Merge .properties file into .xcu files for given locale
			try {
				FileInputStream fis = new FileInputStream("xcu_"+sLocale+".properties");
				props.load(fis);
				fis.close();
				l10n.traverseXcuFiles(props, true);				
			} catch (IOException e) {
				System.err.println("Error reading .properties file: "+e.getMessage());
			}
		}
	}
	
	// Handle localized strings in .xcu files
	
	private void traverseXcuFiles(Properties props, boolean bMerge) {
		traverseXcu("Options.xcu",props,bMerge);
		traverseXcu("OptionPages.xcu",props,bMerge);
		traverseXcu("Addons.xcu",props,bMerge);
		traverseXcu("AddonsAOO4.xcu",props,bMerge);
	}
	
	private void traverseXcu(String sFileName, Properties props, boolean bMerge) {
		System.out.println((bMerge?"Merging ":"Extracting ")+sFileName);
		DOMDocument doc = new DOMDocument(sFileName,"");
		try {
			FileInputStream fis = new FileInputStream(sSourcePath+OXTPATH+sFileName);
			doc.read(fis);
			fis.close();
			Node dom = doc.getContentDOM();
			traverseNode(dom,Misc.removeExtension(sFileName),props,bMerge);
			if (bMerge) {
				doc.write(new FileOutputStream(sSourcePath+OXTPATH+sFileName));
			}
		}
		catch (IOException e) {
			System.err.println("Error reading or writing "+sFileName+": "+e.getMessage());
		}
	}
	
	private void traverseNode(Node node, String sPath, Properties props, boolean bMerge) {
		Node child = node.getFirstChild();
		while (child!=null) {
			if (child.getNodeType()==Node.ELEMENT_NODE) {
				Element elm = (Element) child;
				// An .xcu file contains a hierarchy of named nodes, which are used to construct a path
				String sNewPath = elm.hasAttribute("oor:name") ? sPath+"."+elm.getAttribute("oor:name") : sPath;
				if (elm.getNodeName().equals("prop") && elm.hasAttribute("oor:localized") && elm.getAttribute("oor:localized").equals("true")) {
					// Found a localized property node
					handleNode(elm,sNewPath,props,bMerge);
				}
				else {
					traverseNode(elm,sNewPath,props,bMerge);
				}
			}
			child = child.getNextSibling();
		}
	}
	
	private void handleNode(Element prop, String sPath, Properties props, boolean bMerge) {
		Node child = prop.getFirstChild();
		while (child!=null) {
			if (child.getNodeType()==Node.ELEMENT_NODE && child.getNodeName().equals("value")) {
				Element value = (Element)child;
				if (value.hasAttribute("xml:lang") && value.getAttribute("xml:lang").equals(sLocaleXML)) {
					if (bMerge) {
						if (props.containsKey(sPath)) {
							// Replace current contents
							while (value.hasChildNodes()) { value.removeChild(value.getFirstChild()); }
							value.appendChild(prop.getOwnerDocument().createTextNode(props.getProperty(sPath)));
							return;
						}
						System.err.println("Error: A node is missing in the .properties file, "+sPath);
					}
					else {
						// Extract the current contents
						props.put(sPath, Misc.getPCDATA(value));
						return;
					}
				}
			}
			child = child.getNextSibling();
		}
		// No value found for this locale
		if (bMerge) {
			// Add a new value node
			Element newValue = prop.getOwnerDocument().createElement("value");
			prop.appendChild(newValue);
			newValue.setAttribute("xml:lang", sLocaleXML);
			newValue.appendChild(prop.getOwnerDocument().createTextNode(props.getProperty(sPath)));
		}
		else {
			// Create empty value
			props.put(sPath, "");
		}
	}

}
