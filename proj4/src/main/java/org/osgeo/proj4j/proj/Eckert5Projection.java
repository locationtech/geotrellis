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

public class Eckert5Projection extends Projection {

	private final static double XF = 0.44101277172455148219;
	private final static double RXF = 2.26750802723822639137;
	private final static double YF = 0.88202554344910296438;
	private final static double RYF = 1.13375401361911319568;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.x = XF * (1. + Math.cos(lpphi)) * lplam;
		out.y = YF * lpphi;
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.x = RXF * xyx / (1. + Math.cos( out.y = RYF * xyy));
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Eckert V";
	}

}
