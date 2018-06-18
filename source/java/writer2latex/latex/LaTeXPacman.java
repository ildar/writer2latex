/************************************************************************
 *
 *  LaTeXDocumentPortion.java
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
 *  Version 2.0 (2018-06-18)
 *
 */

package writer2latex.latex;

import java.io.IOException;
import java.io.OutputStreamWriter;

import writer2latex.util.CSVList;

/** This class populates the first part of a LaTeX preamble, which is usually predominated by package loading
 *  with <code>\\usepackage</code>. The class provides particular methods for this, which merges the loading
 *  of several packages to a single <code>\\usepackage</code>.
 */
public class LaTeXPacman extends LaTeXDocumentPortion {
	
    private CSVList packageList; // current set of LaTeX packages to be included in the preamble
    
	public LaTeXPacman(boolean bWrap) {
		super(bWrap);
        packageList = new CSVList(',');
	}
	
    public LaTeXPacman usepackage(String sPackageName) {
    	packageList.addValue(sPackageName);
    	return this;
    }
    
    public LaTeXPacman usepackage(String sOptions, String sPackageName) {
    	if (sOptions!=null && sOptions.length()>0) {
    		append("\\usepackage[").append(sOptions).append("]{").append(sPackageName).append("}").nl();    		
    	}
    	else {
    		usepackage(sPackageName);
    	}
    	return this;
    }
    
    private void usepackages() {
    	if (!packageList.isEmpty()) {
    		// super.append to avoid an inifinite recursion...
    		super.append("\\usepackage{");
    		super.append(packageList.toString());
    		super.append("}");
    		super.nl();
    		packageList.clear();
    	}
    }
    
    // We must override the usual append methods because the package list may need to be flushed
    // before appending further material
	
    @Override public LaTeXDocumentPortion append(LaTeXDocumentPortion ldp) {
    	usepackages();
    	return super.append(ldp);
    }
    
    @Override public LaTeXDocumentPortion append(String s){
    	usepackages();
    	return super.append(s);
    }
    
    @Override public LaTeXDocumentPortion append(int n){
    	usepackages();
    	return super.append(n);
    }
    
    @Override public LaTeXDocumentPortion nl(){
    	usepackages();
    	return super.nl();
    }
    
    // The same applies to the write methods
    
    @Override public void write(OutputStreamWriter osw, int nLineLen, String sNewline) throws IOException {
    	usepackages();
    	super.write(osw, nLineLen, sNewline);
    }
    
    @Override public String toString() {
    	usepackages();
    	return super.toString();
    }

}
