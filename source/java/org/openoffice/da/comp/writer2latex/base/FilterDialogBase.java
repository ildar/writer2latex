/************************************************************************
 *
 *  FilterDialogBase.java
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
 *  Version 2.0 (2022-05-10)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex.base;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import com.sun.star.awt.XDialogEventHandler;
import com.sun.star.beans.PropertyValue;
import com.sun.star.beans.XPropertyAccess;
import com.sun.star.beans.XPropertySet;
import com.sun.star.container.XNameAccess;
import com.sun.star.document.XDocumentPropertiesSupplier;
import com.sun.star.frame.XDesktop;
import com.sun.star.lang.XComponent;
import com.sun.star.lang.XMultiServiceFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.lang.XServiceName;
import com.sun.star.lang.XTypeProvider;
import com.sun.star.ui.dialogs.XExecutableDialog;
import com.sun.star.uno.Type;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;
import com.sun.star.util.XChangesBatch;

import org.openoffice.da.comp.writer2latex.util.DialogBase;
import org.openoffice.da.comp.writer2latex.util.MacroExpander;
import org.openoffice.da.comp.writer2latex.util.PropertyHelper;
import org.openoffice.da.comp.writer2latex.util.XPropertySetHelper;

import org.json.JSONException;
import org.json.JSONObject;

import writer2latex.api.Config;
import writer2latex.api.ConverterFactory;

/** This class provides an abstract uno component which implements a filter ui
 */
public abstract class FilterDialogBase extends DialogBase implements
        XPropertyAccess { // Filter ui requires XExecutableDialog + XPropertyAccess
	
    //////////////////////////////////////////////////////////////////////////
    // The subclass must override the following; and override the
    // implementation of XDialogEventHandler if needed
	
    /** Load settings from the registry to the dialog
     *  The subclass must implement this
     */
    protected abstract void loadSettings(XPropertySet xRegistryProps);
	
    /** Save settings from the dialog to the registry and create FilterData
     *  The subclass must implement this
     */
    protected abstract void saveSettings(XPropertySet xRegistryProps, PropertyHelper filterData);
	
	/** Return the name of the library containing the dialog
     */
    public abstract String getDialogLibraryName();
	
    /** Return the name of the dialog within the library
     */
    public abstract String getDialogName();

    /** Return the path to the options in the registry */
    public abstract String getRegistryPath();
    
    /** Return the MIME type handled by the converter */
    protected abstract String getMIME();
	
    /** Create a new OptionsDialogBase */
    public FilterDialogBase(XComponentContext xContext) {
        super(xContext);
        this.xMSF = null; // must be set properly by subclass
    }

    //////////////////////////////////////////////////////////////////////////
    // Implement some methods required by DialogBase
	
    /** Initialize the dialog (eg. with settings from the registry)
     */
    public void initialize() {
        try {
            // Prepare registry view
            Object view = getRegistryView(false);
            XPropertySet xProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,view);
            
            // Load common settings (configuration and parameters)
            loadCommon(xProps);
            updateParameters();

            // The subclass must take care of the rest
            loadSettings(xProps);

            // Dispose the registry view
            disposeRegistryView(view);
        }
        catch (com.sun.star.uno.Exception e) {
            // Failed to get registry view
        }
    }
	
    /** Finalize the dialog after execution (eg. save settings to the registry)
     */
    public void endDialog() {
        try {
            // Prepare registry view
            Object rwview = getRegistryView(true);
            XPropertySet xProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,rwview);

            // Save common settings and create FilterData
            PropertyHelper filterData = new PropertyHelper();
            saveCommon(xProps, filterData);
            // The subclass takes care of the rest
            saveSettings(xProps, filterData);

            // Commit registry changes
            XChangesBatch  xUpdateContext = (XChangesBatch)
                UnoRuntime.queryInterface(XChangesBatch.class,rwview);
            try {
                xUpdateContext.commitChanges();
            }
            catch (Exception e) {
                // ignore
            }

            // Dispose the registry view
            disposeRegistryView(rwview);

            // Update the media properties with the FilterData
            PropertyHelper helper = new PropertyHelper(mediaProps);
            helper.put("FilterData",filterData.toArray());
            mediaProps = helper.toArray();
        }
        catch (com.sun.star.uno.Exception e) {
            // Failed to get registry view
        }
    }


    //////////////////////////////////////////////////////////////////////////
    // Some private global variables

    // The service factory
    protected XMultiServiceFactory xMSF;
    
    // The media properties (set/get via XPropertyAccess implementation) 
    private PropertyValue[] mediaProps = null;
	
    // Set of locked controls (as read from config)
    private HashSet<String> lockedOptions = new HashSet<String>();
	
    // Some data to connect list boxes in filter UI with config 
    
    // The configuration names are identified by item number in list box
    private String[] sConfigNames = null;
    
    // Configuration parameter names likewise, the mapping being
    // config item -> param item -> param name
    private Map<Short,String[]> paramNames = new HashMap<Short,String[]>();
    private Map<Short,String[]> paramDisplayNames = new HashMap<Short,String[]>();
    
    // And finally parameter values likewise, the mapping being
    // config item -> param item -> param value item -> param value name
    private Map<Short, Map<Short,String[]>> paramValues = new HashMap<Short,Map<Short,String[]>>();
    private Map<Short, Map<Short,String[]>> paramDisplayValues = new HashMap<Short,Map<Short,String[]>>();
    
    // Current value of configuration names and parameters
    private Map<Short,Short> currentParamNames = new HashMap<Short,Short>();
    private Map<Short,Map<Short,Short>> currentParamValues = new HashMap<Short,Map<Short,Short>>();
    
    ///////////////////////////////////////////////////////////
    // Load and save common options (configuration and parameters)
	
    // Load common settings from registry
    private void loadCommon(XPropertySet xProps) {
        // Get name access to all configuration nodes in the registry
        Object configurations = XPropertySetHelper.getPropertyValue(xProps,"Configurations");
        XNameAccess xConfigurations = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,configurations);
        
        // Get all configuration names and sort the by name
        sConfigNames = xConfigurations.getElementNames();
        Arrays.sort(sConfigNames);
        int nConfigs = sConfigNames.length;

        // Make array for display names
        String[] sConfigs = new String[nConfigs];
        
    	// Then iterate over all configurations in the registry
        for (short nConfigItem=0; nConfigItem<nConfigs; nConfigItem++) {
            try {
                // Get the node for this configuration
            	Object regconfig = xConfigurations.getByName(sConfigNames[nConfigItem]);
                XPropertySet xCfgProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,regconfig);
                
                // Get and store the display name
                sConfigs[nConfigItem] = XPropertySetHelper.getPropertyValueAsString(xCfgProps,"DisplayName");
                
                // Get the configuration URL and use it to load parameters
                loadParameters(xProps, nConfigItem, XPropertySetHelper.getPropertyValueAsString(xCfgProps,"ConfigURL"));
            }
            catch (Exception e) {
            	// Errors in registry?
                sConfigs[nConfigItem] = "??";
                loadParameters(xProps, nConfigItem, "");
            }
        }
        
        // Load display names for parameters
        loadParameterDisplayNames(xProps);
        
        // Populate the config list box and select an item 
        setListBoxStringItemList("Config",sConfigs);
        adjustListBoxVisibleItems("Config");
        selectConfig(xProps);
        
                
        // Get current parameter values from registry
        createDefaultCurrentParameterValues();
        loadCurrentParameterValues(xProps);
    }
    
    // Load parameters from registry
    private void loadParameters(XPropertySet xProps, short nConfig, String sConfigURL) {
    	// Load the configuration from the URL and get the parameters
        ConverterHelper helper = new ConverterHelper(xContext);
        Config config = ConverterFactory.createConfig(getMIME());
        helper.readConfig(sConfigURL, config);
        Map<String,List<String>> configParameters = config.getParameters();
        
        // Get the parameter names from the config and store them
        String[] sParamNames = configParameters.keySet().toArray(new String[0]);
        paramNames.put(nConfig, sParamNames);
        
        // Get the parameter values from the config and store them
        paramValues.put(nConfig, new HashMap<Short, String[]>());
        int nParamCount = sParamNames.length; 
        for (short nParam=0; nParam<nParamCount; nParam++) {
        	String[] sParamValues = configParameters.get(sParamNames[nParam]).toArray(new String[0]);
        	paramValues.get(nConfig).put(nParam, sParamValues);
        }
    }
    
    private void loadParameterDisplayNames(XPropertySet xProps) {
    	Map<String,String> displayNames = new HashMap<String,String>();
    	
        // Get name access to all parameter display names in the registry
        Object templates = XPropertySetHelper.getPropertyValue(xProps,"ParameterStrings");
        XNameAccess xParameters = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,templates);
        String[] sNames = xParameters.getElementNames();
        
        // Iterate over the parameters
        for (int i=0; i<sNames.length; i++) {
            try {
                Object parameter = xParameters.getByName(sNames[i]);
                XPropertySet xParProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,parameter);
                String sDisplayName = XPropertySetHelper.getPropertyValueAsString(xParProps,"DisplayName");
                displayNames.put(sNames[i], sDisplayName);
            }
            catch (Exception e) {
            	// ignore, we will do without display name
            }
        }
        
        // Collect display names
        for (Short nConfigItem : paramNames.keySet()) { // iterate over all configs
        	
        	// For parameter names
        	String[] sParamNames = paramNames.get(nConfigItem);
        	int nParamCount = sParamNames.length;
        	String[] sParamDisplayNames = new String[nParamCount];
        	for (int i=0; i<nParamCount; i++) { // iterate over all parameter names for this config
        		if (displayNames.containsKey(sParamNames[i])) {
        			sParamDisplayNames[i]=displayNames.get(sParamNames[i]);
        		}
        		else {
        			sParamDisplayNames[i]=sParamNames[i];        			
        		}
        	}
        	paramDisplayNames.put(nConfigItem, sParamDisplayNames);
        	
        	// For parameter values HashMap<Short,Map<Short,String[]>>();
        	Map<Short,String[]> paramValueMap = new HashMap<Short,String[]>();
        	
        	for (short nParamNameItem : paramValues.get(nConfigItem).keySet()) { // iterate over all parameter names
        		String[] sParamValues = paramValues.get(nConfigItem).get(nParamNameItem);
            	int nParamValueCount = sParamValues.length;
            	String[] sParamDisplayValues = new String[nParamValueCount];
            	for (int i=0; i<nParamValueCount; i++) { // iterate over all parameter values for this parameter
            		if (displayNames.containsKey(sParamValues[i])) {
            			sParamDisplayValues[i]=displayNames.get(sParamValues[i]);
            		}
            		else {
            			sParamDisplayValues[i]=sParamValues[i];        			
            		}
            	}
            	paramValueMap.put(nParamNameItem, sParamDisplayValues);
        	}
        	
        	paramDisplayValues.put(nConfigItem, paramValueMap);
        }
        
    }

    private void createDefaultCurrentParameterValues() {
    	int nConfigs = sConfigNames.length;
    	for (short nConfig=0; nConfig<nConfigs; nConfig++) {
	        currentParamNames.put(nConfig, (short)0);
	        int nParamCount = paramValues.get(nConfig).size();
	        Map<Short,Short> thisParamValues = new HashMap<Short,Short>();
	        for (short nParam=0; nParam<nParamCount; nParam++) {
	        	thisParamValues.put(nParam, (short)0);
	        }
	        currentParamValues.put(nConfig, thisParamValues);    	
    	}
    }
    
    // Load current parameter values from registry
    private void loadCurrentParameterValues(XPropertySet xProps) {
        String sParameters = XPropertySetHelper.getPropertyValueAsString(xProps,"Parameters");
		JSONObject obj = null;
		try {
			obj = new JSONObject(sParameters);
			for (Object config : obj.keySet()) {
				if (config instanceof String) {
					String sConfig = (String)config;
					short nConfig = getConfigIndex(sConfig);
					if (nConfig>-1 && obj.get(sConfig) instanceof JSONObject) {
						JSONObject paramObj = (JSONObject) obj.get(sConfig);
						// Iterate over parameters for this config
			    		for (short nParam=0; nParam<paramNames.get(nConfig).length; nParam++) {
			    			String sParamName = paramNames.get(nConfig)[nParam];
			    			if (paramObj.has(sParamName)) {
			    				String sParamValue = paramObj.getString(sParamName);
			    				String[] sParamValues = paramValues.get(nConfig).get(nParam);
			    				for (short i = 0; i<sParamValues.length; i++) {
			    					if (sParamValues[i].equals(sParamValue)) {
			    						currentParamValues.get(nConfig).put(nParam,i);
			    					}
			    				}
			    			}
			    		}
					}
				}
			}
		} catch (JSONException e) {
			// Bad format of registry value, settings are ignored
		}    	
    }
    
    // Select the configuration in the dialog based on template or registry
    private void selectConfig(XPropertySet xProps) {
        // Get the template name for the current document
        String sTheTemplateName = getTemplateName();
        
        // Get name access to all template declarations in the registry
        Object templates = XPropertySetHelper.getPropertyValue(xProps,"Templates");
        XNameAccess xTemplates = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,templates);
        String[] sTemplateNames = xTemplates.getElementNames();
        
        // Iterate over the template declarations to find a possible match
        for (int i=0; i<sTemplateNames.length; i++) {
            try {
                Object template = xTemplates.getByName(sTemplateNames[i]);
                XPropertySet xTplProps = (XPropertySet)
                    UnoRuntime.queryInterface(XPropertySet.class,template);
                String sTemplateName = XPropertySetHelper.getPropertyValueAsString(xTplProps,"TemplateName");
                if (sTemplateName.equals(sTheTemplateName)) {
                    String sConfigName = XPropertySetHelper.getPropertyValueAsString(xTplProps,"ConfigName");
                    short nConfig = getConfigIndex(sConfigName);
                    if (nConfig>-1) {
                        setListBoxSelectedItem("Config", nConfig);
                        return;
                    }
                }
            }
            catch (Exception e) {
                // ignore
            }
        }

        // If there is no matching template, select item based on value stored in registry
        setListBoxSelectedItem("Config",(short)0);
        String sConfigName = XPropertySetHelper.getPropertyValueAsString(xProps,"ConfigName");
        Short nItem = getConfigIndex(sConfigName);
        if (nItem>-1) {
        	setListBoxSelectedItem("Config",nItem);
        }        
    }
    
    // Get the configuration index from the name
    private short getConfigIndex(String sConfigName) {
    	int nConfigs = sConfigNames.length;
        for (short i=0; i<nConfigs; i++) {
            if (sConfigNames[i].equals(sConfigName)) {
                return i;
            }
        }
        return (short)-1;
    }
    
    // Save common options to registry and filter data
    private void saveCommon(XPropertySet xProps, PropertyHelper filterData) {
    	saveConfig(xProps);
    	saveConfigFilterData(xProps,filterData);
    	saveParameters(xProps);
    	saveParametersFilterData(filterData);
    }
    
    // Save the current configuration index and name in the registry 
    private void saveConfig(XPropertySet xProps) {
        short nConfig = getListBoxSelectedItem("Config");
    	XPropertySetHelper.setPropertyValue(xProps,"Config",nConfig);
		XPropertySetHelper.setPropertyValue(xProps,"ConfigName",sConfigNames[nConfig]);
    }
    
    // Set ConfigURL and TemplateURL in filter data
    private void saveConfigFilterData(XPropertySet xProps, PropertyHelper filterData) {
    	// Get name access to the Configurations node in the registry
        Object configurations = XPropertySetHelper.getPropertyValue(xProps,"Configurations");
        XNameAccess xNameAccess = (XNameAccess)
            UnoRuntime.queryInterface(XNameAccess.class,configurations);

        // Get the ConfigURL and the TemplateURL and store them to filter data
    	try {
    		Object config = xNameAccess.getByName(sConfigNames[getListBoxSelectedItem("Config")]);
    		XPropertySet xCfgProps = (XPropertySet)
    				UnoRuntime.queryInterface(XPropertySet.class,config);
    		MacroExpander expander = new MacroExpander(xContext);
    		filterData.put("ConfigURL",expander.expandMacros(XPropertySetHelper.getPropertyValueAsString(xCfgProps,"ConfigURL")));
    		filterData.put("TemplateURL",expander.expandMacros(XPropertySetHelper.getPropertyValueAsString(xCfgProps,"TargetTemplateURL")));
    	}
    	catch (Exception e) {
    	}
    }

    private void saveParameters(XPropertySet xProps) {
    	// Save current value of all parameters for all configurations
    	// in the registry. To keep the registry simple, we use JSON
    	JSONObject obj = new JSONObject();
    	// Iterate over all configurations
    	for (short nConfig=0; nConfig<sConfigNames.length; nConfig++) {
    		JSONObject configObj = new JSONObject();
    		// Iterate over all parameters in this configuration
    		for (short nParam=0; nParam<paramNames.get(nConfig).length; nParam++) {
    			// Get the current value and store it 
    			int nParamValue = currentParamValues.get(nConfig).get(nParam);
    			String sParamName = paramNames.get(nConfig)[nParam];
    			String sParamValue = paramValues.get(nConfig).get(nParam)[nParamValue];
    			configObj.put(sParamName, sParamValue);
    		}
    		obj.put(sConfigNames[nConfig], configObj);
    	}
    	// Save the JSON object in registry
    	XPropertySetHelper.setPropertyValue(xProps,"Parameters",obj.toString());
    }
    
    private void saveParametersFilterData(PropertyHelper filterData) {
    	// Save parameter values for selected configuration to filter data
        short nConfig = getListBoxSelectedItem("Config");
        int nParamCount = paramNames.get(nConfig).length;
        for (short i=0; i<nParamCount; i++) {
        	String sName = paramNames.get(nConfig)[i];
        	String sValue = paramValues.get(nConfig).get(i)[currentParamValues.get(nConfig).get(i)];
        	filterData.put("param:"+sName, sValue);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // Some private utility methods
	
    // Get the template name from the document with ui focus
    private String getTemplateName() {
        try {
            // Get current component
            Object desktop = xContext.getServiceManager()
                .createInstanceWithContext("com.sun.star.frame.Desktop",xContext);
            XDesktop xDesktop = (XDesktop) UnoRuntime.queryInterface(XDesktop.class,desktop);
            XComponent xComponent = xDesktop.getCurrentComponent();
			
            // Get the document info property set
            XDocumentPropertiesSupplier xDocPropsSuppl =
                	UnoRuntime.queryInterface(XDocumentPropertiesSupplier.class, xComponent);
                return xDocPropsSuppl.getDocumentProperties().getTemplateName();
        }
        catch (Exception e) {
            return "";
        }
    }
	
    // Get a view of the options root in the registry
    private Object getRegistryView(boolean bUpdate) 
        throws com.sun.star.uno.Exception {
        Object provider = xMSF.createInstance(
            "com.sun.star.configuration.ConfigurationProvider");
        XMultiServiceFactory xProvider = (XMultiServiceFactory)
            UnoRuntime.queryInterface(XMultiServiceFactory.class,provider);
        PropertyValue[] args = new PropertyValue[1];
        args[0] = new PropertyValue();
        args[0].Name = "nodepath";
        args[0].Value = getRegistryPath();
        String sServiceName = bUpdate ?
            "com.sun.star.configuration.ConfigurationUpdateAccess" :
            "com.sun.star.configuration.ConfigurationAccess";
        Object view = xProvider.createInstanceWithArguments(sServiceName,args);
        return view;
    }
	
    // Dispose a previously obtained registry view
    private void disposeRegistryView(Object view) {
        XComponent xComponent = (XComponent)
            UnoRuntime.queryInterface(XComponent.class,view);
        xComponent.dispose();
    }

    //////////////////////////////////////////////////////////////////////////
    // Implement uno interfaces
	
    // Override getTypes() from the interface XTypeProvider
    public Type[] getTypes() {
        Type[] typeReturn = {};
        try {
            typeReturn = new Type[] {
            new Type( XServiceName.class ),
            new Type( XServiceInfo.class ),
            new Type( XTypeProvider.class ),
            new Type( XExecutableDialog.class ),
            new Type( XPropertyAccess.class ),
            new Type( XDialogEventHandler.class ) };
        } catch(Exception exception) {
        }
        return typeReturn;
    }


    // Implement the interface XPropertyAccess
    public PropertyValue[] getPropertyValues() {
        return mediaProps;
    }
	
    public void setPropertyValues(PropertyValue[] props) { 
        mediaProps = props;
    }
	

    //////////////////////////////////////////////////////////////////////////
    // Various utility methods to be used by the sublasses
	
    // Helpers to load and save settings
	
    protected void updateLockedOptions() {
        lockedOptions.clear();
        short nItem = getListBoxSelectedItem("Config");
        // Get current configuration name
        String sName = sConfigNames[nItem];
		
        try {
            // Prepare registry view
            Object view = getRegistryView(false);
            XPropertySet xProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,view);
 		
            // Get the available configurations
            Object configurations = XPropertySetHelper.getPropertyValue(xProps,"Configurations");
            XNameAccess xConfigurations = (XNameAccess)
                UnoRuntime.queryInterface(XNameAccess.class,configurations);
			
            // Get the LockedOptions node from the desired configuration
            String sLockedOptions = "";
            Object config = xConfigurations.getByName(sName);
            XPropertySet xCfgProps = (XPropertySet)
                UnoRuntime.queryInterface(XPropertySet.class,config);
            sLockedOptions = XPropertySetHelper.getPropertyValueAsString(xCfgProps,"LockedOptions");
			
            // Dispose the registry view
            disposeRegistryView(view);
		
            // Feed lockedOptions with the comma separated list of options
            int nStart = 0;
            for (int i=0; i<sLockedOptions.length(); i++) {
                if (sLockedOptions.charAt(i)==',') {
                    lockedOptions.add(sLockedOptions.substring(nStart,i).trim());
                    nStart = i+1;
                }
            }
            if (nStart<sLockedOptions.length()) {
                lockedOptions.add(sLockedOptions.substring(nStart).trim());
            }
        }
        catch (Exception e) {
            // no options will be locked...
        }
        
    }    
	
    protected boolean isLocked(String sOptionName) {
        return lockedOptions.contains(sOptionName);
    }
	
    // Populate parameter list boxes based on current config
    protected void updateParameters() {
    	short nConfigItem = getListBoxSelectedItem("Config");
    	if (paramNames.get(nConfigItem).length>0) {
            setListBoxStringItemList("ParameterName", paramDisplayNames.get(nConfigItem));
            setListBoxSelectedItem("ParameterName", currentParamNames.get(nConfigItem));
            adjustListBoxVisibleItems("ParameterName");
        	loadParameterValues();
            setControlEnabled("ParameterName",true);
            setControlEnabled("ParameterValue",true);
    	}
    	else {
    		// No parameters
    		setListBoxStringItemList("ParameterName", new String[0]);
    		setListBoxStringItemList("ParameterValue", new String[0]);
            setControlEnabled("ParameterName",false);
            setControlEnabled("ParameterValue",false);
    	}
    }
    
    // save current parameter name
    protected void parameterNameChange() {
    	short nConfigItem = getListBoxSelectedItem("Config");
    	short nParamNameItem = getListBoxSelectedItem("ParameterName");
    	currentParamNames.put(nConfigItem, nParamNameItem);
    	loadParameterValues();
    }
    
    // Populate parameter value list box based on current parameter name
    protected void loadParameterValues() {
    	short nConfigItem = getListBoxSelectedItem("Config");
    	short nParamNameItem = getListBoxSelectedItem("ParameterName");
        setListBoxStringItemList("ParameterValue", paramDisplayValues.get(nConfigItem).get(nParamNameItem));
        setListBoxSelectedItem("ParameterValue", currentParamValues.get(nConfigItem).get(nParamNameItem));
        adjustListBoxVisibleItems("ParameterValue");
    }
	
    // save current parameter value
    protected void parameterValueChange() {
    	short nConfigItem = getListBoxSelectedItem("Config");
    	short nParamNameItem = getListBoxSelectedItem("ParameterName");
    	short nParamValueItem = getListBoxSelectedItem("ParameterValue");
    	currentParamValues.get(nConfigItem).put(nParamNameItem,nParamValueItem);
    }
    	
    // Check box option (boolean)
    protected boolean loadCheckBoxOption(XPropertySet xProps, String sName) {
        boolean bValue = XPropertySetHelper.getPropertyValueAsBoolean(xProps,sName);
        setCheckBoxStateAsBoolean(sName, bValue);
        return bValue;
    }
	
    protected boolean saveCheckBoxOption(XPropertySet xProps, String sName) {
        boolean bValue = getCheckBoxStateAsBoolean(sName);
        XPropertySetHelper.setPropertyValue(xProps, sName, bValue);
        return bValue;
    }

    protected boolean saveCheckBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        boolean bValue = saveCheckBoxOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, Boolean.toString(bValue));
        }
        return bValue;
    }
	
    // List box option
    protected short loadListBoxOption(XPropertySet xProps, String sName) {
        short nValue = XPropertySetHelper.getPropertyValueAsShort(xProps, sName);
        setListBoxSelectedItem(sName ,nValue);
        return nValue;
    }
	
    protected short saveListBoxOption(XPropertySet xProps, String sName) {
        short nValue = getListBoxSelectedItem(sName);
        XPropertySetHelper.setPropertyValue(xProps, sName, nValue);
        return nValue;
    }
	
    protected short saveListBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName, String[] sValues) {
        short nValue = saveListBoxOption(xProps, sName);
        if (!isLocked(sOptionName) && (nValue>=0) && (nValue<sValues.length)) {
        	filterData.put(sOptionName, sValues[nValue]);
        }
        return nValue;
    }
	
    // Combo box option
    protected String loadComboBoxOption(XPropertySet xProps, String sName) {
        String sValue = XPropertySetHelper.getPropertyValueAsString(xProps, sName);
        setComboBoxText(sName ,sValue);
        return sValue;
    }
	
    protected String saveComboBoxOption(XPropertySet xProps, String sName) {
        String sValue = getComboBoxText(sName);
        XPropertySetHelper.setPropertyValue(xProps, sName, sValue);
        return sValue;
    }
	
    protected String saveComboBoxOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        String sValue = saveComboBoxOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, sValue);
        }
        return sValue;
    }

    // Text Field option
    protected String loadTextFieldOption(XPropertySet xProps, String sName) {
        String sValue = XPropertySetHelper.getPropertyValueAsString(xProps, sName);
        setTextFieldText(sName ,sValue);
        return sValue;
    }
	
    protected String saveTextFieldOption(XPropertySet xProps, String sName) {
        String sValue = getTextFieldText(sName);
        XPropertySetHelper.setPropertyValue(xProps, sName, sValue);
        return sValue;
    }
	
    protected String saveTextFieldOption(XPropertySet xProps, PropertyHelper filterData,
        String sName, String sOptionName) {
        String sValue = saveTextFieldOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName, sValue);
        }
        return sValue;
    }

    // Numeric option
    protected int loadNumericOption(XPropertySet xProps, String sName) {
        int nValue = XPropertySetHelper.getPropertyValueAsInteger(xProps, sName);
        setNumericFieldValue(sName, nValue);
        return nValue;
    }
	
    protected int saveNumericOption(XPropertySet xProps, String sName) {
        int nValue = getNumericFieldValue(sName);
        XPropertySetHelper.setPropertyValue(xProps, sName, nValue);
        return nValue;
    }
	
    protected int saveNumericOptionAsPercentage(XPropertySet xProps,
        PropertyHelper filterData, String sName, String sOptionName) {
        int nValue = saveNumericOption(xProps, sName);
        if (!isLocked(sOptionName)) {
            filterData.put(sOptionName,Integer.toString(nValue)+"%");
        }
        return nValue;
    }
    
    // Adjust size of list box to actual contents
    protected void adjustListBoxVisibleItems(String sName) {
    	int nCount = getListBoxStringItemList(sName).length;
	    if (nCount<=12) {
	        setListBoxLineCount(sName,(short) (nCount));
	    }  
	    else {
	        setListBoxLineCount(sName,(short) 12);
	    }
    }

    			
}
