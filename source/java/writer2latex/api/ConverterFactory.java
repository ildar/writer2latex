/************************************************************************
 *
 *  ConverterFactory.java
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
 *  Version 2.0 (2022-08-18)
 *
 */
 
package writer2latex.api;

/** This is a factory class which provides static methods to create converters
 *  for documents in OpenDocument format into a specific MIME type
 */
public class ConverterFactory {

    // Version information
    private static final String VERSION = "1.9.4";
    private static final String DATE = "2022-08-18";

    /** Return the Writer2LaTeX version in the form (major version).(minor version).(patch level)
     *  @return the version number
     */
    public static String getVersion() { return VERSION; }

    /** Return date information
     *  @return the release date for this Writer2LaTeX version
     */
    public static String getDate() { return DATE; }

    /** Create a <code>Converter</code> implementation which supports
     *  conversion into the specified MIME type.
     *  Currently supported MIME types are:
     *  <ul>
     *    <li><code>application/x-latex</code> for LaTeX format</li>
     *    <li><code>application/x-bibtex</code> for BibTeX format</li>
     *  </ul>
     *  
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>Converter</code> or null if a converter for
     *  the requested MIME type could not be created
     */
    public static Converter createConverter(String sMIME) {
        if (MIMETypes.LATEX.equals(sMIME)) {
            return new writer2latex.latex.ConverterPalette();
        }
        else if (MIMETypes.BIBTEX.equals(sMIME)) {
            return new writer2latex.bibtex.Converter();
        }
        return null;
    }
	
    /** Create a <code>Config</code> implementation which supports
     *  configuration for export to the specified MIME type.
     *  This method is intended for stand alone usage (read/write configuration). 
     *  Currently supported MIME types are:
     *  <ul>
     *    <li><code>application/x-latex</code> for LaTeX format</li>
     *    <li><code>application/x-bibtex</code> for BibTeX format</li>
     *  </ul>
     *  
     *  @param sMIME the MIME type of the target format
     *  @return the required <code>Config</code> or null if a configuration for
     *  the requested MIME type could not be created
     */
    public static Config createConfig(String sMIME) {
        if (MIMETypes.LATEX.equals(sMIME) || MIMETypes.BIBTEX.equals(sMIME)) {
            return new writer2latex.latex.LaTeXConfig();
        }
        return null;
    }
    
    /** Create a <code>StarMathConverter</code> implementation
     *
     *  @return the converter
     */
    public static StarMathConverter createStarMathConverter() {
        return new writer2latex.latex.StarMathConverter();
    }
}
