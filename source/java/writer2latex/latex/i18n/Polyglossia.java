/************************************************************************
 *
 *  XeTeXI18n.java
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
 *  Version 2.0 (2018-03-27) 
 * 
 */

package writer2latex.latex.i18n;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Polyglossia {
	private static Map<String,String> languageMap;
	private static Map<String,String> variantMap;
	private static Set<String> ctlLanguages;
	
	static {  
		languageMap = new HashMap<String,String>();
		languageMap.put("am", "amharic"); // CTL
		languageMap.put("ar", "arabic"); // CTL
		languageMap.put("ast", "asturian"); // LCG
		languageMap.put("bg", "bulgarian"); // LCG 
		languageMap.put("bn", "bengali"); // CTL
		languageMap.put("bo", "tibetan"); // CTL
		languageMap.put("br", "breton"); // LCG
		languageMap.put("ca", "catalan");  // LCG
		languageMap.put("cop", "coptic");  // LCG
		languageMap.put("cs", "czech"); // LCG
		languageMap.put("cy", "welsh"); // LCG
		languageMap.put("da", "danish"); // LCG
		languageMap.put("de", "german");  // LCG
		languageMap.put("dsb", "lsorbian"); // LCG
		languageMap.put("dv", "divehi"); // CTL
		languageMap.put("el", "greek");  // LCG
		languageMap.put("en", "english"); // LCG
		languageMap.put("eo", "esperanto");  // LCG
		languageMap.put("es", "spanish"); // LCG
		languageMap.put("et", "estonian");  // LCG
		languageMap.put("eu", "basque");  // LCG
		languageMap.put("fa", "farsi");  // CTL
		languageMap.put("fi", "finnish");  // LCG
		languageMap.put("fr", "french");  // LCG
		languageMap.put("fur", "friulan");  // LCG
		languageMap.put("ga", "irish"); // LCG
		languageMap.put("gd", "scottish");  // LCG
		languageMap.put("gl", "galician");  // LCG
		languageMap.put("grc", "greek"); // LCG
		languageMap.put("he", "hebrew"); // CTL
		languageMap.put("hi", "hindi"); // CTL
		languageMap.put("hr", "croatian"); // LCG
		languageMap.put("hsb", "usorbian"); // LCG
		languageMap.put("hu", "magyar");  // LCG
		languageMap.put("hy", "armenian"); // LCG
		languageMap.put("id", "bahasai"); // LCG? (Bahasa Indonesia)
		languageMap.put("ie", "interlingua"); // LCG
		languageMap.put("is", "icelandic"); // LCG
		languageMap.put("it", "italian"); // LCG
		languageMap.put("km", "khmer"); // CTL
		languageMap.put("kn", "kannada"); // CTL
		languageMap.put("ko", "korean"); // CJK
		languageMap.put("la", "latin");  // LCG
		languageMap.put("lo", "lao"); // CTL
		languageMap.put("lt", "lithuanian"); // LCG
		languageMap.put("lv", "latvian"); // LCG
		languageMap.put("ml", "malayalam"); // CTL
		languageMap.put("mr", "marathi"); // CTL
		languageMap.put("ms", "bahasam"); // LCG? (Bahasa Melayu)
		languageMap.put("nb", "norsk"); // LCG
		languageMap.put("nl", "dutch");  // LCG
		languageMap.put("nn", "nynorsk"); // LCG
		languageMap.put("nqo", "nko"); // CTL
		languageMap.put("oc", "occitan"); // LCG
		languageMap.put("pl", "polish"); // LCG
		languageMap.put("pms", "piedmontese"); // LCG
		languageMap.put("pt", "portuges");  // LCG
		languageMap.put("pt-BR", "brazilian");  // LCG
		languageMap.put("rm", "romansh");  // LCG
		languageMap.put("ro", "romanian");  // LCG
		languageMap.put("ru", "russian");  // LCG
		languageMap.put("sa", "sanskrit"); // CTL
		languageMap.put("sk", "slovak");  // LCG
		languageMap.put("sl", "slovenian"); // LCG
		languageMap.put("sq", "albanian"); // LCG
		languageMap.put("sr", "serbian");  // LCG
		languageMap.put("sv", "swedish"); // LCG
		languageMap.put("syr", "syriac"); // CTL
		languageMap.put("ta", "tamil"); // CTL
		languageMap.put("te", "telugu"); // CTL
		languageMap.put("th", "thai"); // CTL
		languageMap.put("tk", "turkmen"); // LCG
		languageMap.put("tr", "turkish"); // LCG
		languageMap.put("uk", "ukrainian"); // LCG
		languageMap.put("ur", "urdu"); // CTL
		languageMap.put("vi", "vietnamese"); // LCG
		languageMap.put("sme", "samin"); // LCG (north sami)
		
		variantMap = new HashMap<String,String>();
		// English variants
		variantMap.put("en-US", "american");
		variantMap.put("en-GB", "british");
		variantMap.put("en-AU", "australian");
		variantMap.put("en-NZ", "newzealand");
		// Greek variants
		variantMap.put("el", "monotonic");
		variantMap.put("grc", "ancient"); // Supported in OOo since 3.2
		
		ctlLanguages = new HashSet<String>();
		ctlLanguages.add("am");
		ctlLanguages.add("ar");
		ctlLanguages.add("bn");
		ctlLanguages.add("bo");
		ctlLanguages.add("dv");
		ctlLanguages.add("fa");
		ctlLanguages.add("he");
		ctlLanguages.add("hi");
		ctlLanguages.add("km");
		ctlLanguages.add("kn");
		ctlLanguages.add("lo");
		ctlLanguages.add("ml");
		ctlLanguages.add("mr");
		ctlLanguages.add("nqo");
		ctlLanguages.add("sa");
		ctlLanguages.add("syr");
		ctlLanguages.add("ta");
		ctlLanguages.add("te");
		ctlLanguages.add("th");
		ctlLanguages.add("ur");
	}
	
	private static String getEntry(Map<String,String> map, String sLocale, String sLang) {
		if (map.containsKey(sLocale)) {
			return map.get(sLocale);
		}
		else if (map.containsKey(sLang)) {
			return map.get(sLang);
		}
		return null;
	}
	
	// This ended the static part of Polyglossia
	
	private Set<String> languages = new HashSet<String>();
	private List<String> declarations = new ArrayList<String>();
	private Map<String,String[]> commands = new HashMap<String,String[]>();
	
	/** <p>Get the declarations for the applied languages, in the form</p>
	 *  <p><code>\\usepackage{polyglossia}</code></p>
	 *  <p><code>\\setdefaultlanguage{language1}</code></p>
	 *  <p><code>\\setotherlanguage{language2}</code></p>
	 *  <p><code>\\setotherlanguage{language3}</code></p>
	 *  <p><code>...</code></p>
	 * 
	 * @return the declarations as a string array
	 */
	public String[] getDeclarations() {
		return declarations.toArray(new String[declarations.size()]);
	}
	
	/** <p>Add the given locale to the list of applied locales and return definitions for applying the
	 * language to a text portion:</p>
	 * <ul>
	 * <li>A command of the forn <code>\textlanguage[variant=languagevariant]</code></li>
	 * <li>An environment in the form
	 * <code>\begin{language}[variant=languagevariant]</code>...<code>\end{language}</code></li>
	 * </ul>
	 * <p>The first applied language is the default language</p>
	 * 
	 * @param sLang The language
	 * @param sCountry The country (may be null)
	 * @return a string array containing definitions to apply the language: Entry 0 contains a command
	 * and Entry 1 and 2 contains an environment
	 */
	public String[] applyLanguage(String sLang, String sCountry) {
		String sLocale = sCountry!=null ? sLang+"-"+sCountry : sLang;
		if (commands.containsKey(sLocale)) {
			return commands.get(sLocale);
		}
		else {
			// Get the Polyglossia language and variant
			String sPolyLang = getEntry(languageMap,sLocale,sLang);
			if (sPolyLang!=null) {
				String sVariant = getEntry(variantMap,sLocale,sLang);
				if (sVariant!=null) {
					sVariant = "[variant="+sVariant+"]";
				}
				else {
					sVariant = "";
				}
				
				if (languages.size()==0) {
					// First language, load Polyglossia and make the language default
					declarations.add("\\usepackage{polyglossia}");
					declarations.add("\\setdefaultlanguage"+sVariant+"{"+sPolyLang+"}");
					languages.add(sPolyLang);
					sVariant = ""; // Do not apply variant directly
				}
				else if (!languages.contains(sPolyLang)) {
					// New language, add to declarations
					declarations.add("\\setotherlanguage"+sVariant+"{"+sPolyLang+"}");
					languages.add(sPolyLang);
					sVariant = ""; // Do not apply variant directly
				}
				
				String[] sCommand = new String[3];
				sCommand[0] = "\\text"+sPolyLang+sVariant;
				if ("arabic".equals(sPolyLang)) { sPolyLang="Arabic"; }
				sCommand[1] = "\\begin{"+sPolyLang+"}"+sVariant;
				sCommand[2] = "\\end{"+sPolyLang+"}";
				commands.put(sLocale, sCommand);
				return sCommand;
			}
			else {
				// Unknown language
				String[] sCommand = new String[3];
				sCommand[0] = "";
				sCommand[1] = "";
				sCommand[2] = "";
				commands.put(sLocale, sCommand);
				return sCommand;
			}
		}
	}
	
	/** Test whether a given language uses complex text layout
	 * 
	 * @param sLang
	 * @param sCountry
	 * @return
	 */
	public boolean isCTL(String sLang, String sCountry) {
		return ctlLanguages.contains(sLang);
	}
}
