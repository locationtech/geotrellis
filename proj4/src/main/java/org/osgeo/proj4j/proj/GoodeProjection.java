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

public class GoodeProjection extends Projection {

	private final static double Y_COR = 0.05280;
	private final static double PHI_LIM = .71093078197902358062;

	private SinusoidalProjection sinu = new SinusoidalProjection();
	private MolleweideProjection moll = new MolleweideProjection();

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		if (Math.abs(lpphi) <= PHI_LIM)
			out = sinu.project(lplam, lpphi, out);
		else {
			out = moll.project(lplam, lpphi, out);
			out.y -= lpphi >= 0.0 ? Y_COR : -Y_COR;
		}
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		if (Math.abs(xyy) <= PHI_LIM)
			out = sinu.projectInverse(xyx, xyy, out);
		else {
			xyy += xyy >= 0.0 ? Y_COR : -Y_COR;
			out = moll.projectInverse(xyx, xyy, out);
		}
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Goode Homolosine";
	}

}
