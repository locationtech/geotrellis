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

public class GallProjection extends Projection {

	private final static double YF = 1.70710678118654752440;
	private final static double XF = 0.70710678118654752440;
	private final static double RYF = 0.58578643762690495119;
	private final static double RXF = 1.41421356237309504880;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.x = XF * lplam;
		out.y = YF * Math.tan(.5 * lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.x = RXF * xyx;
		out.y = 2. * Math.atan(xyy * RYF);
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Gall (Gall Stereographic)";
	}

}
