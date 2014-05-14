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

public class Eckert2Projection extends Projection {

	private final static double FXC = 0.46065886596178063902;
	private final static double FYC = 1.44720250911653531871;
	private final static double C13 = 0.33333333333333333333;
	private final static double ONEEPS = 1.0000001;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.x = FXC * lplam * (out.y = Math.sqrt(4. - 3. * Math.sin(Math.abs(lpphi))));
		out.y = FYC * (2. - out.y);
		if ( lpphi < 0.) out.y = -out.y;
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.x = xyx / (FXC * ( out.y = 2. - Math.abs(xyy) / FYC) );
		out.y = (4. - out.y * out.y) * C13;
		if (Math.abs(out.y) >= 1.) {
			if (Math.abs(out.y) > ONEEPS)	throw new ProjectionException("I");
			else
				out.y = out.y < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		} else
			out.y = Math.asin(out.y);
		if (xyy < 0)
			out.y = -out.y;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Eckert II";
	}

}
