/************************************************************************
 *
 *  ConverterFactory.java
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
 *  Version 2.0 (2018-03-08)
 *
 */
 
package writer2latex.api;

/** This is a factory class which provides static methods to create converters
 *  for documents in OpenDocument (or OpenOffice.org 1.x) format into a specific MIME type
 */
public class ConverterFactory {

    // Version information
    private static final String VERSION = "1.9.1";
    private static final String DATE = "2018-03-08";

    /** Return the Writer2LaTeX version in the form
     *  (major version).(minor version).(patch level)<br/>
     *  Development versions have an odd minor version number
     *  @return the version number
     */
    public static String getVersion() { return VERSION; }

    /** Return date information
     *  @return the release date for this Writer2LaTeX version
     */
    public static String getDate() { return DATE; }

    /** <p>Create a <code>Converter</code> implementation which supports
     *  conversion into the specified MIME type.</p>
     *  <p>Currently supported MIME types are:</p>
     *  <ul>
     *    <li><code>application/x-latex</code> for LaTeX format</li>
     *    <li><code>application/x-bibtex</code> for BibTeX format</li>
     *    <li><code>text/html5</code> or <code>text/html5</code> for HTML5 documents
     *    (the latter for backwards compatibility with w2l 1.4 and 1.6)</li>
     *    <li><code>application/epub+zip</code></li> for EPUB format</li>
     *    <li><code>epub3</code> for EPUB 3 format</li>
     *  </ul>
     *  
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>Converter</code> or null if a converter for
     *  the requested MIME type could not be created
     */
    public static Converter createConverter(String sMIME) {
        Object converter = null;
        if (MIMETypes.LATEX.equals(sMIME)) {
            converter = createInstance("writer2latex.latex.ConverterPalette");
        }
        else if (MIMETypes.BIBTEX.equals(sMIME)) {
            converter = createInstance("writer2latex.bibtex.Converter");
        }
        else if (MIMETypes.HTML.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.Converter");
        }
        else if (MIMETypes.HTML5.equals(sMIME)) {
            converter = createInstance("writer2latex.xhtml.Converter");
        }
        else if (MIMETypes.EPUB3.equals(sMIME)) {
            converter = createInstance("writer2latex.epub.EPUB3Converter");
        }
        else if (MIMETypes.EPUB.equals(sMIME)) {
            converter = createInstance("writer2latex.epub.EPUBConverter");
        }
        return converter instanceof Converter ? (Converter) converter : null;
    }
	
    /** Create a <code>StarMathConverter</code> implementation
     *
     *  @return the converter
     */
    public static StarMathConverter createStarMathConverter() {
        Object converter = createInstance("writer2latex.latex.StarMathConverter");
        return converter instanceof StarMathConverter ? (StarMathConverter) converter : null;
    }
	
    private static Object createInstance(String sClassName) {
        try {
		    return Class.forName(sClassName).newInstance();
        }
        catch (java.lang.ClassNotFoundException e) {
            return null;
        } 
        catch (java.lang.InstantiationException e) {
            return null;
        }
        catch (java.lang.IllegalAccessException e) {
            return null;
        } 
    }

}
