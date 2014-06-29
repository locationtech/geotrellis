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

public class GnomonicAzimuthalProjection extends AzimuthalProjection {
	
	public GnomonicAzimuthalProjection() {
		this(Math.toRadians(90.0), Math.toRadians(0.0));
	}

	public GnomonicAzimuthalProjection(double projectionLatitude, double projectionLongitude) {
		super(projectionLatitude, projectionLongitude);
		minLatitude = Math.toRadians(0);
		maxLatitude = Math.toRadians(90);
		initialize();
	}
	
	public void initialize() {
		super.initialize();
	}

	public ProjCoordinate project(double lam, double phi, ProjCoordinate xy) {
		double sinphi = Math.sin(phi);
		double cosphi = Math.cos(phi);
		double coslam = Math.cos(lam);

		switch (mode) {
		case EQUATOR:
			xy.y = cosphi * coslam;
			break;
		case OBLIQUE:
			xy.y = sinphi0 * sinphi + cosphi0 * cosphi * coslam;
			break;
		case SOUTH_POLE:
			xy.y = -sinphi;
			break;
		case NORTH_POLE:
			xy.y = sinphi;
			break;
		}
		if (Math.abs(xy.y) <= EPS10)
			throw new ProjectionException();
		xy.x = (xy.y = 1. / xy.y) * cosphi * Math.sin(lam);
		switch (mode) {
		case EQUATOR:
			xy.y *= sinphi;
			break;
		case OBLIQUE:
			xy.y *= cosphi0 * sinphi - sinphi0 * cosphi * coslam;
			break;
		case NORTH_POLE:
			coslam = -coslam;
		case SOUTH_POLE:
			xy.y *= cosphi * coslam;
			break;
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		double  rh, cosz, sinz;

		rh = ProjectionMath.distance(x, y);
		sinz = Math.sin(lp.y = Math.atan(rh));
		cosz = Math.sqrt(1. - sinz * sinz);
		if (Math.abs(rh) <= EPS10) {
			lp.y = projectionLatitude;
			lp.x = 0.;
		} else {
			switch (mode) {
			case OBLIQUE:
				lp.y = cosz * sinphi0 + y * sinz * cosphi0 / rh;
				if (Math.abs(lp.y) >= 1.)
					lp.y = lp.y > 0. ? ProjectionMath.HALFPI : - ProjectionMath.HALFPI;
				else
					lp.y = Math.asin(lp.y);
				y = (cosz - sinphi0 * Math.sin(lp.y)) * rh;
				x *= sinz * cosphi0;
				break;
			case EQUATOR:
				lp.y = y * sinz / rh;
				if (Math.abs(lp.y) >= 1.)
					lp.y = lp.y > 0. ? ProjectionMath.HALFPI : - ProjectionMath.HALFPI;
				else
					lp.y = Math.asin(lp.y);
				y = cosz * rh;
				x *= sinz;
				break;
			case SOUTH_POLE:
				lp.y -= ProjectionMath.HALFPI;
				break;
			case NORTH_POLE:
				lp.y = ProjectionMath.HALFPI - lp.y;
				y = -y;
				break;
			}
			lp.x = Math.atan2(x, y);
		}
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Gnomonic Azimuthal";
	}

}
