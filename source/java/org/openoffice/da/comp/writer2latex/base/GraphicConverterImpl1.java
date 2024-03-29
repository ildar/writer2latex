/************************************************************************
 *
 *  GraphicConverterImpl1.java
 *
 *  Copyright: 2002-2014 by Henrik Just
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
 *  Version 1.4 (2014-09-05)
 */

 
package org.openoffice.da.comp.writer2latex.base;

// Java uno helper class
import com.sun.star.lib.uno.adapter.ByteArrayToXInputStreamAdapter;

// UNO classes
import com.sun.star.beans.PropertyValue;
import com.sun.star.graphic.XGraphic;
import com.sun.star.graphic.XGraphicProvider;
//import com.sun.star.io.XInputStream;
//import com.sun.star.io.XOutputStream;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.uno.XComponentContext;
import com.sun.star.uno.UnoRuntime;

//import java.io.InputStream;
//import java.io.OutputStream;

import writer2latex.api.GraphicConverter;
import writer2latex.api.MIMETypes;

/** A GraphicConverter implementation which uses the GraphicProvider service
 *  to convert the graphic. This service does only support simple format
 *  conversion using the "internal" graphics filters in Draw. Advanced features
 *  like pdf, crop and resize thus cannot be handled.
 */
public class GraphicConverterImpl1 implements GraphicConverter {

    private XGraphicProvider xGraphicProvider;
    
    private EPSCleaner epsCleaner;
	
    public GraphicConverterImpl1(XComponentContext xComponentContext) {
        try {
            // Get the XGraphicProvider interface of the GraphicProvider service
            XMultiComponentFactory xMCF = xComponentContext.getServiceManager();
            Object graphicProviderObject = xMCF.createInstanceWithContext("com.sun.star.graphic.GraphicProvider", xComponentContext);
            xGraphicProvider = (XGraphicProvider) UnoRuntime.queryInterface(XGraphicProvider.class, graphicProviderObject);
        }
        catch (com.sun.star.uno.Exception ex) {
            System.err.println("Failed to get XGraphicProvider object");
            xGraphicProvider = null;
        }
        
        epsCleaner = new EPSCleaner();
            
    }
	
    public boolean supportsConversion(String sSourceMime, String sTargetMime, boolean bCrop, boolean bResize) {
        // We don't support cropping and resizing
        if (bCrop || bResize) { return false; }

        // We can convert vector formats to EPS and SVG
        if ((MIMETypes.EPS.equals(sTargetMime) || MIMETypes.SVG.equals(sTargetMime)) &&
        		(MIMETypes.EMF.equals(sSourceMime) || MIMETypes.WMF.equals(sSourceMime) || MIMETypes.SVM.equals(sSourceMime))) {
            return true;
        }
		
        // And we can convert all formats to bitmaps
        boolean bSupportsSource =
           MIMETypes.PNG.equals(sSourceMime) || MIMETypes.JPEG.equals(sSourceMime) ||
           MIMETypes.GIF.equals(sSourceMime) || MIMETypes.TIFF.equals(sSourceMime) ||
           MIMETypes.BMP.equals(sSourceMime) || MIMETypes.EMF.equals(sSourceMime) ||
           MIMETypes.WMF.equals(sSourceMime) || MIMETypes.SVM.equals(sSourceMime);
        boolean bSupportsTarget =
           MIMETypes.PNG.equals(sTargetMime) || MIMETypes.JPEG.equals(sTargetMime) ||
           MIMETypes.GIF.equals(sTargetMime) || MIMETypes.TIFF.equals(sTargetMime) ||
           MIMETypes.BMP.equals(sTargetMime);
        return bSupportsSource && bSupportsTarget;
    }
	
    public byte[] convert(byte[] source, String sSourceMime, String sTargetMime) {
        // It seems that the GraphicProvider can only create proper eps if
        // the source is a vector format, hence
        if (MIMETypes.EPS.equals(sTargetMime)) {
            if (!MIMETypes.EMF.equals(sSourceMime) && !MIMETypes.WMF.equals(sSourceMime) && !MIMETypes.SVM.equals(sSourceMime)) {
                return null;
            }
        }

        ByteArrayToXInputStreamAdapter xSource = new ByteArrayToXInputStreamAdapter(source);
        ByteArrayXStream xTarget = new ByteArrayXStream();
        try {
            // Read the source
            PropertyValue[] sourceProps = new PropertyValue[1];
            sourceProps[0]       = new PropertyValue();
            sourceProps[0].Name  = "InputStream";
            sourceProps[0].Value = xSource;
            XGraphic result = xGraphicProvider.queryGraphic(sourceProps);

            // Store as new type
            PropertyValue[] targetProps = new PropertyValue[2];
            targetProps[0]       = new PropertyValue();
            targetProps[0].Name  = "MimeType";
            targetProps[0].Value = sTargetMime;
            targetProps[1]       = new PropertyValue();
            targetProps[1].Name  = "OutputStream";
            targetProps[1].Value = xTarget; 
            xGraphicProvider.storeGraphic(result,targetProps);

            // Close the output and return the result
            xTarget.flush();
            xTarget.closeOutput();
            if (MIMETypes.EPS.equals(sTargetMime)) {
                return epsCleaner.cleanEps(xTarget.getBuffer());
            }
            else {
            	byte[] converted = xTarget.getBuffer();
                if (converted.length>0) { // Older versions of AOO/LO fails to convert to SVG (empty result) 
                	return converted;
                }
                return null;
            }
        }
        catch (com.sun.star.io.IOException e) {
            return null;
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            return null;
        }
        catch (com.sun.star.lang.WrappedTargetException e) {
            return null;
        }
        catch (Throwable e) {
            return null;
        }
    } 
	
}

