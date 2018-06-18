/************************************************************************
 *
 *  ConverterPalette.java
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
 *  Version 2.0 (2018-06-14)
 *
 */

package writer2latex.latex;

import org.w3c.dom.Element;

import java.io.IOException;

import writer2latex.api.Config;
import writer2latex.api.ConverterFactory;
import writer2latex.base.ConverterBase;
import writer2latex.latex.i18n.ClassicI18n;
import writer2latex.latex.i18n.I18n;
import writer2latex.latex.i18n.XeTeXI18n;
import writer2latex.latex.util.Context;
import writer2latex.util.CSVList;
import writer2latex.util.ExportNameCollection;
import writer2latex.util.Misc;
import writer2latex.office.MIMETypes;
import writer2latex.office.StyleWithProperties;
import writer2latex.office.XMLString;

/** This class converts a Writer XML file to a LaTeX file
 */
public final class ConverterPalette extends ConverterBase {

    // Configuration
    private LaTeXConfig config;
	
    public Config getConfig() { return config; }

    // The main outfile
    private LaTeXDocument texDoc;
    
    // Various data used in conversion
    private Context mainContext; // main context
    private CSVList globalOptions; // global options
    private LaTeXDocumentPortion packages; // Document portion containing all \\usepackage commands

    // The helpers (the "colors" of the palette)
    private I18n i18n;
    private ColorConverter colorCv;
    private MicrotypeConverter microtypeCv;
    private FrameStyleConverter frameSc;
    private CharStyleConverter charSc;
    private HeadingStyleConverter headingSc;
    private PageStyleConverter pageSc;
    private BlockConverter blockCv;
    private ParConverter parCv;
    private HeadingConverter headingCv;
    private IndexConverter indexCv;
    private BibConverter bibCv;
    private SectionConverter sectionCv;
    private TableConverter tableCv;
    private ListConverter listCv;
    private NoteConverter noteCv;
    private CaptionConverter captionCv;
    private InlineConverter inlineCv;
    private FieldConverter fieldCv;
    private DrawConverter drawCv;
    private CustomShapeConverter customShapeCv;
    private MathConverter mathCv;
    private Info info;
	
    // Constructor
    public ConverterPalette() {
        super();
        config = new LaTeXConfig();
    }
	
    // Accessor methods for data
	
    public String getOutFileName() { return sTargetFileName; }

    public Context getMainContext() { return mainContext; }
	
    public void addGlobalOption(String sOption) {
	    globalOptions.addValue(sOption);
    }
    
    // Accessor methods for helpers
    public I18n getI18n() { return i18n; }
    public ColorConverter getColorCv() { return colorCv; }
    public MicrotypeConverter getMicrotypeCv() { return microtypeCv; }
    public FrameStyleConverter getFrameStyleSc() { return frameSc; }
    public CharStyleConverter getCharSc() { return charSc; }
    //public HeadingStyleConverter getHeadingSc() { return headingSc; }
    public PageStyleConverter getPageSc() { return pageSc; }
    public BlockConverter getBlockCv() { return blockCv; }
    public ParConverter getParCv() { return parCv; }
    public HeadingConverter getHeadingCv() { return headingCv; }
    public IndexConverter getIndexCv() { return indexCv; }
    public BibConverter getBibCv() { return bibCv; }
    public SectionConverter getSectionCv() { return sectionCv; }
    public TableConverter getTableCv() { return tableCv; }
    public ListConverter getListCv() { return listCv; }
    public NoteConverter getNoteCv() { return noteCv; }
    public CaptionConverter getCaptionCv() { return captionCv; }
    public InlineConverter getInlineCv() { return inlineCv; }
    public FieldConverter getFieldCv() { return fieldCv; }
    public DrawConverter getDrawCv() { return drawCv; }
    public CustomShapeConverter getCustomShapeCv() { return customShapeCv; }
    public MathConverter getMathCv() { return mathCv; }
    public Info getInfo() { return info; }
	
	
    // fill out inner converter method
    public void convertInner() throws IOException {
        sTargetFileName = Misc.trimDocumentName(sTargetFileName,".tex");
        String sSafeTargetFileName = new ExportNameCollection(true).getExportName(sTargetFileName);
        imageConverter.setBaseFileName(sSafeTargetFileName+"-img");
        if (config.saveImagesInSubdir()) {
        	imageConverter.setUseSubdir(sSafeTargetFileName+"-img");
        }
		
        // Set graphics formats depending on backend
        if (config.getBackend()==LaTeXConfig.PDFTEX || config.getBackend()==LaTeXConfig.XETEX) {
            imageConverter.setDefaultFormat(MIMETypes.PNG);
            imageConverter.setDefaultVectorFormat(MIMETypes.PDF);
            imageConverter.addAcceptedFormat(MIMETypes.JPEG);
        }
        else if (config.getBackend()==LaTeXConfig.DVIPS) {
            imageConverter.setDefaultFormat(MIMETypes.EPS);
        }
        // Other values: keep original format
		
        // Inject user sequence names for tables and figures into OfficeReader
        if (config.getTableSequenceName().length()>0) {
            ofr.addTableSequenceName(config.getTableSequenceName());
        }
        if (config.getFigureSequenceName().length()>0) {
            ofr.addFigureSequenceName(config.getFigureSequenceName());
        }
		
        // Create helpers
        if (config.getBackend()!=LaTeXConfig.XETEX) {
            i18n = new ClassicI18n(ofr,config,this);        	
        }
        else {
            i18n = new XeTeXI18n(ofr,config,this);        	        	
        }
        colorCv = new ColorConverter(ofr,config,this);
        microtypeCv = new MicrotypeConverter(ofr,config,this);
        frameSc = new FrameStyleConverter(ofr,config,this);
        charSc = new CharStyleConverter(ofr,config,this);
        headingSc = new HeadingStyleConverter(ofr,config,this);
        pageSc = new PageStyleConverter(ofr,config,this);
        blockCv = new BlockConverter(ofr,config,this);
        parCv = new ParConverter(ofr,config,this);
        headingCv = new HeadingConverter(ofr,config,this);
        indexCv = new IndexConverter(ofr,config,this);
        bibCv = new BibConverter(ofr,config,this);
        sectionCv = new SectionConverter(ofr,config,this);
        tableCv = new TableConverter(ofr,config,this);
        listCv = new ListConverter(ofr,config,this);
        noteCv = new NoteConverter(ofr,config,this);
        captionCv = new CaptionConverter(ofr,config,this);
        inlineCv = new InlineConverter(ofr,config,this);
        fieldCv = new FieldConverter(ofr,config,this);
        drawCv = new DrawConverter(ofr,config,this);
        customShapeCv = new CustomShapeConverter(ofr,config,this);
        mathCv = new MathConverter(ofr,config,this);
        info = new Info(ofr,config,this);

        // Create master document and add this
        this.texDoc = new LaTeXDocument(sTargetFileName,config.getWrapLinesAfter(),true);
        if (config.getBackend()!=LaTeXConfig.XETEX) {
            texDoc.setEncoding(ClassicI18n.writeJavaEncoding(config.getInputencoding()));        	
        }
        else {
            texDoc.setEncoding("UTF-8");        	
        	
        }
        converterResult.addDocument(texDoc);

        // Setup context.
        // The default language is specified in the default paragraph style:
        mainContext = new Context();
        mainContext.resetFormattingFromStyle(ofr.getDefaultParStyle());
        mainContext.setInMulticols(pageSc.isTwocolumn());
		
        // Create main LaTeXDocumentPortions
        LaTeXPacman packages = new LaTeXPacman(false);
        LaTeXDocumentPortion declarations = new LaTeXDocumentPortion(false);
        LaTeXDocumentPortion body = new LaTeXDocumentPortion(true);
        
        // Create additional data for the preamble
        globalOptions = new CSVList(',');

        // Traverse the content
        Element content = ofr.getContent();
        blockCv.traverseBlockText(content,body,mainContext);
        noteCv.insertEndnotes(body);

        // Add declarations from our helpers
        i18n.appendDeclarations(packages,declarations);
        	// common: usepackage tipa, tipx, bbding, ifsym, pifont, eurosym, amsmath, wasysym, amssymb, amsfonts
        	// classic: usepackage inputenc, babel, textcomp, fontenc, cmbright, ccfonts, eulervm, iwona, kurier, anttor,
        	// kmath, kerkis, fouriernc, pxfonts, mathpazo, mathpple, txfonts, mathptmx, arev, mathdesign, fourier
        	// xetex: usepackage fontspec, mathspec, xepersian
        colorCv.appendDeclarations(packages,declarations); // usepackage xolor
        microtypeCv.appendDeclarations(packages,declarations); // usepackage microtype, letterspace
        frameSc.appendDeclarations(packages,declarations); // usepackage longfbox
        noteCv.appendDeclarations(packages,declarations); // usepackage endnotes
        headingSc.appendDeclarations(packages,declarations); // usepackage titlesec
        charSc.appendDeclarations(packages,declarations); // usepackage ulem
        headingCv.appendDeclarations(packages,declarations); // no packages
        parCv.appendDeclarations(packages,declarations); // no packages
        pageSc.appendDeclarations(packages,declarations); // usepackage geometry, fancyhdr
        blockCv.appendDeclarations(packages,declarations); // no packages
        indexCv.appendDeclarations(packages,declarations); // usepackage makeidx
        bibCv.appendDeclarations(packages,declarations); // no packages
        sectionCv.appendDeclarations(packages,declarations); // usepackage multicol
        tableCv.appendDeclarations(packages,declarations); // usepackage array, longtable, supertabular, tabulary, hhline, colortbl
        listCv.appendDeclarations(packages,declarations); // no packages
        captionCv.appendDeclarations(packages,declarations); // usepackage caption
        inlineCv.appendDeclarations(packages,declarations); // no packages
        fieldCv.appendDeclarations(packages,declarations); // usepackage natbib, lastpage, titleref, hyperref 
        drawCv.appendDeclarations(packages,declarations); // usepackage graphicx
        customShapeCv.appendDeclarations(packages,declarations); // usepackage tikz
        mathCv.appendDeclarations(packages,declarations); // usepackage calc

        // Add custom preamble
        String sCustomPreamble = config.getCustomPreamble();
        if (sCustomPreamble.length()>0) {
        	declarations.append(sCustomPreamble).nl();
        }

        // Set \title, \author and \date (for \maketitle)
        createMeta("title",metaData.getTitle(),declarations);
        if (config.metadata()) {
            createMeta("author",metaData.getCreator(),declarations);
            // According to the spec, the date has the format YYYY-MM-DDThh:mm:ss
            String sDate = metaData.getDate();
            if (sDate!=null) {
            	createMeta("date",Misc.dateOnly(sDate),declarations);
            }
        }
		
        // Create options for documentclass
        if (config.formatting()>=LaTeXConfig.CONVERT_MOST) {
            StyleWithProperties dpStyle = ofr.getDefaultParStyle();
            if (dpStyle!=null) {
                String s = dpStyle.getProperty(XMLString.FO_FONT_SIZE);
                if ("10pt".equals(s)) { globalOptions.addValue("10pt"); }
                if ("11pt".equals(s)) { globalOptions.addValue("11pt"); }
                if ("12pt".equals(s)) { globalOptions.addValue("12pt"); }
            }
        }
		
        // Temp solution. TODO: Fix when new CSVList is implemented
        if (config.getGlobalOptions().length()>0) {
            globalOptions.addValue(config.getGlobalOptions());
        }

        // Assemble the document
        LaTeXDocumentPortion result = texDoc.getContents();

        if (!config.noPreamble()) {
            // Create document class declaration
	        result.append("% This file was converted to LaTeX by Writer2LaTeX ver. "+ConverterFactory.getVersion()).nl()
                  .append("% see http://writer2latex.sourceforge.net for more info").nl();
            result.append("\\documentclass");
            if (!globalOptions.isEmpty()) {
                result.append("[").append(globalOptions.toString()).append("]");
            }
            result.append("{").append(config.getDocumentclass()).append("}").nl();

            result.append(packages)
                  .append(declarations)
                  .append("\\begin{document}").nl();
        }

        result.append(body);

        if (!config.noPreamble()) {
            result.append("\\end{document}").nl();
        }
        else {
            result.append("\\endinput").nl();
        }
		
        // Add BibTeX document if there's any bibliographic data
        if (bibCv.getBibTeXDocument()!=null) {
            converterResult.addDocument(bibCv.getBibTeXDocument());
        }
    }
	
    private void createMeta(String sName, String sValue,LaTeXDocumentPortion ldp) {
        if (sValue==null) { return; }
        // Meta data is assumed to be in the default language:
        ldp.append("\\"+sName+"{"+i18n.convert(sValue,false,mainContext.getLang())+"}").nl();
    }


}