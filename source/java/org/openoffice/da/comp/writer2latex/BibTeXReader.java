/************************************************************************
 *
 *  BibTeXReader.java
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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jbibtex.BibTeXDatabase;
import org.jbibtex.BibTeXEntry;
import org.jbibtex.BibTeXParser;
import org.jbibtex.BibTeXString;
import org.jbibtex.Key;
import org.jbibtex.LaTeXParser;
import org.jbibtex.LaTeXPrinter;
import org.jbibtex.ParseException;
import org.jbibtex.Value;

import writer2latex.bibtex.BibTeXEntryMap;
import writer2latex.office.BibMark;
import writer2latex.office.BibMark.EntryType;

/**
 * The class reads the contents of a BibTeX file and makes it available as a map
 * of ODF <code>BibMark</code> objects
 */
public class BibTeXReader {

	private File file;
	private Map<String, BibMark> entries;
	
	/** Construct a new <code>BibTeXReader</code> based on a file
	 * 
	 * @param file the file to read
	 * @throws IOException if any error occurs reading the file
	 * @throws ParseException if any error occurs interpreting the contents of the file
	 */
	public BibTeXReader(File file) throws IOException, ParseException {
		this.file = file;
		reload();
	}

	/** Parse the contents of the file, replacing any previous entries in this <code>BibTeXReader</code>
	 */
	public void reload() throws IOException, ParseException {
		entries = new HashMap<String, BibMark>();
		BibTeXDatabase database = parseBibTeX(file);
		readEntries(database);		
	}
	
	/** Get the file associated with this <code>BibTeXReader</code>
	 * 
	 * @return the file
	 */
	public File getFile() {
		return file;
	}
	
	/** Get the entries of this BibTeX file
	 * 
	 * @return the entries
	 */
	public Map<String, BibMark> getEntries() {
		return entries;
	}

	private static BibTeXDatabase parseBibTeX(File file) throws ParseException, IOException {
		Reader reader = new FileReader(file);
		try {
			BibTeXParser parser = new BibTeXParser() {
				@Override
				public void checkStringResolution(Key key, BibTeXString string) {
					if (string == null) {
						System.err.println("Unresolved string: \"" + key.getValue() + "\"");
					}
				}

				@Override
				public void checkCrossReferenceResolution(Key key,
						BibTeXEntry entry) {
					if (entry == null) {
						System.err.println("Unresolved cross-reference: \""	+ key.getValue() + "\"");
					}
				}
			};
			return parser.parse(reader);
		} finally {
			reader.close();
		}
	}

	private void readEntries(BibTeXDatabase database) {
		Map<Key, BibTeXEntry> entryMap = database.getEntries();

		Collection<BibTeXEntry> bibentries = entryMap.values();
		for (BibTeXEntry bibentry : bibentries) {
			String sKey = bibentry.getKey().toString();
			String sType = bibentry.getType().toString();
			BibMark entry = new BibMark(sKey,sType);
			entries.put(sKey, entry);
			
			Map<Key,Value> fields = bibentry.getFields();
			for (Key key : fields.keySet()) {
				Value value = fields.get(key);
				EntryType entryType = BibTeXEntryMap.getEntryType(key.getValue());
				if (entryType!=null) {
					entry.setField(entryType, parseLaTeX(value.toUserString()));
				}
			}
		}	
	}

	private static String parseLaTeX(String string) {
		Reader reader = new StringReader(string);
		try {
			LaTeXParser parser = new LaTeXParser();
			LaTeXPrinter printer = new LaTeXPrinter();
			return printer.print(parser.parse(reader));
		} catch (ParseException e) {
			// If parsing fails, return the original string
			return string;
		} finally {
			try {
				reader.close();
			} catch (IOException e) {
				// Reading from a String will not fail :-)
			}
		}
	}

}