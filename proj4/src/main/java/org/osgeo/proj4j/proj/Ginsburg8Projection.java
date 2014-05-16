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

/*
 * This file was semi-automatically converted from the public-domain USGS PROJ source.
 */
package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.ProjCoordinate;

public class Ginsburg8Projection extends Projection {

	private final static double Cl = 0.000952426;
	private final static double Cp = 0.162388;
	private final static double C12 = 0.08333333333333333;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double t = lpphi * lpphi;

		out.y = lpphi * (1. + t * C12);
		out.x = lplam * (1. - Cp * t);
		t = lplam * lplam;
		out.x *= (0.87 - Cl * t * t);
		return out;
	}

	public String toString() {
		return "Ginsburg VIII (TsNIIGAiK)";
	}

}
