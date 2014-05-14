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

public class FoucautSinusoidalProjection extends Projection {
	private double n, n1;

	private final static int MAX_ITER = 10;
	private final static double LOOP_TOL = 1e-7;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double t;

		t = Math.cos(lpphi);
		out.x = lplam * t / (n + n1 * t);
		out.y = n * lpphi + n1 * Math.sin(lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double V;
		int i;

		if (n != 0) {
			out.y = xyy;
			for (i = MAX_ITER; i > 0; --i) {
				out.y -= V = (n * out.y + n1 * Math.sin(out.y) - xyy ) /
					(n + n1 * Math.cos(out.y));
				if (Math.abs(V) < LOOP_TOL)
					break;
			}
			if (i == 0)
				out.y = xyy < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		} else
			out.y = ProjectionMath.asin(xyy);
		V = Math.cos(out.y);
		out.x = xyx * (n + n1 * V) / V;
		return out;
	}

	public void initialize() {
		super.initialize();
//		n = pj_param(params, "dn").f;
		if (n < 0. || n > 1.)
			throw new ProjectionException("-99");
		n1 = 1. - n;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Foucaut Sinusoidal";
	}

}
