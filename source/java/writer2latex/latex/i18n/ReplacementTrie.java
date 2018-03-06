/************************************************************************
 *
 *  ReplacementTrie.java
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
 *  Version 1.2 (2009-09-20)
 *
 */

package writer2latex.latex.i18n;

import java.util.HashSet;
import java.util.Set;

/** This class contains a trie of string -> LaTeX code replacements 
*/
public class ReplacementTrie extends ReplacementTrieNode {

    public ReplacementTrie() {
        super('*',0);
    }
	
    public ReplacementTrieNode get(String sInput) {
        return get(sInput,0,sInput.length());
    }
	
    public ReplacementTrieNode get(String sInput, int nStart, int nEnd) {
        if (sInput.length()==0) { return null; }
        else { return super.get(sInput,nStart,nEnd); }
    }
	
    public void put(String sInput, String sLaTeXCode, int nFontencs) {
        if (sInput.length()==0) { return; }
        else { super.put(sInput,sLaTeXCode,nFontencs); }
    }
	
    public Set<String> getInputStrings() {
    	HashSet<String> strings = new HashSet<String>();
    	collectStrings(strings,"");
    	return strings;
    }


}
