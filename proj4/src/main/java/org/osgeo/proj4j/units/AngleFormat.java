/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package org.osgeo.proj4j.units;

import java.text.DecimalFormat;
import java.text.FieldPosition;
import java.text.NumberFormat;
import java.text.ParsePosition;

import org.osgeo.proj4j.util.ProjectionMath;

/**
 * A NumberFormat for formatting Angles in various commonly-found mapping styles.
 */
public class AngleFormat extends NumberFormat {

  public static final char CH_MIN_SYMBOL = '\'';
  public static final String STR_SEC_SYMBOL = "\"";
  public static final char CH_DEG_SYMBOL = '\u00b0';
  public static final char CH_DEG_ABBREV = 'd';
  public static final char CH_MIN_ABBREV = 'm';
  public static final String STR_SEC_ABBREV = "s";
  
  public static final char CH_N = 'N';
  public static final char CH_E = 'E';
  public static final char CH_S = 'S';
  public static final char CH_W = 'W';

	public final static String ddmmssPattern = "DdM";
	public final static String ddmmssPattern2 = "DdM'S\"";
	public final static String ddmmssLongPattern = "DdM'S\"W";
	public final static String ddmmssLatPattern = "DdM'S\"N";
	public final static String ddmmssPattern4 = "DdMmSs";
	public final static String decimalPattern = "D.F";

	private DecimalFormat format;
	private String pattern;
	private boolean isDegrees;

	public AngleFormat() {
		this(ddmmssPattern);
	}
	
	public AngleFormat(String pattern) {
		this(pattern, false);
	}
	
	public AngleFormat(String pattern, boolean isDegrees) {
		this.pattern = pattern;
		this.isDegrees = isDegrees;
		format = new DecimalFormat();
		format.setMaximumFractionDigits(0);
		format.setGroupingUsed(false);
	}
	
	public StringBuffer format(long number, StringBuffer result, FieldPosition fieldPosition) {
		return format((double)number, result, fieldPosition);
	}

	public StringBuffer format(double number, StringBuffer result, FieldPosition fieldPosition) {
		int length = pattern.length();
		int f;
		boolean negative = false;

		if (number < 0) {
			for (int i = length-1; i >= 0; i--) {
				char c = pattern.charAt(i);
				if (c == 'W' || c == 'N') {
					number = -number;
					negative = true;
					break;
				}
			}
		}
		
		double ddmmss = isDegrees ? number : Math.toDegrees(number);
		int iddmmss = (int)Math.round(ddmmss * 3600);
		if (iddmmss < 0)
			iddmmss = -iddmmss;
		int fraction = iddmmss % 3600;

		for (int i = 0; i < length; i++) {
			char c = pattern.charAt(i);
			switch (c) {
			case 'R':
				result.append(number);
				break;
			case 'D':
				result.append((int)ddmmss);
				break;
			case 'M':
				f = fraction / 60;
				if (f < 10)
					result.append('0');
				result.append(f);
				break;
			case 'S':
				f = fraction % 60;
				if (f < 10)
					result.append('0');
				result.append(f);
				break;
			case 'F':
				result.append(fraction);
				break;
			case 'W':
				if (negative)
					result.append(CH_W);
				else
					result.append(CH_E);
				break;
			case 'N':
				if (negative)
					result.append(CH_S);
				else
					result.append(CH_N);
				break;
			default:
				result.append(c);
				break;
			}
		}
		return result;
	}
	
  /**
   * 
   * @param s
   * @return
   * @deprecated
   * @see Angle#parse(String)
   */
	public Number parse(String text, ParsePosition parsePosition) {
		double d = 0, m = 0, s = 0;
		double result;
		boolean negate = false;
		int length = text.length();
		if (length > 0) {
			char c = Character.toUpperCase(text.charAt(length-1));
			switch (c) {
			case 'W':
			case 'S':
				negate = true;
				// Fall into...
			case 'E':
			case 'N':
				text = text.substring(0, length-1);
				break;
			}
		}
		int i = text.indexOf('d');
		if (i == -1)
			i = text.indexOf('\u00b0');
		if (i != -1) {
			String dd = text.substring(0, i);
			String mmss = text.substring(i+1);
			d = Double.valueOf(dd).doubleValue();
			i = mmss.indexOf('m');
			if (i == -1)
				i = mmss.indexOf('\'');
			if (i != -1) {
				if (i != 0) {
					String mm = mmss.substring(0, i);
					m = Double.valueOf(mm).doubleValue();
				}
				if (mmss.endsWith("s") || mmss.endsWith("\""))
					mmss = mmss.substring(0, mmss.length()-1);
				if (i != mmss.length()-1) {
					String ss = mmss.substring(i+1);
					s = Double.valueOf(ss).doubleValue();
				}
				if (m < 0 || m > 59)
					throw new NumberFormatException("Minutes must be between 0 and 59");
				if (s < 0 || s >= 60)
					throw new NumberFormatException("Seconds must be between 0 and 59");
			} else if (i != 0)
				m = Double.valueOf(mmss).doubleValue();
			if (isDegrees)
				result = ProjectionMath.dmsToDeg(d, m, s);
			else
				result = ProjectionMath.dmsToRad(d, m, s);
		} else {
			result = Double.parseDouble(text);
			if (!isDegrees)
				result = Math.toRadians(result);
		}
		if (parsePosition != null)
			parsePosition.setIndex(text.length());
		if (negate)
			result = -result;
		return new Double(result);
	}
}

