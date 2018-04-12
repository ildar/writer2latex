/************************************************************************
 *
 *  W2LRegistration.java
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
 *  Version 2.0 (2018-04-09) 
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import org.openoffice.da.comp.writer2latex.bibtex.BibTeXDialog;
import org.openoffice.da.comp.writer2latex.bibtex.BibliographyDialog;
import org.openoffice.da.comp.writer2latex.html5.Html5ConfigurationDialog;
import org.openoffice.da.comp.writer2latex.html5.Html5SettingsDialog;
import org.openoffice.da.comp.writer2latex.html5.XhtmlOptionsDialog;
import org.openoffice.da.comp.writer2latex.latex.ApplicationsDialog;
import org.openoffice.da.comp.writer2latex.latex.ConfigurationDialog;
import org.openoffice.da.comp.writer2latex.latex.LaTeXOptionsDialog;
import org.openoffice.da.comp.writer2latex.latex.LogViewerDialog;
import org.openoffice.da.comp.writer2latex.latex.TeXDetectService;
import org.openoffice.da.comp.writer2latex.latex.TeXImportFilter;
import org.openoffice.da.comp.writer2latex.latex.W2LStarMathConverter;

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
public class W2LRegistration {
    
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
        if (implName.equals(W2LExportFilter.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(W2LExportFilter.class,
            W2LExportFilter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(LaTeXOptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(LaTeXOptionsDialog.class,
            LaTeXOptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(XhtmlOptionsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(XhtmlOptionsDialog.class,
            XhtmlOptionsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(W2LStarMathConverter.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(W2LStarMathConverter.class,
            W2LStarMathConverter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ConfigurationDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ConfigurationDialog.class,
            ConfigurationDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(Html5ConfigurationDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Html5ConfigurationDialog.class,
            Html5ConfigurationDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(Writer2LaTeX.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Writer2LaTeX.class,
            Writer2LaTeX.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(TeXImportFilter.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(TeXImportFilter.class,
            TeXImportFilter.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(TeXDetectService.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(TeXDetectService.class,
            TeXDetectService.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(ApplicationsDialog.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(ApplicationsDialog.class,
            ApplicationsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(BibliographyDialog.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(BibliographyDialog.class,
            BibliographyDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(Html5SettingsDialog.__implementationName)) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(Html5SettingsDialog.class,
            Html5SettingsDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(LogViewerDialog.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(LogViewerDialog.class,
            LogViewerDialog.__serviceName,
            multiFactory,						    
            regKey);
        }
        else if (implName.equals(BibTeXDialog.__implementationName) ) {
            xSingleServiceFactory = FactoryHelper.getServiceFactory(BibTeXDialog.class,
            BibTeXDialog.__serviceName,
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
            FactoryHelper.writeRegistryServiceInfo(W2LExportFilter.__implementationName,
                W2LExportFilter.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(LaTeXOptionsDialog.__implementationName,
                LaTeXOptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(XhtmlOptionsDialog.__implementationName,
                XhtmlOptionsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(W2LStarMathConverter.__implementationName,
                W2LStarMathConverter.__serviceName, regKey) &
        	FactoryHelper.writeRegistryServiceInfo(ConfigurationDialog.__implementationName,
                ConfigurationDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(Html5ConfigurationDialog.__implementationName,
                Html5ConfigurationDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(Writer2LaTeX.__implementationName,
                Writer2LaTeX.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(TeXImportFilter.__implementationName,
                TeXImportFilter.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(TeXDetectService.__implementationName,
                TeXDetectService.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(ApplicationsDialog.__implementationName,
            	ApplicationsDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(BibliographyDialog.__implementationName,
                BibliographyDialog.__serviceName, regKey) &
            FactoryHelper.writeRegistryServiceInfo(Html5SettingsDialog.__implementationName,
                Html5SettingsDialog.__serviceName, regKey) & 
            FactoryHelper.writeRegistryServiceInfo(LogViewerDialog.__implementationName,
                LogViewerDialog.__serviceName, regKey) &    
        	FactoryHelper.writeRegistryServiceInfo(BibTeXDialog.__implementationName,
                BibTeXDialog.__serviceName, regKey);    
    }
}
