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

public class AugustProjection extends Projection {

	private final static double M = 1.333333333333333;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double t, c1, c, x1, x12, y1, y12;

		t = Math.tan(.5 * lpphi);
		c1 = Math.sqrt(1. - t * t);
		c = 1. + c1 * Math.cos(lplam *= .5);
		x1 = Math.sin(lplam) *  c1 / c;
		y1 =  t / c;
		out.x = M * x1 * (3. + (x12 = x1 * x1) - 3. * (y12 = y1 *  y1));
		out.y = M * y1 * (3. + 3. * x12 - y12);
		return out;
	}

	/**
	 * Returns true if this projection is conformal
	 */
	public boolean isConformal() {
		return true;
	}

	public boolean hasInverse() {
		return false;
	}

	public String toString() {
		return "August Epicycloidal";
	}

}
