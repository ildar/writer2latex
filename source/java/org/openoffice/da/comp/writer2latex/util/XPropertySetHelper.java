/************************************************************************
 *
 *  XPropertySetHelper.java
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
package org.openoffice.da.comp.writer2latex.util;

import com.sun.star.beans.PropertyVetoException;
import com.sun.star.beans.UnknownPropertyException;
import com.sun.star.beans.XPropertySet;
import com.sun.star.lang.IllegalArgumentException;
import com.sun.star.lang.WrappedTargetException;

/** Helper class providing staic convenience methods for accesing an XPropertySet
 * The helpers will fail silently if names or data is provided, but the user is expected to
 * apply them with correct data only...
 */
public class XPropertySetHelper {

	public static Object getPropertyValue(XPropertySet xProps, String sName) {
        try {
            return xProps.getPropertyValue(sName);
        }
        catch (UnknownPropertyException e) {
            return null;
        }
        catch (WrappedTargetException e) {
            return null;
        }
    } 
	
    public static void setPropertyValue(XPropertySet xProps, String sName, Object value) {
        try {
            xProps.setPropertyValue(sName,value);
        }
        catch (UnknownPropertyException e) {
        }
        catch (PropertyVetoException e) { // unacceptable value
        }
        catch (IllegalArgumentException e) {
        }
        catch (WrappedTargetException e) {
        }
    }
	
    public static String getPropertyValueAsString(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof String ? (String) value : "";
    }
	
    public static int getPropertyValueAsInteger(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Integer ? ((Integer) value).intValue() : 0;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, int nValue) {
        setPropertyValue(xProps,sName,new Integer(nValue));
    }

    public static short getPropertyValueAsShort(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Short ? ((Short) value).shortValue() : 0;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, short nValue) {
        setPropertyValue(xProps,sName,new Short(nValue));
    }

    public static boolean getPropertyValueAsBoolean(XPropertySet xProps, String sName) {
        Object value = getPropertyValue(xProps,sName);
        return value instanceof Boolean ? ((Boolean) value).booleanValue() : false;
    }
	
    public static void setPropertyValue(XPropertySet xProps, String sName, boolean bValue) {
        setPropertyValue(xProps,sName,new Boolean(bValue));
    }



}
