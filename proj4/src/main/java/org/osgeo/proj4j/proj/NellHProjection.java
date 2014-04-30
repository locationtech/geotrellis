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

public class NellHProjection extends Projection {

	private final static int NITER = 9;
	private final static double EPS = 1e-7;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.x = 0.5 * lplam * (1. + Math.cos(lpphi));
		out.y = 2.0 * (lpphi - Math.tan(0.5 *lpphi));
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double V, c, p;
		int i;

		p = 0.5 * xyy;
		for (i = NITER; i > 0 ; --i) {
			c = Math.cos(0.5 * xyy);
			out.y -= V = (xyy - Math.tan(xyy/2) - p)/(1. - 0.5/(c*c));
			if (Math.abs(V) < EPS)
				break;
		}
		if (i == 0) {
			out.y = p < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
			out.x = 2. * xyx;
		} else
			out.x = 2. * xyx / (1. + Math.cos(xyy));
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Nell-Hammer";
	}

}
