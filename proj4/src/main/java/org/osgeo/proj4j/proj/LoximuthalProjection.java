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

public class LoximuthalProjection extends PseudoCylindricalProjection {

	private final static double FC = .92131773192356127802;
	private final static double RP = .31830988618379067154;
	private final static double EPS = 1e-8;
	
	private double phi1;
	private double cosphi1;
	private double tanphi1;

	public LoximuthalProjection() {
		phi1 = Math.toRadians(40.0);//FIXME - param
		cosphi1 = Math.cos(phi1);
		tanphi1 = Math.tan(ProjectionMath.QUARTERPI + 0.5 * phi1);
	}

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double x;
		double y = lpphi - phi1;
		if (y < EPS)
			x = lplam * cosphi1;
		else {
			x = ProjectionMath.QUARTERPI + 0.5 * lpphi;
			if (Math.abs(x) < EPS || Math.abs(Math.abs(x) - ProjectionMath.HALFPI) < EPS)
				x = 0.;
			else
				x = lplam * y / Math.log( Math.tan(x) / tanphi1 );
		}
		out.x = x;
		out.y = y;
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double latitude = xyy + phi1;
		double longitude;
		if (Math.abs(xyy) < EPS)
			longitude = xyx / cosphi1;
		else if (Math.abs( longitude = ProjectionMath.QUARTERPI + 0.5 * xyy ) < EPS ||
			Math.abs(Math.abs(xyx) -ProjectionMath.HALFPI) < EPS)
			longitude = 0.;
		else
			longitude = xyx * Math.log( Math.tan(longitude) / tanphi1 ) / xyy;

		out.x = longitude;
		out.y = latitude;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Loximuthal";
	}

}
