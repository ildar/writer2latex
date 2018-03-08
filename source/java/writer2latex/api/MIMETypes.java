/************************************************************************
 *
 *  MIMETypes.java
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

/* Some helpers to handle the MIME types used by OOo and Writer2LaTeX
 */

public class MIMETypes {
    // Various graphics formats, see
    // http://api.openoffice.org/docs/common/ref/com/sun/star/graphic/MediaProperties.html#MimeType
    public static final String PNG="image/png";
    public static final String JPEG="image/jpeg";
    public static final String GIF="image/gif";
    public static final String TIFF="image/tiff";
    public static final String BMP="image/bmp";
    public static final String EMF="image/x-emf";
    public static final String WMF="image/x-wmf";
    public static final String EPS="image/x-eps";
    public static final String SVG="image/svg+xml";
    // MIME type for SVM has changed
    //public static final String SVM="image/x-svm";
    public static final String SVM="application/x-openoffice-gdimetafile;windows_formatname=\"GDIMetaFile\"";
    public static final String PDF="application/pdf";
	
    // Destination formats
    public static final String HTML="text/html";
    /** This is a fake MIME type, for backwards compatibility */
    public static final String HTML5="text/html5";
    public static final String EPUB="application/epub+zip";
    /** This is not a MIME type either */
    public static final String EPUB3="epub3";
    public static final String LATEX="application/x-latex";
    public static final String BIBTEX="application/x-bibtex";
    public static final String TEXT="text";
	
}