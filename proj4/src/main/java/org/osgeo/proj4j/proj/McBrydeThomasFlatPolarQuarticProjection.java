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

public class McBrydeThomasFlatPolarQuarticProjection extends PseudoCylindricalProjection {

	private final static int NITER = 20;
	private final static double EPS = 1e-7;
	private final static double ONETOL = 1.000001;
	private final static double C = 1.70710678118654752440;
	private final static double RC = 0.58578643762690495119;
	private final static double FYC = 1.87475828462269495505;
	private final static double RYC = 0.53340209679417701685;
	private final static double FXC = 0.31245971410378249250;
	private final static double RXC = 3.20041258076506210122;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double th1, c;
		int i;

		c = C * Math.sin(lpphi);
		for (i = NITER; i > 0; --i) {
			out.y -= th1 = (Math.sin(.5*lpphi) + Math.sin(lpphi) - c) /
				(.5*Math.cos(.5*lpphi)  + Math.cos(lpphi));
			if (Math.abs(th1) < EPS) break;
		}
		out.x = FXC * lplam * (1.0 + 2. * Math.cos(lpphi)/Math.cos(0.5 * lpphi));
		out.y = FYC * Math.sin(0.5 * lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double t = 0;

		double lpphi = RYC * xyy;
		if (Math.abs(lpphi) > 1.) {
			if (Math.abs(lpphi) > ONETOL)	throw new ProjectionException("I");
			else if (lpphi < 0.) { t = -1.; lpphi = -Math.PI; }
			else { t = 1.; lpphi = Math.PI; }
		} else
			lpphi = 2. * Math.asin(t = lpphi);
		out.x = RXC * xyx / (1. + 2. * Math.cos(lpphi)/Math.cos(0.5 * lpphi));
		lpphi = RC * (t + Math.sin(lpphi));
		if (Math.abs(lpphi) > 1.)
			if (Math.abs(lpphi) > ONETOL)
				throw new ProjectionException("I");
			else
				lpphi = lpphi < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		else
			lpphi = Math.asin(lpphi);
		out.y = lpphi;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public boolean isEqualArea() {
     return true;
	}

	public String toString() {
		return "McBryde-Thomas Flat-Polar Quartic";
	}

}
