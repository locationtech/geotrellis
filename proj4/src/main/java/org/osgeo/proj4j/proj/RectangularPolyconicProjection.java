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

public class RectangularPolyconicProjection extends Projection {

	private double phi0;
	private double phi1;
	private double fxa;
	private double fxb;
	private boolean mode;

	private final static double EPS = 1e-9;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double fa;

		if (mode)
			fa = Math.tan(lplam * fxb) * fxa;
		else
			fa = 0.5 * lplam;
		if (Math.abs(lpphi) < EPS) {
			out.x = fa + fa;
			out.y = - phi0;
		} else {
			out.y = 1. / Math.tan(lpphi);
			out.x = Math.sin(fa = 2. * Math.atan(fa * Math.sin(lpphi))) * out.y;
			out.y = lpphi - phi0 + (1. - Math.cos(fa)) * out.y;
		}
		return out;
	}

	public void initialize() { // rpoly
		super.initialize();
/*FIXME
		if ((mode = (phi1 = Math.abs(pj_param(params, "rlat_ts").f)) > EPS)) {
			fxb = 0.5 * Math.sin(phi1);
			fxa = 0.5 / fxb;
		}
*/
	}

	public String toString() {
		return "Rectangular Polyconic";
	}

}
