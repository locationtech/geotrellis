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

public class McBrydeThomasFlatPolarParabolicProjection extends Projection {

	private final static double CS = .95257934441568037152;
	private final static double FXC = .92582009977255146156;
	private final static double FYC = 3.40168025708304504493;
	private final static double C23 = .66666666666666666666;
	private final static double C13 = .33333333333333333333;
	private final static double ONEEPS = 1.0000001;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.y = Math.asin(CS * Math.sin(lpphi));
		out.x = FXC * lplam * (2. * Math.cos(C23 * lpphi) - 1.);
		out.y = FYC * Math.sin(C13 * lpphi);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.y = xyy / FYC;
		if (Math.abs(out.y) >= 1.) {
			if (Math.abs(out.y) > ONEEPS)	throw new ProjectionException("I");
			else	out.y = (out.y < 0.) ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		} else
			out.y = Math.asin(out.y);
		out.x = xyx / ( FXC * (2. * Math.cos(C23 * (out.y *= 3.)) - 1.) );
		if (Math.abs(out.y = Math.sin(out.y) / CS) >= 1.) {
			if (Math.abs(out.y) > ONEEPS)	throw new ProjectionException("I");
			else	out.y = (out.y < 0.) ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		} else
			out.y = Math.asin(out.y);
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "McBride-Thomas Flat-Polar Parabolic";
	}

}
