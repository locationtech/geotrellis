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

import org.osgeo.proj4j.*;
import org.osgeo.proj4j.ProjectionException;
import org.osgeo.proj4j.util.ProjectionMath;

public class StereographicAzimuthalProjection extends AzimuthalProjection {

	private final static double TOL = 1.e-8;
	
	private double akm1;
	
	public StereographicAzimuthalProjection() {
		this(Math.toRadians(90.0), Math.toRadians(0.0));
	}

	public StereographicAzimuthalProjection(double projectionLatitude, double projectionLongitude) {
		super(projectionLatitude, projectionLongitude);
		initialize();
	}
	
	public void setupUPS(int pole) {
		projectionLatitude = (pole == SOUTH_POLE) ? -ProjectionMath.HALFPI: ProjectionMath.HALFPI;
		projectionLongitude = 0.0;
		scaleFactor = 0.994;
		falseEasting = 2000000.0;
		falseNorthing = 2000000.0;
		trueScaleLatitude = ProjectionMath.HALFPI;
		initialize();
	}
	
	public void initialize() {
		double t;

		super.initialize();
		if (Math.abs((t = Math.abs(projectionLatitude)) - ProjectionMath.HALFPI) < EPS10)
			mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
		else
			mode = t > EPS10 ? OBLIQUE : EQUATOR;
		trueScaleLatitude = Math.abs(trueScaleLatitude);
		if (! spherical) {
			double X;

			switch (mode) {
			case NORTH_POLE:
			case SOUTH_POLE:
				if (Math.abs(trueScaleLatitude - ProjectionMath.HALFPI) < EPS10)
					akm1 = 2. * scaleFactor /
					   Math.sqrt(Math.pow(1+e,1+e)*Math.pow(1-e,1-e));
				else {
					akm1 = Math.cos(trueScaleLatitude) /
					   ProjectionMath.tsfn(trueScaleLatitude, t = Math.sin(trueScaleLatitude), e);
					t *= e;
					akm1 /= Math.sqrt(1. - t * t);
				}
				break;
			case EQUATOR:
				akm1 = 2. * scaleFactor;
				break;
			case OBLIQUE:
				t = Math.sin(projectionLatitude);
				X = 2. * Math.atan(ssfn(projectionLatitude, t, e)) - ProjectionMath.HALFPI;
				t *= e;
				akm1 = 2. * scaleFactor * Math.cos(projectionLatitude) / Math.sqrt(1. - t * t);
				sinphi0 = Math.sin(X);
				cosphi0 = Math.cos(X);
				break;
			}
		} else {
			switch (mode) {
			case OBLIQUE:
				sinphi0 = Math.sin(projectionLatitude);
				cosphi0 = Math.cos(projectionLatitude);
			case EQUATOR:
				akm1 = 2. * scaleFactor;
				break;
			case SOUTH_POLE:
			case NORTH_POLE:
				akm1 = Math.abs(trueScaleLatitude - ProjectionMath.HALFPI) >= EPS10 ?
				   Math.cos(trueScaleLatitude) / Math.tan(ProjectionMath.QUARTERPI - .5 * trueScaleLatitude) :
				   2. * scaleFactor ;
				break;
			}
		}
	}

	public ProjCoordinate project(double lam, double phi, ProjCoordinate xy) {
		double coslam = Math.cos(lam);
		double sinlam = Math.sin(lam);
		double sinphi = Math.sin(phi);

		if (spherical) {
			double cosphi = Math.cos(phi);

			switch (mode) {
			case EQUATOR:
				xy.y = 1. + cosphi * coslam;
				if (xy.y <= EPS10)
					throw new ProjectionException();
				xy.x = (xy.y = akm1 / xy.y) * cosphi * sinlam;
				xy.y *= sinphi;
				break;
			case OBLIQUE:
				xy.y = 1. + sinphi0 * sinphi + cosphi0 * cosphi * coslam;
				if (xy.y <= EPS10)
					throw new ProjectionException();
				xy.x = (xy.y = akm1 / xy.y) * cosphi * sinlam;
				xy.y *= cosphi0 * sinphi - sinphi0 * cosphi * coslam;
				break;
			case NORTH_POLE:
				coslam = - coslam;
				phi = - phi;
			case SOUTH_POLE:
				if (Math.abs(phi - ProjectionMath.HALFPI) < TOL)
					throw new ProjectionException();
				xy.x = sinlam * ( xy.y = akm1 * Math.tan(ProjectionMath.QUARTERPI + .5 * phi) );
				xy.y *= coslam;
				break;
			}
		} else {
			double sinX = 0, cosX = 0, X, A;

			if (mode == OBLIQUE || mode == EQUATOR) {
				sinX = Math.sin(X = 2. * Math.atan(ssfn(phi, sinphi, e)) - ProjectionMath.HALFPI);
				cosX = Math.cos(X);
			}
			switch (mode) {
			case OBLIQUE:
				A = akm1 / (cosphi0 * (1. + sinphi0 * sinX + cosphi0 * cosX * coslam));
				xy.y = A * (cosphi0 * sinX - sinphi0 * cosX * coslam);
				xy.x = A * cosX;
				break;
			case EQUATOR:
				A = 2. * akm1 / (1. + cosX * coslam);
				xy.y = A * sinX;
				xy.x = A * cosX;
				break;
			case SOUTH_POLE:
				phi = -phi;
				coslam = -coslam;
				sinphi = -sinphi;
			case NORTH_POLE:
				xy.x = akm1 * ProjectionMath.tsfn(phi, sinphi, e);
				xy.y = - xy.x * coslam;
				break;
			}
			xy.x = xy.x * sinlam;
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		if (spherical) {
			double  c, rh, sinc, cosc;

			sinc = Math.sin(c = 2. * Math.atan((rh = ProjectionMath.distance(x, y)) / akm1));
			cosc = Math.cos(c);
			lp.x = 0.;
			switch (mode) {
			case EQUATOR:
				if (Math.abs(rh) <= EPS10)
					lp.y = 0.;
				else
					lp.y = Math.asin(y * sinc / rh);
				if (cosc != 0. || x != 0.)
					lp.x = Math.atan2(x * sinc, cosc * rh);
				break;
			case OBLIQUE:
				if (Math.abs(rh) <= EPS10)
					lp.y = projectionLatitude;
				else
					lp.y = Math.asin(cosc * sinphi0 + y * sinc * cosphi0 / rh);
				if ((c = cosc - sinphi0 * Math.sin(lp.y)) != 0. || x != 0.)
					lp.x = Math.atan2(x * sinc * cosphi0, c * rh);
				break;
			case NORTH_POLE:
				y = -y;
			case SOUTH_POLE:
				if (Math.abs(rh) <= EPS10)
					lp.y = projectionLatitude;
				else
					lp.y = Math.asin(mode == SOUTH_POLE ? - cosc : cosc);
				lp.x = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
				break;
			}
		} else {
			double cosphi, sinphi, tp, phi_l, rho, halfe, halfpi;

			rho = ProjectionMath.distance(x, y);
			switch (mode) {
			case OBLIQUE:
			case EQUATOR:
			default:	// To prevent the compiler complaining about uninitialized vars.
				cosphi = Math.cos( tp = 2. * Math.atan2(rho * cosphi0 , akm1) );
				sinphi = Math.sin(tp);
				if (rho <= 0) {
				  phi_l = Math.asin(cosphi * sinphi0);
				}
				else {
				  phi_l = Math.asin(cosphi * sinphi0 + (y * sinphi * cosphi0 / rho));
				}
				tp = Math.tan(.5 * (ProjectionMath.HALFPI + phi_l));
				x *= sinphi;
				y = rho * cosphi0 * cosphi - y * sinphi0* sinphi;
				halfpi = ProjectionMath.HALFPI;
				halfe = .5 * e;
				break;
			case NORTH_POLE:
				y = -y;
			case SOUTH_POLE:
				phi_l = ProjectionMath.HALFPI - 2. * Math.atan(tp = - rho / akm1);
				halfpi = -ProjectionMath.HALFPI;
				halfe = -.5 * e;
				break;
			}
			for (int i = 8; i-- != 0; phi_l = lp.y) {
				sinphi = e * Math.sin(phi_l);
				lp.y = 2. * Math.atan(tp * Math.pow((1.+sinphi)/(1.-sinphi), halfe)) - halfpi;
				if (Math.abs(phi_l - lp.y) < EPS10) {
					if (mode == SOUTH_POLE)
						lp.y = -lp.y;
					lp.x = (x == 0. && y == 0.) ? 0. : Math.atan2(x, y);
					return lp;
				}
			}
			throw new ConvergenceFailureException("Iteration didn't converge");
		}
		return lp;
	}
	
	/**
	 * Returns true if this projection is conformal
	 */
	public boolean isConformal() {
		return true;
	}
	
	public boolean hasInverse() {
		return true;
	}

	private double ssfn(double phit, double sinphi, double eccen) {
		sinphi *= eccen;
		return Math.tan (.5 * (ProjectionMath.HALFPI + phit)) *
		   Math.pow((1. - sinphi) / (1. + sinphi), .5 * eccen);
	}

	public String toString() {
		return "Stereographic Azimuthal";
	}

}

