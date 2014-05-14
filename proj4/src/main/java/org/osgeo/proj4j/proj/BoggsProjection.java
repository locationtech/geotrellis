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

public class BoggsProjection extends PseudoCylindricalProjection {

	private final static int NITER = 20;
	private final static double EPS = 1e-7;
	private final static double ONETOL = 1.000001;
	private final static double FXC = 2.00276;
	private final static double FXC2 = 1.11072;
	private final static double FYC = 0.49931;
	private final static double FYC2 = 1.41421356237309504880;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double theta, th1, c;
		int i;

		theta = lpphi;
		if (Math.abs(Math.abs(lpphi) - ProjectionMath.HALFPI) < EPS)
			out.x = 0.;
		else {
			c = Math.sin(theta) * Math.PI;
			for (i = NITER; i > 0; --i) {
				theta -= th1 = (theta + Math.sin(theta) - c) /
					(1. + Math.cos(theta));
				if (Math.abs(th1) < EPS) break;
			}
			theta *= 0.5;
			out.x = FXC * lplam / (1. / Math.cos(lpphi) + FXC2 / Math.cos(theta));
		}
		out.y = FYC * (lpphi + FYC2 * Math.sin(theta));
		return out;
	}

	/**
	 * Returns true if this projection is equal area
	 */
	public boolean isEqualArea() {
		return true;
	}

	public boolean hasInverse() {
		return false;
	}

	public String toString() {
		return "Boggs Eumorphic";
	}

}
