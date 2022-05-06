/************************************************************************
 *
 *  Calc.java
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
 *  Version 2.0 (2022-05-06)
 *
 */
package writer2latex.util;

/** A collection of static methods used to perform calculations on strings representing floating point numbers
 *  with units or percentages. In the JavaDoc, a length refers to a string like e.g. "21.7cm" and percent refers
 *  to a string like e.g. "2.5%".
 */
public class Calc {

	/** Get a float value from a string (e.g. "218.86" returns 218.86F)
	 * 
	 * @param sFloat the string to parse
	 * @param fDefault a default value to return if the string cannot be parsed as a float
	 * @return the float value of the string
	 */
	public static final float getFloat(String sFloat, float fDefault){
	    float f;
	    try {
	        f=Float.parseFloat(sFloat);
	    }
	    catch (NumberFormatException e) {
	        return fDefault;
	    }
	    return f;
	}

	/** Replace the unit inch with in on a length (e.g. "17.5inch" returns "17.5in")
	 * 
	 * @param sValue the length
	 * @return the truncated length
	 */
	public static String truncateLength(String sValue) {
	    if (sValue.endsWith("inch")) {
	        // Cut of inch to in
	        return sValue.substring(0,sValue.length()-2);
	    }
	    else {
	        return sValue;
	    }
	}

	/** Checks whether a given length is zero within a tolerance of 0.001 (e.g. "0.0005cm" returns true)
	 * 
	 * @param sValue the length to check
	 * @return true if the value is close to zero
	 */
	public static boolean isZero(String sValue) {
		return Math.abs(getFloat(sValue.substring(0, sValue.length()-2),0))<0.001;
	}

	// Return units per inch for some unit
	private static final float getUpi(String sUnit) {
	    if ("in".equals(sUnit)) { return 1.0F; }
	    else if ("mm".equals(sUnit)) { return 25.4F; }
	    else if ("cm".equals(sUnit)) { return 2.54F; }
	    else if ("pc".equals(sUnit)) { return 6F; }
	    else { return 72; } // pt or unknown
	}

	/** Convert a length to px assuming 96ppi; cf. the CSS spec (e.g. "0.1in" returns "9.6px").
	 *  Exception: Never return less than 1px
	 * 
	 * @param sLength the length to convert
	 * @return the converted length
	 */
	public static final String length2px(String sLength) {
	    if (sLength.equals("0")) { return "0"; }
	    float fLength=getFloat(sLength.substring(0,sLength.length()-2),1);
	    String sUnit=sLength.substring(sLength.length()-2);
	    float fPixels = 96.0F/getUpi(sUnit)*fLength;
	    if (Math.abs(fPixels)<0.01) {
	        // Very small, treat as zero
	        return "0";
	    }
	    else if (fPixels>0) {
	        // Never return less that 1px
	        return Float.toString(fPixels<1 ? 1 : fPixels)+"px";
	    }
	    else {
	        // Or above -1px
	        return Float.toString(fPixels>-1 ? -1 : fPixels)+"px";
	    }
	}

	/** Convert a length to rem assuming that 1em=16px, which seems to be a
	 *  suitable interpretation (the CSS spec does not prescribe anything)
	 * 
	 * @param sLength the length to convert
	 * @return the converted length
	 */
	public static final String length2rem(String sLength) {
	    if (sLength.equals("0")) { return "0"; }
	    float fLength=getFloat(sLength.substring(0,sLength.length()-2),1);
	    String sUnit=sLength.substring(sLength.length()-2);
	    float fRems = 6.0F/getUpi(sUnit)*fLength;
	    if (Math.abs(fRems)<0.01) {
	        // Very small, treat as zero
	        return "0";
	    }
	    else {
	        return Float.toString(fRems)+"rem";
	    }
	}

	/** Convert a length to cm 
	 * 
	 * @param sLength the length to convert
	 * @return the converted length as a double precision float
	 */
	public static final double length2cm(String sLength) {
	    if (sLength.equals("0")) { return 0; }
	    float fLength=getFloat(sLength.substring(0,sLength.length()-2),0);
	    String sUnit=sLength.substring(sLength.length()-2);
	    float fConvertedLength = getUpi("cm")/getUpi(sUnit)*fLength;
	    return fConvertedLength;
	}

	/** Divide dividend by divisor and return the quotient as an integer percentage
	 *  (e.g. "0.5cm" divided by "2cm" returns "25%"). 
	 *  Exception: Never returns below 1% except if the dividend is zero.
	 * 
	 * @param sDividend the length to use as dividend
	 * @param sDivisor the length to use as divisor
	 * @return the quotient percentage
	 */
	public static final String divide(String sDividend, String sDivisor) {
		return divide(sDividend,sDivisor,false);
	}

	/** Divide dividend by divisor and return the quotient as an integer percentage
	 *  (e.g. "0.5cm" divided by "2cm" returns "25%"). 
	 *  Exception: Never returns below 1% except if the dividend is zero, and never returns above 100%
	 *  if last parameter is true.
	 * 
	 * @param sDividend the length to use as dividend
	 * @param sDivisor the length to use as divisor
	 * @param bMax100 true if a maximum of 100% should be returned
	 * @return the quotient percentage
	 */
	public static final String divide(String sDividend, String sDivisor, boolean bMax100) {
	    if (sDividend.equals("0")) { return "0%"; }
	    if (sDivisor.equals("0")) { return "100%"; }
	
	    float fDividend=getFloat(sDividend.substring(0,sDividend.length()-2),1);
	    String sDividendUnit=sDividend.substring(sDividend.length()-2);
	    float fDivisor=getFloat(sDivisor.substring(0,sDivisor.length()-2),1);
	    String sDivisorUnit=sDivisor.substring(sDivisor.length()-2);
	    int nPercent = Math.round(100*fDividend*getUpi(sDivisorUnit)/fDivisor/getUpi(sDividendUnit));
	    if (bMax100 && nPercent>100) {
	    	return "100%";
	    }
	    else if (nPercent>0) {
	    	return Integer.toString(nPercent)+"%";
	    }
	    else {
	    	return "1%";
	    }
	}

	/** Multiply a length by a percentage (e.g. "150%" multiplied with "2.5mm" returns "3.75cm")
	 * 
	 * @param sPercent the percentage
	 * @param sLength the length
	 * @return the product length
	 */
	public static final String multiply(String sPercent, String sLength) {
	    if (sLength.equals("0")) { return "0"; }
	    float fPercent=getFloat(sPercent.substring(0,sPercent.length()-1),1);
	    return multiply(fPercent/100.0F,sLength);
	}
	
	/** Multiply a length by a float (e.g. 1.5 multiplied with "2cm" returns "3cm")
	 * 
	 */
	public static final String multiply(float f, String sLength) {
	    float fLength=getFloat(sLength.substring(0,sLength.length()-2),1);
	    String sUnit=sLength.substring(sLength.length()-2);
	    return Float.toString(f*fLength)+sUnit;
		
	}
	
	/** Add two lengths (e.g. "2.5cm" added to "1.08cm" returns "3.58cm")
	 * 
	 * @param sLength1 the first length term
	 * @param sLength2 the second length term
	 * @return the sum (as a length with the same unit as the first term)
	 */
	public static final String add(String sLength1, String sLength2){
	    if (sLength1.equals("0")) { return sLength2; }
	    if (sLength2.equals("0")) { return sLength1; }
	    float fLength1=getFloat(sLength1.substring(0,sLength1.length()-2),1);
	    String sUnit1=sLength1.substring(sLength1.length()-2);
	    float fLength2=getFloat(sLength2.substring(0,sLength2.length()-2),1);
	    String sUnit2=sLength2.substring(sLength2.length()-2);
	    // Use unit from sLength1:
	    return Float.toString(fLength1+getUpi(sUnit1)/getUpi(sUnit2)*fLength2)+sUnit1;
	}

	/** Subtract two lengths (e.g. "2.5cm" subtracted by "1.08cm" returns "1.42cm")
	 * 
	 * @param sLength1 the first length term
	 * @param sLength2 the second length term
	 * @return the difference (as a length with the same unit as the first term)
	 */
	public static final String sub(String sLength1, String sLength2){
	    return add(sLength1,multiply("-100%",sLength2));
	}
	
	/** Round a length to two decimal places
	 * 
	 * @param sLength the length to reound
	 * @return the rounded length
	 */
	public static String round(String sLength) {
		// Add 0.05; this implies that we can simply truncate
		String s = Float.toString(getFloat(sLength.substring(0,sLength.length()-2),1)+0.005F);
		int nPoint = s.indexOf(".");
		if (nPoint>-1) {
			s = s.substring(0,Math.min(nPoint+3,s.length()));
			while (s.charAt(s.length()-1)=='0') { s = s.substring(0,s.length()-1); }
			if (s.charAt(s.length()-1)=='.') { s = s.substring(0,s.length()-1); }
			if (s.length()==0) { s="0"; }
		}
		return s+sLength.substring(sLength.length()-2);
	}
	
	/** Test if two lengths are equal (within a tolerance of 0.001
	 * 
	 * @param sThis one length
	 * @param sThat another length
	 * @return true if the lengths can be considered equal
	 */
	public static boolean isEqual(String sThis, String sThat) {
		return isZero(sub(sThis,sThat));
	}

	/** Test whether a given length is smaller than another length
	 *  (e.g. "2.5cm" compared to "2.6cm" returns true;
	 *  
	 * @param sThis is this length the smaller?
	 * @param sThat is this length the larger?
	 * @return true is the first length is smaller than the second length
	 */
	public static boolean isLessThan(String sThis, String sThat) {
	    return sub(sThis,sThat).startsWith("-");
	}
	
	/** Return the maximum of some lengths, discarding null values
	 * 
	 * @param sLengths one or more lengths
	 * @return the minimum length (or null if all lengths were null)
	 */
	public static String max(String... sLengths) {
		String sMax = null;
		for (String sLength : sLengths) {
			if (sMax==null || (sMax!=null && sLength!=null && isLessThan(sMax,sLength))) {
				sMax = sLength;
			}
		}
		return sMax;
	}

	/** Return the minimum of some lengths, discarding null values
	 * 
	 * @param sLengths one or more lengths
	 * @return the minimum length (or null if all lengths were null)
	 */
	public static String min(String... sLengths) {
		String sMin = null;
		for (String sLength : sLengths) {
			if (sMin==null || (sMin!=null && sLength!=null && isLessThan(sLength,sMin))) {
				sMin = sLength;
			}
		}
		return sMin;
	}

	/** Get the absolute value of a length (e.g. "-2.5cm" returns "2.5cm")
	 * 
	 * @param sLength the length
	 * @return the absolute value
	 */
	public static String abs(String sLength) {
	    return sLength.startsWith("-") ? sLength.substring(1) : sLength;
	}

}
