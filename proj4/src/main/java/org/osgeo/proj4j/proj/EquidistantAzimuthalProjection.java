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

public class EquidistantAzimuthalProjection extends AzimuthalProjection {
	
	private final static double TOL = 1.e-8;

	private int mode;
	private double[] en;
	private double M1;
	private double N1;
	private double Mp;
	private double He;
	private double G;
	private double sinphi0, cosphi0;
	
	public EquidistantAzimuthalProjection() {
		this(Math.toRadians(90.0), Math.toRadians(0.0));
	}

	public EquidistantAzimuthalProjection(double projectionLatitude, double projectionLongitude) {
		super(projectionLatitude, projectionLongitude);
		initialize();
	}
	
	public Object clone() {
		EquidistantAzimuthalProjection p = (EquidistantAzimuthalProjection)super.clone();
		if (en != null)
			p.en = (double[])en.clone();
		return p;
	}
	
	public void initialize() {
		super.initialize();
		if (Math.abs(Math.abs(projectionLatitude) - ProjectionMath.HALFPI) < EPS10) {
			mode = projectionLatitude < 0. ? SOUTH_POLE : NORTH_POLE;
			sinphi0 = projectionLatitude < 0. ? -1. : 1.;
			cosphi0 = 0.;
		} else if (Math.abs(projectionLatitude) < EPS10) {
			mode = EQUATOR;
			sinphi0 = 0.;
			cosphi0 = 1.;
		} else {
			mode = OBLIQUE;
			sinphi0 = Math.sin(projectionLatitude);
			cosphi0 = Math.cos(projectionLatitude);
		}
		if (!spherical) {
			en = ProjectionMath.enfn(es);
			switch (mode) {
			case NORTH_POLE:
				Mp = ProjectionMath.mlfn(ProjectionMath.HALFPI, 1., 0., en);
				break;
			case SOUTH_POLE:
				Mp = ProjectionMath.mlfn(-ProjectionMath.HALFPI, -1., 0., en);
				break;
			case EQUATOR:
			case OBLIQUE:
				N1 = 1. / Math.sqrt(1. - es * sinphi0 * sinphi0);
				G = sinphi0 * (He = e / Math.sqrt(one_es));
				He *= cosphi0;
				break;
			}
		}
	}

	public ProjCoordinate project(double lam, double phi, ProjCoordinate xy) {
		if (spherical) {
			double  coslam, cosphi, sinphi;

			sinphi = Math.sin(phi);
			cosphi = Math.cos(phi);
			coslam = Math.cos(lam);
			switch (mode) {
			case EQUATOR:
			case OBLIQUE:
				if (mode == EQUATOR)
					xy.y = cosphi * coslam;
				else
					xy.y = sinphi0 * sinphi + cosphi0 * cosphi * coslam;
				if (Math.abs(Math.abs(xy.y) - 1.) < TOL)
					if (xy.y < 0.)
						throw new ProjectionException(); 
					else
						xy.x = xy.y = 0.;
				else {
					xy.y = Math.acos(xy.y);
					xy.y /= Math.sin(xy.y);
					xy.x = xy.y * cosphi * Math.sin(lam);
					xy.y *= (mode == EQUATOR) ? sinphi :
				   		cosphi0 * sinphi - sinphi0 * cosphi * coslam;
				}
				break;
			case NORTH_POLE:
				phi = -phi;
				coslam = -coslam;
			case SOUTH_POLE:
				if (Math.abs(phi - ProjectionMath.HALFPI) < EPS10)
					throw new ProjectionException();
				xy.x = (xy.y = (ProjectionMath.HALFPI + phi)) * Math.sin(lam);
				xy.y *= coslam;
				break;
			}
		} else {
			double  coslam, cosphi, sinphi, rho, s, H, H2, c, Az, t, ct, st, cA, sA;

			coslam = Math.cos(lam);
			cosphi = Math.cos(phi);
			sinphi = Math.sin(phi);
			switch (mode) {
			case NORTH_POLE:
				coslam = - coslam;
			case SOUTH_POLE:
				xy.x = (rho = Math.abs(Mp - ProjectionMath.mlfn(phi, sinphi, cosphi, en))) *
					Math.sin(lam);
				xy.y = rho * coslam;
				break;
			case EQUATOR:
			case OBLIQUE:
				if (Math.abs(lam) < EPS10 && Math.abs(phi - projectionLatitude) < EPS10) {
					xy.x = xy.y = 0.;
					break;
				}
				t = Math.atan2(one_es * sinphi + es * N1 * sinphi0 *
					Math.sqrt(1. - es * sinphi * sinphi), cosphi);
				ct = Math.cos(t); st = Math.sin(t);
				Az = Math.atan2(Math.sin(lam) * ct, cosphi0 * st - sinphi0 * coslam * ct);
				cA = Math.cos(Az); sA = Math.sin(Az);
				s = ProjectionMath.asin( Math.abs(sA) < TOL ?
					(cosphi0 * st - sinphi0 * coslam * ct) / cA :
					Math.sin(lam) * ct / sA );
				H = He * cA;
				H2 = H * H;
				c = N1 * s * (1. + s * s * (- H2 * (1. - H2)/6. +
					s * ( G * H * (1. - 2. * H2 * H2) / 8. +
					s * ((H2 * (4. - 7. * H2) - 3. * G * G * (1. - 7. * H2)) /
					120. - s * G * H / 48.))));
				xy.x = c * sA;
				xy.y = c * cA;
				break;
			}
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		if (spherical) {
			double cosc, c_rh, sinc;

			if ((c_rh = ProjectionMath.distance(x, y)) > Math.PI) {
				if (c_rh - EPS10 > Math.PI)
					throw new ProjectionException(); 
				c_rh = Math.PI;
			} else if (c_rh < EPS10) {
				lp.y = projectionLatitude;
				lp.x = 0.;
				return lp;
			}
			if (mode == OBLIQUE || mode == EQUATOR) {
				sinc = Math.sin(c_rh);
				cosc = Math.cos(c_rh);
				if (mode == EQUATOR) {
					lp.y = ProjectionMath.asin(y * sinc / c_rh);
					x *= sinc;
					y = cosc * c_rh;
				} else {
					lp.y = ProjectionMath.asin(cosc * sinphi0 + y * sinc * cosphi0 /
						c_rh);
					y = (cosc - sinphi0 * Math.sin(lp.y)) * c_rh;
					x *= sinc * cosphi0;
				}
				lp.x = y == 0. ? 0. : Math.atan2(x, y);
			} else if (mode == NORTH_POLE) {
				lp.y = ProjectionMath.HALFPI - c_rh;
				lp.x = Math.atan2(x, -y);
			} else {
				lp.y = c_rh - ProjectionMath.HALFPI;
				lp.x = Math.atan2(x, y);
			}
		} else {
			double c, Az, cosAz, A, B, D, E, F, psi, t;
			int i;

			if ((c = ProjectionMath.distance(x, y)) < EPS10) {
				lp.y = projectionLatitude;
				lp.x = 0.;
				return (lp);
			}
			if (mode == OBLIQUE || mode == EQUATOR) {
				cosAz = Math.cos(Az = Math.atan2(x, y));
				t = cosphi0 * cosAz;
				B = es * t / one_es;
				A = - B * t;
				B *= 3. * (1. - A) * sinphi0;
				D = c / N1;
				E = D * (1. - D * D * (A * (1. + A) / 6. + B * (1. + 3.*A) * D / 24.));
				F = 1. - E * E * (A / 2. + B * E / 6.);
				psi = ProjectionMath.asin(sinphi0 * Math.cos(E) + t * Math.sin(E));
				lp.x = ProjectionMath.asin(Math.sin(Az) * Math.sin(E) / Math.cos(psi));
				if ((t = Math.abs(psi)) < EPS10)
					lp.y = 0.;
				else if (Math.abs(t - ProjectionMath.HALFPI) < 0.)
					lp.y = ProjectionMath.HALFPI;
				else
					lp.y = Math.atan((1. - es * F * sinphi0 / Math.sin(psi)) * Math.tan(psi) / one_es);
			} else {
				lp.y = ProjectionMath.inv_mlfn(mode == NORTH_POLE ? Mp - c : Mp + c, es, en);
				lp.x = Math.atan2(x, mode == NORTH_POLE ? -y : y);
			}
		}
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Equidistant Azimuthal";
	}
	
}

