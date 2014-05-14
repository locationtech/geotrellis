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

public class NicolosiProjection extends Projection {

	private final static double EPS = 1e-10;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		if (Math.abs(lplam) < EPS) {
			out.x = 0;
			out.y = lpphi;
		} else if (Math.abs(lpphi) < EPS) {
			out.x = lplam;
			out.y = 0.;
		} else if (Math.abs(Math.abs(lplam) - ProjectionMath.HALFPI) < EPS) {
			out.x = lplam * Math.cos(lpphi);
			out.y = ProjectionMath.HALFPI * Math.sin(lpphi);
		} else if (Math.abs(Math.abs(lpphi) - ProjectionMath.HALFPI) < EPS) {
			out.x = 0;
			out.y = lpphi;
		} else {
			double tb, c, d, m, n, r2, sp;

			tb = ProjectionMath.HALFPI / lplam - lplam / ProjectionMath.HALFPI;
			c = lpphi / ProjectionMath.HALFPI;
			d = (1 - c * c)/((sp = Math.sin(lpphi)) - c);
			r2 = tb / d;
			r2 *= r2;
			m = (tb * sp / d - 0.5 * tb)/(1. + r2);
			n = (sp / r2 + 0.5 * d)/(1. + 1./r2);
			double x = Math.cos(lpphi);
			x = Math.sqrt(m * m + x * x / (1. + r2));
			out.x = ProjectionMath.HALFPI * ( m + (lplam < 0. ? -x : x));
			double y = Math.sqrt(n * n - (sp * sp / r2 + d * sp - 1.) /
				(1. + 1./r2));
			out.y = ProjectionMath.HALFPI * ( n + (lpphi < 0. ? y : -y ));
		}
		return out;
	}

	public String toString() {
		return "Nicolosi Globular";
	}

}
