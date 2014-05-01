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


public class DegreeUnit extends Unit {
	
	static final long serialVersionUID = -3212757578604686538L;
	
	private static AngleFormat format = new AngleFormat(AngleFormat.ddmmssPattern, true);
	
	public DegreeUnit() {
		super("degree", "degrees", "deg", 1);
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
}

