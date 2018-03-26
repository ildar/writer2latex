/************************************************************************
 *
 *  W2XRegistration.java
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
 *  Version 2.0 (2018-03-26) 
 */ 
 
package org.openoffice.da.comp.writer2xhtml;

import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XSingleServiceFactory;
import com.sun.star.registry.XRegistryKey;

import com.sun.star.comp.loader.FactoryHelper;

/** This class provides a static method to instantiate our uno components
 * on demand (__getServiceFactory()), and a static method to give
 * information about the components (__writeRegistryServiceInfo()).
 * Furthermore, it saves the XMultiServiceFactory provided to the
 * __getServiceFactory method for future reference by the componentes.
 */
public class W2XRegistration {
    
    public static XMultiServiceFactory xMultiServiceFactory;

    /**
     * Returns a factory for creating the service.
     * This method is called by the <code>JavaLoader</code>
     *
     * @return  returns a <code>XSingleServiceFactory</code> for creating the
     *          component
     *
     * @param   implName     the name of the implementation for which a
     *                       service is desired
     * @param   multiFactory the service manager to be used if needed
     * @param   regKey       the registryKey
     *
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static XSingleServiceFactory __getServiceFactory(String implName,
        XMultiServiceFactory multiFactory, XRegistryKey regKey) {
        xMultiServiceFactory = multiFactory;
        XSingleServiceFactory xSingleServiceFactory = null;
        if (implName.equals(Writer2xhtml.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Writer2xhtml.class,
            Writer2xhtml.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ConfigurationDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ConfigurationDialog.class,
            ConfigurationDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ToolbarSettingsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ToolbarSettingsDialog.class,
            ToolbarSettingsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        
        return xSingleServiceFactory;
    }
    
    /**
     * Writes the service information into the given registry key.
     * This method is called by the <code>JavaLoader</code>
     * <p>
     * @return  returns true if the operation succeeded
     * @param   regKey       the registryKey
     * @see                  com.sun.star.comp.loader.JavaLoader
     */
    public static boolean __writeRegistryServiceInfo(XRegistryKey regKey) {
        return
            FactoryHelper.writeRegistryServiceInfo(Writer2xhtml.__implementationName,
                        Writer2xhtml.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(ConfigurationDialog.__implementationName,
                ConfigurationDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(ToolbarSettingsDialog.__implementationName,
                ToolbarSettingsDialog.__serviceName, regKey);
    }
}

