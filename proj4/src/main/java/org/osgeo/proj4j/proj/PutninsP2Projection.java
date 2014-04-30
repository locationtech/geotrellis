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

public class PutninsP2Projection extends Projection {

	private final static double C_x = 1.89490;
	private final static double C_y = 1.71848;
	private final static double C_p = 0.6141848493043784;
	private final static double EPS = 1e-10;
	private final static int NITER = 10;
	private final static double PI_DIV_3 = 1.0471975511965977;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double p, c, s, V;
		int i;

		p = C_p * Math.sin(lpphi);
		s = lpphi * lpphi;
		out.y *= 0.615709 + s * ( 0.00909953 + s * 0.0046292 );
		for (i = NITER; i > 0; --i) {
			c = Math.cos(lpphi);
			s = Math.sin(lpphi);
			out.y -= V = (lpphi + s * (c - 1.) - p) /
				(1. + c * (c - 1.) - s * s);
			if (Math.abs(V) < EPS)
				break;
		}
		if (i == 0)
			out.y = lpphi < 0 ? - PI_DIV_3 : PI_DIV_3;
		out.x = C_x * lplam * (Math.cos(lpphi) - 0.5);
		out.y = C_y * Math.sin(lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double c;

		out.y = ProjectionMath.asin(xyy / C_y);
		out.x = xyx / (C_x * ((c = Math.cos(out.y)) - 0.5));
		out.y = ProjectionMath.asin((out.y + Math.sin(out.y) * (c - 1.)) / C_p);
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Putnins P2";
	}

}
