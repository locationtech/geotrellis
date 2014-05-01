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

public class TranverseCentralCylindricalProjection extends CylindricalProjection {

	public TranverseCentralCylindricalProjection() {
		minLongitude = ProjectionMath.degToRad(-60);
		maxLongitude = ProjectionMath.degToRad(60);
	}
	
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double b, bt;

		b = Math.cos(lpphi) * Math.sin(lplam);
		if ((bt = 1. - b * b) < EPS10)
			throw new ProjectionException("F");
		out.x = b / Math.sqrt(bt);
		out.y = Math.atan2(Math.tan(lpphi), Math.cos(lplam));
		return out;
	}

	public boolean isRectilinear() {
		return false;
	}

	public String toString() {
		return "Transverse Central Cylindrical";
	}

}
