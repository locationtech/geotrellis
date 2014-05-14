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

public class Units {

	// Angular units
	public final static Unit DEGREES = new DegreeUnit();
	public final static Unit RADIANS = new Unit("radian", "radians", "rad", Math.toDegrees(1));
	public final static Unit ARC_MINUTES = new Unit("arc minute", "arc minutes", "min", 1/60.0);
	public final static Unit ARC_SECONDS = new Unit("arc second", "arc seconds", "sec", 1/3600.0);

	// Distance units
	
	// Metric units
	public final static Unit KILOMETRES = new Unit("kilometre", "kilometres", "km", 1000);
	public final static Unit METRES = new Unit("metre", "metres", "m", 1);
	public final static Unit DECIMETRES = new Unit("decimetre", "decimetres", "dm", 0.1);
	public final static Unit CENTIMETRES = new Unit("centimetre", "centimetres", "cm", 0.01);
	public final static Unit MILLIMETRES = new Unit("millimetre", "millimetres", "mm", 0.001);

	// International units
	public final static Unit NAUTICAL_MILES = new Unit("nautical mile", "nautical miles", "kmi", 1852);
	public final static Unit MILES = new Unit("mile", "miles", "mi", 1609.344);
	public final static Unit CHAINS = new Unit("chain", "chains", "ch", 20.1168);
	public final static Unit YARDS = new Unit("yard", "yards", "yd", 0.9144);
	public final static Unit FEET = new Unit("foot", "feet", "ft", 0.3048);
	public final static Unit INCHES = new Unit("inch", "inches", "in", 0.0254);

	// U.S. units
	public final static Unit US_MILES = new Unit("U.S. mile", "U.S. miles", "us-mi", 1609.347218694437);
	public final static Unit US_CHAINS = new Unit("U.S. chain", "U.S. chains", "us-ch", 20.11684023368047);
	public final static Unit US_YARDS = new Unit("U.S. yard", "U.S. yards", "us-yd", 0.914401828803658);
	public final static Unit US_FEET = new Unit("U.S. foot", "U.S. feet", "us-ft", 0.304800609601219);
	public final static Unit US_INCHES = new Unit("U.S. inch", "U.S. inches", "us-in", 1.0/39.37);

	// Miscellaneous units
	public final static Unit FATHOMS = new Unit("fathom", "fathoms", "fath", 1.8288);
	public final static Unit LINKS = new Unit("link", "links", "link", 0.201168);

	public final static Unit POINTS = new Unit("point", "points", "point", 0.0254/72.27);

	public static Unit[] units = {
		DEGREES,
		KILOMETRES, METRES, DECIMETRES, CENTIMETRES, MILLIMETRES,
		MILES, YARDS, FEET, INCHES,
		US_MILES, US_YARDS, US_FEET, US_INCHES,
		NAUTICAL_MILES
	};

	public static Unit findUnits(String name) {
		for (int i = 0; i < units.length; i++) {
			if (name.equals(units[i].name) || name.equals(units[i].plural) || name.equals(units[i].abbreviation))
				return units[i];
		}
		return METRES;
	}

	public static double convert(double value, Unit from, Unit to) {
		if (from == to)
			return value;
		return to.fromBase(from.toBase(value));
	}

}
