/************************************************************************
 *
 *  ApplicationsDialog.java
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License version 2.1, as published by the Free Software Foundation.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston,
 *  MA  02111-1307  USA
 *
 *  Copyright: 2002-2015 by Henrik Just
 *
 *  All Rights Reserved.
 * 
 *  Version 1.6 (2015-05-19)
 *
 */ 
 
package org.openoffice.da.comp.writer2latex;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Vector;

import com.sun.star.awt.XContainerWindowEventHandler;
import com.sun.star.awt.XDialog;
import com.sun.star.awt.XDialogProvider2;
import com.sun.star.awt.XWindow;
import com.sun.star.lang.XMultiComponentFactory;
import com.sun.star.lang.XServiceInfo;
import com.sun.star.uno.AnyConverter;
import com.sun.star.uno.UnoRuntime;
import com.sun.star.uno.XComponentContext;

import com.sun.star.lib.uno.helper.WeakBase;

import org.openoffice.da.comp.w2lcommon.helper.DialogAccess;
import org.openoffice.da.comp.w2lcommon.helper.FilePicker;
import org.openoffice.da.comp.w2lcommon.helper.StreamGobbler;

/** This class provides a UNO component which implements the configuration
 *  of applications for the Writer2LaTeX toolbar
 */
public final class ApplicationsDialog
    extends WeakBase
    implements XServiceInfo, XContainerWindowEventHandler {

    private XComponentContext xContext;
    private FilePicker filePicker;
    
    private ExternalApps externalApps;
    
    /** The component will be registered under this name.
     */
    public static String __serviceName = "org.openoffice.da.writer2latex.ApplicationsDialog";

    /** The component should also have an implementation name.
     */
    public static String __implementationName = "org.openoffice.da.comp.writer2latex.ApplicationsDialog";

    /** Create a new ApplicationsDialog */
    public ApplicationsDialog(XComponentContext xContext) {
        this.xContext = xContext;
        externalApps = new ExternalApps(xContext);
        filePicker = new FilePicker(xContext);
    }
	
    // **** Implement XContainerWindowEventHandler
    
    @Override public boolean callHandlerMethod(XWindow xWindow, Object event, String sMethod)
        throws com.sun.star.lang.WrappedTargetException {
		XDialog xDialog = (XDialog)UnoRuntime.queryInterface(XDialog.class, xWindow);
		DialogAccess dlg = new DialogAccess(xDialog);

        try {
            if (sMethod.equals("external_event") ){
                return handleExternalEvent(dlg, event);
            }
            else if (sMethod.equals("AfterExportChange")) {
                return changeProcessingLevel(dlg);
            }
            else if (sMethod.equals("ApplicationChange")) {
                return changeApplication(dlg);
            }
            else if (sMethod.equals("UseDefaultChange")) {
            	return useDefaultChange(dlg) && updateApplication(dlg);
            }
            else if (sMethod.equals("BrowseClick")) {
                return browseForExecutable(dlg);
            }
            else if (sMethod.equals("ExecutableUnfocus")) {
                return updateApplication(dlg);
            }
            else if (sMethod.equals("OptionsUnfocus")) {
                return updateApplication(dlg);
            }
            else if (sMethod.equals("AutomaticClick")) {
                return autoConfigure(dlg);
            }
        }
        catch (com.sun.star.uno.RuntimeException e) {
            throw e;
        }
        catch (com.sun.star.uno.Exception e) {
            throw new com.sun.star.lang.WrappedTargetException(sMethod, this, e);
        }
        return false;
    }
	
    @Override public String[] getSupportedMethodNames() {
        String[] sNames = { "external_event", "AfterExportChange", "ApplicationChange", "BrowseClick", "ExecutableUnfocus", "OptionsUnfocus", "AutomaticClick" };
        return sNames;
    }
    
    // **** Implement the interface XServiceInfo

    @Override public boolean supportsService(String sServiceName) {
        return sServiceName.equals(__serviceName);
    }

    @Override public String getImplementationName() {
        return __implementationName;
    }
    
    @Override public String[] getSupportedServiceNames() {
        String[] sSupportedServiceNames = { __serviceName };
        return sSupportedServiceNames;
    }
	
    // **** Event handlers
    
    private boolean handleExternalEvent(DialogAccess dlg, Object aEventObject)
        throws com.sun.star.uno.Exception {
        try {
            String sMethod = AnyConverter.toString(aEventObject);
            if (sMethod.equals("ok")) {
                externalApps.save();
                return true;
            } else if (sMethod.equals("back") || sMethod.equals("initialize")) {
                externalApps.load();
                updateProcessingLevel(dlg);
                return changeApplication(dlg);
            }
        }
        catch (com.sun.star.lang.IllegalArgumentException e) {
            throw new com.sun.star.lang.IllegalArgumentException(
            "Method external_event requires a string in the event object argument.", this,(short) -1);
        }
        return false;
    }
    
    private boolean changeProcessingLevel(DialogAccess dlg) {
    	externalApps.setProcessingLevel(dlg.getListBoxSelectedItem("AfterExport"));
    	return true;
    }
    
    private boolean updateProcessingLevel(DialogAccess dlg) {
    	dlg.setListBoxSelectedItem("AfterExport", externalApps.getProcessingLevel());
    	return true;
    }
	
    private boolean changeApplication(DialogAccess dlg) {
        String sAppName = getSelectedAppName(dlg);
        if (sAppName!=null) {
            String[] s = externalApps.getApplication(sAppName);
            dlg.setComboBoxText("Executable", s[0]);
            dlg.setComboBoxText("Options", s[1]);
            dlg.setCheckBoxStateAsBoolean("UseDefault", externalApps.getUseDefaultApplication(sAppName));
        	dlg.setControlEnabled("UseDefault", externalApps.isViewer(sAppName));
        	useDefaultChange(dlg);
        }
        return true;
    }
    
    private boolean useDefaultChange(DialogAccess dlg) {
        boolean bCustomApp = !dlg.getCheckBoxStateAsBoolean("UseDefault");
    	dlg.setControlEnabled("ExecutableLabel", bCustomApp);
    	dlg.setControlEnabled("Executable", bCustomApp);
    	dlg.setControlEnabled("OptionsLabel", bCustomApp);
    	dlg.setControlEnabled("Options", bCustomApp);
    	dlg.setControlEnabled("BrowseButton", bCustomApp);
    	return true;
    }
	
    private boolean browseForExecutable(DialogAccess dlg) {
    	String sPath = filePicker.getPath();
    	if (sPath!=null) {
    		try {
				dlg.setComboBoxText("Executable", new File(new URI(sPath)).getCanonicalPath());
			}
    		catch (IOException e) {
			}
    		catch (URISyntaxException e) {
			}
    		updateApplication(dlg);
    	}     
    	return true;
    }
	
    private boolean updateApplication(DialogAccess dlg) {
        String sAppName = getSelectedAppName(dlg);
        if (sAppName!=null) {
            externalApps.setApplication(sAppName, dlg.getComboBoxText("Executable"), dlg.getComboBoxText("Options"));
            externalApps.setUseDefaultApplication(sAppName, dlg.getCheckBoxStateAsBoolean("UseDefault"));
        }
        return true;
    }
    
    private boolean autoConfigure(DialogAccess dlg) {
		String sOsName = System.getProperty("os.name");
		String sOsVersion = System.getProperty("os.version");
		String sOsArch = System.getProperty("os.arch");
		StringBuilder info = new StringBuilder();
		info.append("Results of configuration:\n\n");
		info.append("Your system identifies itself as "+sOsName+" version "+sOsVersion+ " (" + sOsArch +")\n\n");
    	if (sOsName.startsWith("Windows")) {
    		autoConfigureWindows(dlg, info);
    	}
    	else {
    		autoConfigureUnix(dlg, info);
    	}
		displayAutoConfigInfo(info.toString());
    	changeApplication(dlg);
        return true;
    }
    
    private void displayAutoConfigInfo(String sText) {
    	XDialog xDialog = getDialog("W2LDialogs2.AutoConfigInfo");
    	if (xDialog!=null) {
    		DialogAccess info = new DialogAccess(xDialog);
    		info.setTextFieldText("Info", sText);
    		xDialog.execute();
    		xDialog.endExecute();
    	}
    }
    
    private XDialog getDialog(String sDialogName) {
    	XMultiComponentFactory xMCF = xContext.getServiceManager();
    	try {
    		Object provider = xMCF.createInstanceWithContext(
    				"com.sun.star.awt.DialogProvider2", xContext);
    		XDialogProvider2 xDialogProvider = (XDialogProvider2)
    		UnoRuntime.queryInterface(XDialogProvider2.class, provider);
    		String sDialogUrl = "vnd.sun.star.script:"+sDialogName+"?location=application";
    		return xDialogProvider.createDialogWithHandler(sDialogUrl, this);
    	}
    	catch (Exception e) {
    		return null;
    	}
     }
    
    private String getSelectedAppName(DialogAccess dlg) {
        short nItem = dlg.getListBoxSelectedItem("Application");
        //String sAppName = null;
        switch (nItem) {
            case 0: return ExternalApps.LATEX;
            case 1: return ExternalApps.PDFLATEX;
            case 2: return ExternalApps.XELATEX;
            case 3: return ExternalApps.DVIPS;
            case 4: return ExternalApps.BIBTEX;
            case 5: return ExternalApps.MAKEINDEX;
            //case 6: return ExternalApps.MK4HT;
            case 6: return ExternalApps.DVIVIEWER;
            case 7: return ExternalApps.PDFVIEWER;
            case 8: return ExternalApps.POSTSCRIPTVIEWER;
        }
        return "???";
    }
    
    // **** Automatic configuration of applications for Windows systems (assuming MikTeX)
    
    private void autoConfigureWindows(DialogAccess dlg, StringBuilder info) {
		String sMikTeXPath = getMikTeXPath();
		
		// Configure TeX and friends + yap (DVi viewer) + TeXworks (PDF viewer)
		boolean bFoundTexworks = false;
		if (sMikTeXPath!=null) {
			info.append("Found MikTeX\n");
			configureMikTeX(sMikTeXPath, ExternalApps.LATEX, "latex", "--interaction=batchmode %s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.PDFLATEX, "pdflatex", "--interaction=batchmode %s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.XELATEX, "xelatex", "--interaction=batchmode %s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.DVIPS, "dvips", "%s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.BIBTEX, "bibtex", "%s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.MAKEINDEX, "makeindex", "%s", info, true);
			//configureMikTeX(sMikTeXPath, ExternalApps.MK4HT, "mk4ht", "%c %s", info, true);
			configureMikTeX(sMikTeXPath, ExternalApps.DVIVIEWER, "yap", "--single-instance %s", info, true);
			externalApps.setUseDefaultApplication(ExternalApps.DVIVIEWER, false);
			// MikTeX 2.8 provides texworks for pdf viewing
			bFoundTexworks = configureMikTeX(sMikTeXPath, ExternalApps.PDFVIEWER, "texworks", "%s", info, true);
		}
		else {
			info.append("Failed to find MikTeX\n");
			info.append("Writer2LaTeX has been configured to work if MikTeX is added to your path\n");
			externalApps.setApplication(ExternalApps.LATEX, "latex", "--interaction=batchmode %s");
			externalApps.setApplication(ExternalApps.PDFLATEX, "pdflatex", "--interaction=batchmode %s");
			externalApps.setApplication(ExternalApps.XELATEX, "xelatex", "--interaction=batchmode %s");
			externalApps.setApplication(ExternalApps.DVIPS, "dvips", "%s");
			externalApps.setApplication(ExternalApps.BIBTEX, "bibtex", "%s");
			externalApps.setApplication(ExternalApps.MAKEINDEX, "makeindex", "%s");
			//externalApps.setApplication(ExternalApps.MK4HT, "mk4ht", "%c %s");
			externalApps.setUseDefaultApplication(ExternalApps.DVIVIEWER, true);
		}
		externalApps.setUseDefaultApplication(ExternalApps.PDFVIEWER, !bFoundTexworks);
		info.append("\n");
		
		// Configure gsview (PostScript viewer and fall back viewer for PDF)
		String sGsview = getGsviewPath();		
		if (sGsview!=null) {
			info.append("Found gsview - OK\n");
    		externalApps.setApplication(ExternalApps.POSTSCRIPTVIEWER, sGsview, "-e \"%s\"");  
    		if (!bFoundTexworks) {
    			externalApps.setApplication(ExternalApps.PDFVIEWER, sGsview, "-e \"%s\"");
    			externalApps.setUseDefaultApplication(ExternalApps.PDFVIEWER, false);
    		}
		}
		else {
			if (!bFoundTexworks) {				
				info.append("Using default application for PDF\n");
			}
			info.append("Using default application for PostScript\n");			
		}
		externalApps.setUseDefaultApplication(ExternalApps.POSTSCRIPTVIEWER, sGsview!=null);
	}
    
    // Windows: Get the path to the MikTeX bin directory
    private String getMikTeXPath() {
	    String[] sPaths = System.getenv("PATH").split(";");
		for (String s : sPaths) {
			if (s.toLowerCase().indexOf("miktex")>-1 && containsExecutable(s,"latex.exe")) {
				return s;
			}
		}
		for (String s : sPaths) {
			if (containsExecutable(s,"latex.exe")) {
				return s;
			}
		}
		return null;
    }
    
    // Windows: Get the path to the gsview executable
    private String getGsviewPath() {
		String sProgramFiles = System.getenv("ProgramFiles");
		if (sProgramFiles!=null) {
			if (containsExecutable(sProgramFiles+"\\ghostgum\\gsview","gsview32.exe")) {
				return sProgramFiles+"\\ghostgum\\gsview\\gsview32.exe";
			}
		}
		return null;
    }

    // Windows: Test that the given path contains a given executable
    private boolean containsExecutable(String sPath,String sExecutable) {
    	File dir = new File(sPath);
    	if (dir.exists() && dir.canRead()) {
    		File exe = new File(dir,sExecutable);
    		return exe.exists();
    	}
    	return false;
    }
	
    // Windows: Configure a certain MikTeX application
    private boolean configureMikTeX(String sPath, String sName, String sAppName, String sArguments, StringBuilder info, boolean bRequired) {
    	File app = new File(new File(sPath),sAppName+".exe");
    	if (app.exists()) {
    		externalApps.setApplication(sName, sAppName, sArguments);
			info.append("  Found "+sName+": "+sAppName+" - OK\n");
			return true;
    	}
    	else if (bRequired) {
    		externalApps.setApplication(sName, "???", "???");
			info.append("  Failed to find "+sName+"\n");    		
    	}
    	return false;
    }

    // **** Automatic configuration of applications for other systems (assuming unix-like systems)

    private void autoConfigureUnix(DialogAccess dlg, StringBuilder info) {
    	// Assume that the "which" command is supported
		configureApp(ExternalApps.LATEX, "latex", "--interaction=batchmode %s",info);
		configureApp(ExternalApps.PDFLATEX, "pdflatex", "--interaction=batchmode %s",info);
		configureApp(ExternalApps.XELATEX, "xelatex", "--interaction=batchmode %s",info);
		configureApp(ExternalApps.DVIPS, "dvips", "%s",info);
		configureApp(ExternalApps.BIBTEX, "bibtex", "%s",info);
		configureApp(ExternalApps.MAKEINDEX, "makeindex", "%s",info);
		//configureApp(ExternalApps.MK4HT, "mk4ht", "%c %s",info);    		
		// We have several possible viewers
		String[] sDviViewers = {"evince", "okular", "xdvi"};
		configureViewer(ExternalApps.DVIVIEWER, sDviViewers, "%s",info);
		String[] sPdfViewers =  {"evince", "okular", "xpdf"};
		configureViewer(ExternalApps.PDFVIEWER, sPdfViewers, "%s",info);
		String[] sPsViewers =  {"evince", "okular", "ghostview"};
		configureViewer(ExternalApps.POSTSCRIPTVIEWER, sPsViewers, "%s",info);
		
    	// Maybe add some info for Debian/Ubuntu users, e.g.
    	// sudo apt-get install texlive
    	// sudo apt-get install texlive-xetex
    	// sudo apt-get install texlive-latex-extra
    	// sudo apt-get install tex4ht
    }
	
    // Unix: Test to determine whether a certain application is available in the OS
    // Requires "which", hence Unix only
    private boolean hasApp(String sAppName) {
        try {
			Vector<String> command = new Vector<String>();
			command.add("which");
			command.add(sAppName);
			
            ProcessBuilder pb = new ProcessBuilder(command);
            Process proc = pb.start();        

            // Gobble the error stream of the application
            StreamGobbler errorGobbler = new 
                StreamGobbler(proc.getErrorStream(), "ERROR");            
            
            // Gobble the output stream of the application
            StreamGobbler outputGobbler = new 
                StreamGobbler(proc.getInputStream(), "OUTPUT");
                
            errorGobbler.start();
            outputGobbler.start();
                                    
            // The application exists if the process exits with 0
            return proc.waitFor()==0;
        }
        catch (InterruptedException e) {
            return false;
        }
        catch (IOException e) {
            return false;
        }
    }
    
    // Unix: Configure a certain application, testing and reporting the availability
    private boolean configureApp(String sName, String sAppName, String sArguments, StringBuilder info) {
    	if (hasApp(sAppName)) {
    		externalApps.setApplication(sName, sAppName, sArguments);
    		externalApps.setUseDefaultApplication(sName, false);
    		if (info!=null) {
    			info.append("Found "+sAppName+" - OK\n");
    		}
    		return true;
    	}
    	else {
    		externalApps.setApplication(sName, "???", "???");
    		externalApps.setUseDefaultApplication(sName, false);
    		if (info!=null) {
    			info.append("Failed to find "+sAppName+"\n");
    		}
    		return false;
    	}
    }
    
    // Unix: Configure a certain application, testing and reporting the availability
    // This variant uses an array of potential apps
    private boolean configureViewer(String sName, String[] sAppNames, String sArguments, StringBuilder info) {
    	for (String sAppName : sAppNames) {
    		if (configureApp(sName, sAppName, sArguments, null)) {
    			info.append("Found "+sName+": "+sAppName+" - OK\n");
    			return true;
    		}
    	}
    	externalApps.setUseDefaultApplication(sName, true);
    	info.append("Using default application as "+sName+"\n");
    	return true;
    }
    
}

