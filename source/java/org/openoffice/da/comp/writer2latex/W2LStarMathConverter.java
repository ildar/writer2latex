/************************************************************************
 *
 *  W2LStarMathConverter.java
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
 */

// Version 1.0 (2008-11-22)
 
package org.openoffice.da.comp.writer2latex;

import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.uno.Type;
//import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.lang.XServiceName;

import writer2latex.api.ConverterFactory;
import writer2latex.api.StarMathConverter;

// Import interface as defined in uno idl
import org.openoffice.da.writer2latex.XW2LStarMathConverter;

/** This class provides a uno component which implements the interface
 *  org.openoffice.da.writer2latex.XW2LConverter
 */
public class W2LStarMathConverter implements
        XW2LStarMathConverter,						     
        XServiceName,
        XServiceInfo,
        XTypeProvider {
    
    /** The component will be registered under this name.
     */
    public static final String __serviceName = "org.openoffice.da.writer2latex.W2LStarMathConverter";
	
    public static final String __implementationName = "org.openoffice.da.comp.writer2latex.W2LStarMathConverter";

    //private static XComponentContext xComponentContext = null;
    private static StarMathConverter starMathConverter; 

    public W2LStarMathConverter(XComponentContext xComponentContext1) {
        starMathConverter = ConverterFactory.createStarMathConverter();
    }
        
    // Implementation of XW2LConverter:
    public String convertFormula(String sStarMathFormula) {
        return starMathConverter.convert(sStarMathFormula);
    }
	
    public String getPreamble() {
        return starMathConverter.getPreamble();
    }
       

        // Implement methods from interface XTypeProvider
        // Implementation of XTypeProvider
		
        public com.sun.star.uno.Type[] getTypes() {
            Type[] typeReturn = {};

            try {
                typeReturn = new Type[] {
                new Type( XW2LStarMathConverter.class ),
                new Type( XTypeProvider.class ),
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

        // Implement method from interface XServiceName
        public String getServiceName() {
            return( __serviceName );
        }
    
        // Implement methods from interface XServiceInfo
        public boolean supportsService(String stringServiceName) {
            return( stringServiceName.equals( __serviceName ) );
        }
    
        public String getImplementationName() {
            return( __implementationName );
        }
    
        public String[] getSupportedServiceNames() {
            String[] stringSupportedServiceNames = { __serviceName };
            return( stringSupportedServiceNames );
        }

		
}



