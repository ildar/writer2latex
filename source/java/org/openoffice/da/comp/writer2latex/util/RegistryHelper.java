/**
 *  RegistryHelper.java
 *
 *  Copyright: 2002-2009 by Henrik Just
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
 *  Version 1.2 (2009-05-01)
 *
 * 
 */

package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.beans.PropertyValue;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;


/** This class defines convenience methods to access the OOo registry
 *  using a given base path 
 */
public class RegistryHelper {
	
	private XComponentContext xContext;
	
	/** Construct a new RegistryHelper using a given component context
	 * 
	 * @param xContext the context to use to create new services
	 */
	public RegistryHelper(XComponentContext xContext) {
		this.xContext = xContext;
	}
	
    /** Get a registry view relative to the given path
     * 
     * @param sPath the base path within the registry
     * @param bUpdate true if we need update access
     * @return the registry view
     * @throws com.sun.star.uno.Exception
     */
    public Object getRegistryView(String sPath, boolean bUpdate) 
        throws com.sun.star.uno.Exception {
        //Object provider = xMSF.createInstance(
        Object provider = xContext.getServiceManager().createInstanceWithContext(
            "com.sun.star.configuration.ConfigurationProvider", xContext);
        XMultiServiceFactory xProvider = (XMultiServiceFactory)
            UnoRuntime.queryInterface(XMultiServiceFactory.class,provider);
        PropertyValue[] args = new PropertyValue[1];
        args[0] = new PropertyValue();
        args[0].Name = "nodepath";
        args[0].Value = sPath;
        String sServiceName = bUpdate ?
            "com.sun.star.configuration.ConfigurationUpdateAccess" :
            "com.sun.star.configuration.ConfigurationAccess";
        Object view = xProvider.createInstanceWithArguments(sServiceName,args);
        return view;
    }
	
    /** Dispose a previously obtained registry view
     * 
     * @param view the view to dispose
     */
    public void disposeRegistryView(Object view) {
        XComponent xComponent = (XComponent)
            UnoRuntime.queryInterface(XComponent.class,view);
        xComponent.dispose();
    }

}
