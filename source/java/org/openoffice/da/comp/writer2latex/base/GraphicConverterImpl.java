/************************************************************************
 *
 *  GraphicConverterImpl.java
 *
 *  Copyright: 2002-2008 by Henrik Just
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
 *  Version 1.0 (2008-07-21)
 *
 */
 
package org.openoffice.da.comp.writer2latex.base;

import com.sun.star.uno.XComponentContext;

import writer2latex.api.GraphicConverter;

public class GraphicConverterImpl implements GraphicConverter {

    private GraphicConverter graphicConverter1;
    private GraphicConverter graphicConverter2;

    public GraphicConverterImpl(XComponentContext xComponentContext) {
        graphicConverter1 = new GraphicConverterImpl1(xComponentContext);
        graphicConverter2 = new GraphicConverterImpl2(xComponentContext);
    }
	
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize) {
        return graphicConverter1.supportsConversion(sSourceMime, sTargetMime, bCrop, bResize) ||
               graphicConverter2.supportsConversion(sSourceMime, sTargetMime, bCrop, bResize);
    }
	
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime) {
        byte[] result = null;

        // Prefer the simple implementation (GraphicProvider)
        if (graphicConverter1.supportsConversion(sSourceMime, sTargetMime, false, false)) {
            result = graphicConverter1.convert(source, sSourceMime, sTargetMime);
        }

        // If this is not possible or fails, try the complex implementation
        if (result==null) {
            result = graphicConverter2.convert(source, sSourceMime, sTargetMime);
        }
		
        return result;
	
    }


}

