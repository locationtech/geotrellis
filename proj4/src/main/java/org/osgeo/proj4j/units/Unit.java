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

import java.io.Serializable;
import java.text.NumberFormat;

public class Unit implements Serializable {

	static final long serialVersionUID = -6704954923429734628L;
	
	public final static int ANGLE_UNIT = 0;
	public final static int LENGTH_UNIT = 1;
	public final static int AREA_UNIT = 2;
	public final static int VOLUME_UNIT = 3;
	
	public String name, plural, abbreviation;
	public double value;
	public static final NumberFormat format;
	
	static {
		format = NumberFormat.getNumberInstance();
		format.setMaximumFractionDigits(2);
		format.setGroupingUsed(false);
	}
	
	public Unit(String name, String plural, String abbreviation, double value) {
		this.name = name;
		this.plural = plural;
		this.abbreviation = abbreviation;
		this.value = value;
	}

	public double toBase(double n) {
		return n * value;
	}

	public double fromBase(double n) {
		return n / value;
	}
	
	public double parse(String s) throws NumberFormatException {
		try {
			return format.parse(s).doubleValue();
		}
		catch (java.text.ParseException e) {
			throw new NumberFormatException(e.getMessage());
		}
	}
	
	public String format(double n) {
		return format.format(n)+" "+abbreviation;
	}
	
	public String format(double n, boolean abbrev) {
		if (abbrev)
			return format.format(n)+" "+abbreviation;
		return format.format(n);
	}
	
	public String format(double x, double y, boolean abbrev) {
		if (abbrev)
			return format.format(x)+"/"+format.format(y)+" "+abbreviation;
		return format.format(x)+"/"+format.format(y);
	}

	public String format(double x, double y) {
		return format(x, y, true);
	}

	public String toString() {
		return plural;
	}

	public boolean equals(Object o) {
		if (o instanceof Unit) {
			return ((Unit)o).value == value;
		}
		return false;
	}

}
