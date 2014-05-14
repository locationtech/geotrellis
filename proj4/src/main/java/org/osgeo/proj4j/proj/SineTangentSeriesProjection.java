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
import org.osgeo.proj4j.util.ProjectionMath;

class SineTangentSeriesProjection extends ConicProjection {
	private double C_x;
	private double C_y;
	private double C_p;
	private boolean tan_mode;

	protected SineTangentSeriesProjection( double p, double q, boolean mode ) {
		es = 0.;
		C_x = q / p;
		C_y = p;
		C_p = 1/ q;
		tan_mode = mode;
		initialize();
	}
	
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
		double c;

		xy.x = C_x * lplam * Math.cos(lpphi);
		xy.y = C_y;
		lpphi *= C_p;
		c = Math.cos(lpphi);
		if (tan_mode) {
			xy.x *= c * c;
			xy.y *= Math.tan(lpphi);
		} else {
			xy.x /= c;
			xy.y *= Math.sin(lpphi);
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate lp) {
		double c;
		
		xyy /= C_y;
		c = Math.cos(lp.y = tan_mode ? Math.atan(xyy) : ProjectionMath.asin(xyy));
		lp.y /= C_p;
    lp.x = xyx / (C_x * Math.cos(lp.y));
		if (tan_mode)
			lp.x /= c * c;
		else
			lp.x *= c;
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

}
