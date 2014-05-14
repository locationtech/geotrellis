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


public class DenoyerProjection extends Projection {

	public final static double C0 = 0.95;
	public final static double C1 = -.08333333333333333333;
	public final static double C3 = 0.00166666666666666666;
	public final static double D1 = 0.9;
	public final static double D5 = 0.03;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.y = lpphi;
		out.x = lplam;
		double aphi = Math.abs(lplam);
		out.x *= Math.cos((C0 + aphi * (C1 + aphi * aphi * C3)) *
			(lpphi * (D1 + D5 * lpphi * lpphi * lpphi * lpphi)));
		return out;
	}

	public boolean parallelsAreParallel() {
		return true;
	}

	public boolean hasInverse() {
		return false;
	}

	public String toString() {
		return "Denoyer Semi-elliptical";
	}

}
