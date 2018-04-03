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
		
		// Part 1: Western languages (latin, cyrillic, greek, armenian scripts)
		
		// Latin script
		languageMap.put("ast", "asturian");
		languageMap.put("bg", "bulgarian");
		languageMap.put("br", "breton");
		languageMap.put("ca", "catalan");
		languageMap.put("cs", "czech");
		languageMap.put("cy", "welsh");
		languageMap.put("da", "danish");
		languageMap.put("de", "german");
		languageMap.put("dsb", "lsorbian"); // Lower sorbian
		languageMap.put("en", "english");
		languageMap.put("eo", "esperanto");
		languageMap.put("es", "spanish");
		languageMap.put("et", "estonian");
		languageMap.put("eu", "basque");
		languageMap.put("fi", "finnish");
		languageMap.put("fr", "french");
		languageMap.put("fur", "friulan");
		languageMap.put("ga", "irish");
		languageMap.put("gd", "scottish");
		languageMap.put("gl", "galician");
		languageMap.put("hr", "croatian");
		languageMap.put("hsb", "usorbian"); // Upper sorbian
		languageMap.put("hu", "magyar");
		languageMap.put("id", "bahasai"); // Bahasa Indonesia
		languageMap.put("ie", "interlingua");
		languageMap.put("is", "icelandic");
		languageMap.put("it", "italian");
		languageMap.put("la", "latin");
		languageMap.put("lt", "lithuanian");
		languageMap.put("lv", "latvian");
		languageMap.put("ms", "bahasam"); // Bahasa Melayu
		languageMap.put("nb", "norsk");
		languageMap.put("nl", "dutch");
		languageMap.put("nn", "nynorsk");
		languageMap.put("oc", "occitan");
		languageMap.put("pl", "polish");
		languageMap.put("pms", "piedmontese");
		languageMap.put("pt", "portuges");
		languageMap.put("pt-BR", "brazilian");
		languageMap.put("rm", "romansh");
		languageMap.put("ro", "romanian");
		languageMap.put("sk", "slovak");
		languageMap.put("sl", "slovenian");
		languageMap.put("sq", "albanian");
		languageMap.put("sv", "swedish");
		languageMap.put("tr", "turkish");
		languageMap.put("vi", "vietnamese");
		languageMap.put("sme", "samin"); // North sami
		
		// Cyrillic and latin script (both variants are in use)
		languageMap.put("sr", "serbian");
		languageMap.put("tk", "turkmen");

		// Cyrillic script
		languageMap.put("ru", "russian");
		languageMap.put("uk", "ukrainian");
		
		// Greek script
		languageMap.put("cop", "coptic");
		languageMap.put("el", "greek");
		languageMap.put("grc", "greek");

		// Armenian script (Note: not supported by Latin Modern)
		languageMap.put("hy", "armenian");
		
		// Part 2: CTL
		languageMap.put("am", "amharic"); // Amharic script
		languageMap.put("ar", "arabic"); // Arabic script		
		languageMap.put("bn", "bengali"); // Bengali script
		languageMap.put("bo", "tibetan"); // Tibetan script
		languageMap.put("dv", "divehi"); // Maldivian, Thaana script
		languageMap.put("fa", "farsi"); // Persian script (Note: extension of Arabic)
		languageMap.put("he", "hebrew"); // Hebrew
		languageMap.put("hi", "hindi"); // Devanagari
		languageMap.put("km", "khmer"); // Khmer script
		languageMap.put("kn", "kannada"); // Kannada script
		languageMap.put("lo", "lao"); // Lao or Thai script
		languageMap.put("ml", "malayalam"); // Malayalam script
		languageMap.put("mr", "marathi"); // Devanagari 
		languageMap.put("nqo", "nko"); // N'ko script
		languageMap.put("sa", "sanskrit"); // Devanagari
		languageMap.put("syr", "syriac"); // Syriac alphabet
		languageMap.put("ta", "tamil"); // Tamil alphabet or Arwi script
		languageMap.put("te", "telugu"); // Telugu script
		languageMap.put("th", "thai"); // Thai script
		languageMap.put("ur", "urdu"); // Urdu script (Note: Extension of Persian)
		
		// Part 3: CJK
		languageMap.put("ko", "korean"); // CJK

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
