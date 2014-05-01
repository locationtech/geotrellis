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

public class Eckert4Projection extends Projection {

	private final static double C_x = .42223820031577120149;
	private final static double C_y = 1.32650042817700232218;
	private final static double RC_y = .75386330736002178205;
	private final static double C_p = 3.57079632679489661922;
	private final static double RC_p = .28004957675577868795;
	private final static double EPS = 1e-7;
	private final int NITER = 6;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double p, V, s, c;
		int i;

		p = C_p * Math.sin(lpphi);
		V = lpphi * lpphi;
		lpphi *= 0.895168 + V * ( 0.0218849 + V * 0.00826809 );
		for (i = NITER; i > 0; --i) {
			c = Math.cos(lpphi);
			s = Math.sin(lpphi);
			lpphi -= V = (lpphi + s * (c + 2.) - p) /
				(1. + c * (c + 2.) - s * s);
			if (Math.abs(V) < EPS)
				break;
		}
		if (i == 0) {
			out.x = C_x * lplam;
			out.y = lpphi < 0. ? -C_y : C_y;
		} else {
			out.x = C_x * lplam * (1. + Math.cos(lpphi));
			out.y = C_y * Math.sin(lpphi);
		}
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double c;

		out.y = ProjectionMath.asin(xyy / C_y);
		out.x = xyx / (C_x * (1. + (c = Math.cos(out.y))));
		out.y = ProjectionMath.asin((out.y + Math.sin(out.y) * (c + 2.)) / C_p);
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public boolean isEqualArea() {
     return true;
	}

	public String toString() {
		return "Eckert IV";
	}

}
