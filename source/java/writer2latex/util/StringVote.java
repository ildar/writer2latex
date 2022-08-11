/************************************************************************
 *
 *  StringVote.java
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
 *  Version 2.0 (2022-08-11)
 *
 */
package writer2latex.util;

import java.util.HashMap;
import java.util.Map;

/** This class is used to find the most popular string value
 *
 */
public class StringVote {
	
	private Map<String,Integer> items = new HashMap<>();
	
	private String sCurrentWinner = null;
	
	public void castVote(String s) {
		if (items.containsKey(s)) {
			items.put(s, items.get(s)+1);
		} 
		else {
			items.put(s, 1);
		}
		if (sCurrentWinner!=null) {
			if (items.get(s)>items.get(sCurrentWinner)) {
				sCurrentWinner = s;
			}
		}
		else {
			sCurrentWinner = s;
		}
	}
	
	public String getWinner() {
		/*for (String s : items.keySet()) {
			System.out.println(s+" -> "+items.get(s));
		}
		System.out.println("Winner "+sCurrentWinner);*/
		return sCurrentWinner;
	}

}
