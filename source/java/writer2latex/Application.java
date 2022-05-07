/************************************************************************
 *
 *  Application.java
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
 *  Version 2.0 (2022-05-07) 
 *
 */
 
package writer2latex;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import writer2latex.api.Converter;
import writer2latex.api.ConverterFactory;
import writer2latex.api.ConverterResult;
import writer2latex.api.MIMETypes;

import writer2latex.util.Misc;

/**
 * <p>Command line utility to convert an OpenOffice.org Writer XML file into XHTML/LaTeX/BibTeX</p>
 * <p>The utility is invoked with the following command line:</p>
 * <pre>java -jar writer2latex.jar [options] source [target]</pre>
 * <p>Where the available options are
 * <ul>
 * <li><code>-latex</code>, <code>-bibtex</code>, <code>-html5</code>,
 * <li><code>-ultraclean</code>, <code>-clean</code>, <code>-formatted</code>,
 * <code>-cleanxhtml</code>
 * <li><code>-config[=]filename</code>
 * <li><code>-template[=]filename</code>
 * <li><code>-option[=]value</code>
 * </ul>
 * <p>where <code>option</code> can be any simple option known to Writer2LaTeX
 * (see documentation for the configuration file).</p>
 */
public final class Application {
	
    /* Based on command-line parameters. */
    private String sTargetMIME = MIMETypes.LATEX;
    private Vector<String> configFileNames = new Vector<String>();
    private String sTemplateFileName = null;
    private Hashtable<String,String> options = new Hashtable<String,String>();
    private String sSource = null;
    private String sTarget = null;

    /**
     *  Main method
     *
     *  @param  args  The argument passed on the command line.
     */
    public static final void main (String[] args){
        try {
        	//long time = System.currentTimeMillis();
            Application app = new Application();
            app.parseCommandLine(args);
            app.doConversion();
            //System.out.println("Total conversion time was "+(System.currentTimeMillis()-time)+" miliseconds");
        } catch (IllegalArgumentException ex) {
            String msg = ex.getMessage();
            showUsage(msg);
        }
    }
    
    private void doConversion() {
		sayHello();
        File source = getSource();
        File target = getTarget(source);
        Converter converter = getConverter();
        readTemplate(converter);
        readConfig(converter);
        setOptions(converter);
        performConversion(converter,source,target);
        System.out.println("Done!");
    }
    
    private void sayHello() {
        System.out.println();
        System.out.println("This is Writer2LaTeX, Version " 
        		+ ConverterFactory.getVersion()
        		+ " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        System.out.println("Starting conversion...");    	
    }
    
    private File getSource() {
    	File source = new File(sSource);
        if (!source.exists()) {
            System.out.println("I'm sorry, I can't find "+sSource);
            System.exit(1);
        }
        if (!source.canRead()) {
            System.out.println("I'm sorry, I can't read "+sSource);
            System.exit(1);
        }
        if (!source.isFile()) {
            System.out.println("I'm sorry, "+sSource+" is not a file");
            System.exit(1);        	
        }
        return source;
    }
    
    private File getTarget(File source) {
        File target;
        if (sTarget==null) {
            target = new File(source.getParent(),Misc.removeExtension(source.getName()));
        }
        else {
            target = new File(sTarget);
            if (sTarget.endsWith(File.separator)) {
                target = new File(target,Misc.removeExtension(source.getName()));
            }
        }
        return target;
    }
    
    private Converter getConverter() {
    	Converter converter = ConverterFactory.createConverter(sTargetMIME);
        if (converter==null) {
            System.out.println("Failed to create converter for "+sTargetMIME);
            System.exit(1);
        }
		return converter;
    }
    
    private void readTemplate(Converter converter) {
        if (sTemplateFileName!=null) {
            try {
                System.out.println("Reading template "+sTemplateFileName);
                byte [] templateBytes = Misc.inputStreamToByteArray(new FileInputStream(sTemplateFileName));
                converter.readTemplate(new ByteArrayInputStream(templateBytes));
            }
            catch (FileNotFoundException e) {
                System.out.println("--> This file does not exist!");
                System.out.println("    "+e.getMessage());
            }
            catch (IOException e) {
                System.out.println("--> Failed to read the template file!");
                System.out.println("    "+e.getMessage());
            }
        }
    }
    
    private void readConfig(Converter converter) {
        for (int i=0; i<configFileNames.size(); i++) {
            String sConfigFileName = (String) configFileNames.get(i);
            if (sConfigFileName.startsWith("*")) {
                sConfigFileName = sConfigFileName.substring(1);
                System.out.println("Reading default configuration "+sConfigFileName);
                try {
                    converter.getConfig().readDefaultConfig(sConfigFileName);
                }
                catch (IllegalArgumentException e) {
                    System.err.println("--> This configuration is unknown!");
                    System.out.println("    "+e.getMessage());
                }
            }
            else {
                System.out.println("Reading configuration file "+sConfigFileName);
                try {
                    byte[] configBytes = Misc.inputStreamToByteArray(new FileInputStream(sConfigFileName));
                    converter.getConfig().read(new ByteArrayInputStream(configBytes));
                }
                catch (IOException e) {
                    System.err.println("--> Failed to read the configuration!");
                    System.out.println("    "+e.getMessage());
                }
            }
        }
    }
    
    private void setOptions(Converter converter) {
        Enumeration<String> keys = options.keys();
        while (keys.hasMoreElements()) {
            String sKey = keys.nextElement();
            String sValue = (String) options.get(sKey);
            converter.getConfig().setOption(sKey,sValue);
        }
    }
    
    private void performConversion(Converter converter,File source, File target) {
        System.out.println("Converting "+source.getPath());
        ConverterResult dataOut = null;

        try {
            dataOut = converter.convert(source,target.getName());
        }
        catch (FileNotFoundException e) {
            System.out.println("--> The file "+source.getPath()+" does not exist!");
            System.out.println("    "+e.getMessage());
            System.exit(1);
        }
        catch (IOException e) {
            System.out.println("--> Failed to convert the file "+source.getPath()+"!");
            System.out.println("    "+e.getMessage());
            System.out.println("    Please make sure the file is in OpenDocument format");
            System.exit(1);
        }    	

        // TODO: Should do some further checking on the feasability of writing
        // the directory and the files.
        File targetDir = target.getParentFile();
        if (targetDir!=null && !targetDir.exists()) { targetDir.mkdirs(); }
        try {
            dataOut.write(targetDir);
        }
        catch (IOException e) {
            System.out.println("--> Error writing out file!");
            System.out.println("    "+e.getMessage());
            System.exit(1);
        }
    }
    
    private void parseCommandLine(String sArgs[])
        throws IllegalArgumentException {

        int i = 0;
		
        while (i<sArgs.length) {
            String sArg = getArg(i++,sArgs);
            if (sArg.startsWith("-")) { // found an option
                if ("-latex".equals(sArg)) { sTargetMIME = MIMETypes.LATEX; }
                else if ("-bibtex".equals(sArg)) { sTargetMIME = MIMETypes.BIBTEX; }
                else if ("-html5".equals(sArg)) { sTargetMIME = MIMETypes.HTML; }
                else if ("-ultraclean".equals(sArg)) { configFileNames.add("*ultraclean.xml"); }
                else if ("-clean".equals(sArg)) { configFileNames.add("*clean.xml"); }
                else if ("-formatted".equals(sArg)) { configFileNames.add("*formatted.xml"); }
                else if ("-cleanxhtml".equals(sArg)) { configFileNames.add("*cleanxhtml.xml"); }
                else { // option with argument
                    int j=sArg.indexOf("=");
                    String sArg2;
                    if (j>-1) { // argument is separated by =
                        sArg2 = sArg.substring(j+1);
                        sArg = sArg.substring(0,j);
                    }
                    else { // argument is separated by space
                        sArg2 = getArg(i++,sArgs);
                    }
                    if ("-config".equals(sArg)) { configFileNames.add(sArg2); }
                    else if ("-template".equals(sArg)) { sTemplateFileName = sArg2; }
                    else { // configuration option
                        options.put(sArg.substring(1),sArg2);
                    }
                }
            }
            else { // not an option, so this must be the source
                sSource = sArg;
                // Possibly followed by the target
                if (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs); 
                    if (sArgument.length()>0) { sTarget = sArgument; }
                }
                // Skip any trailing empty arguments and signal an error if there's more
                while (i<sArgs.length) {
                    String sArgument = getArg(i++,sArgs);
                    if (sArgument.length()>0) {
                        throw new IllegalArgumentException("I didn't expect "+sArgument+"?");
                    }
                }
            }
        }
        if (sSource==null) {
            throw new IllegalArgumentException("Please specify a source document/directory!");
        }
        // Parsing of command line ended successfully!
    }


    /**
     *  Extract the next argument from the array, while checking to see
     *  that the array size is not exceeded.  Throw a friendly error
     *  message in case the arg is missing.
     *
     *  @param  i     Argument index.
     *  @param  args  Array of command line arguments.
     *
     *  @return  The argument with the specified index.
     *
     *  @throws  IllegalArgumentException  If an argument is invalid.
     */
    private String getArg(int i, String args[])
        throws IllegalArgumentException {

        if (i < args.length) {
            return args[i];
        }
        else throw new
            IllegalArgumentException("I'm sorry, the commandline ended abnormally");
    }
	
    private static void showUsage(String msg) {
        System.out.println();
        System.out.println("This is Writer2LaTeX, Version " + ConverterFactory.getVersion() 
                           + " (" + ConverterFactory.getDate() + ")");
        System.out.println();
        if (msg != null) System.out.println(msg);
        System.out.println();
        System.out.println("Usage:");
        System.out.println("   java -jar <path>/writer2latex.jar <options> <source file/directory> [<target file/directory>]");
        System.out.println("where the available options are:");
        System.out.println("   -latex");
        System.out.println("   -bibtex");
        System.out.println("   -html5");
        System.out.println("   -template[=]<template file>");
        System.out.println("   -ultraclean");
        System.out.println("   -clean");
        System.out.println("   -formatted");
        System.out.println("   -cleanxhtml");
        System.out.println("   -config[=]<configuration file>");
        System.out.println("   -<configuration option>[=]<value>");
        System.out.println("See the documentation for the available configuration options");
    }


}
