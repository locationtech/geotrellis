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
import org.osgeo.proj4j.ProjectionException;
import org.osgeo.proj4j.util.ProjectionMath;

public class CentralCylindricalProjection extends CylindricalProjection {

	private double ap;

	private final static double EPS10 = 1.e-10;

	public CentralCylindricalProjection() {
		minLatitude = Math.toRadians(-80);
		maxLatitude = Math.toRadians(80);
	}
	
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		if (Math.abs(Math.abs(lpphi) - ProjectionMath.HALFPI) <= EPS10) throw new ProjectionException("F");
		out.x = lplam;
		out.y = Math.tan(lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.y = Math.atan(xyy);
		out.x = xyx;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Central Cylindrical";
	}

}
