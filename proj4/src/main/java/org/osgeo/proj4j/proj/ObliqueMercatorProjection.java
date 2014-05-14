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
import org.osgeo.proj4j.datum.Ellipsoid;
import org.osgeo.proj4j.util.ProjectionMath;

/**
* Oblique Mercator Projection algorithm is taken from the USGS PROJ package.
*/
public class ObliqueMercatorProjection extends CylindricalProjection {

	private final static double TOL	= 1.0e-7;

	private double lamc, lam1, phi1, lam2, phi2, Gamma, al, bl, el, singam, cosgam, sinrot, cosrot, u_0;
	private boolean ellips, rot;

	public ObliqueMercatorProjection() {
		ellipsoid = Ellipsoid.WGS84;
		projectionLatitude = Math.toRadians(0);
		projectionLongitude = Math.toRadians(0);
		minLongitude = Math.toRadians(-60);
		maxLongitude = Math.toRadians(60);
		minLatitude = Math.toRadians(-80);
		maxLatitude = Math.toRadians(80);
		alpha = Math.toRadians(-45);//FIXME
		initialize();
	}
	
	/**
	* Set up a projection suitable for State Plane Coordinates.
	*/
	public ObliqueMercatorProjection(Ellipsoid ellipsoid, double lon_0, double lat_0, double alpha, double k, double x_0, double y_0) {
		setEllipsoid(ellipsoid);
		lamc = lon_0;
		projectionLatitude = lat_0;
		this.alpha = alpha;
		scaleFactor = k;
		falseEasting = x_0;
		falseNorthing = y_0;
		initialize();
	}
	
	public void initialize() {
		super.initialize();
		double con, com, cosphi0, d, f, h, l, sinphi0, p, j;

		//FIXME-setup rot, alpha, longc,lon/lat1/2
		rot = true;
    lamc = lonc;
    
    // true if alpha provided
    int azi = Double.isNaN(alpha) ? 0 : 1;
		if (azi != 0) { // alpha specified
			if (Math.abs(alpha) <= TOL ||
				Math.abs(Math.abs(projectionLatitude) - ProjectionMath.HALFPI) <= TOL ||
				Math.abs(Math.abs(alpha) - ProjectionMath.HALFPI) <= TOL)
				throw new ProjectionException("Obl 1");
		} else {
			if (Math.abs(phi1 - phi2) <= TOL ||
				(con = Math.abs(phi1)) <= TOL ||
				Math.abs(con - ProjectionMath.HALFPI) <= TOL ||
				Math.abs(Math.abs(projectionLatitude) - ProjectionMath.HALFPI) <= TOL ||
				Math.abs(Math.abs(phi2) - ProjectionMath.HALFPI) <= TOL) throw new ProjectionException("Obl 2");
		}
		com = (spherical = es == 0.) ? 1 : Math.sqrt(one_es);
		if (Math.abs(projectionLatitude) > EPS10) {
			sinphi0 = Math.sin(projectionLatitude);
			cosphi0 = Math.cos(projectionLatitude);
			if (!spherical) {
				con = 1. - es * sinphi0 * sinphi0;
				bl = cosphi0 * cosphi0;
				bl = Math.sqrt(1. + es * bl * bl / one_es);
				al = bl * scaleFactor * com / con;
				d = bl * com / (cosphi0 * Math.sqrt(con));
			} else {
				bl = 1.;
				al = scaleFactor;
				d = 1. / cosphi0;
			}
			if ((f = d * d - 1.) <= 0.)
				f = 0.;
			else {
				f = Math.sqrt(f);
				if (projectionLatitude < 0.)
					f = -f;
			}
			el = f += d;
			if (!spherical)
				el *= Math.pow(ProjectionMath.tsfn(projectionLatitude, sinphi0, e), bl);
			else
				el *= Math.tan(.5 * (ProjectionMath.HALFPI - projectionLatitude));
		} else {
			bl = 1. / com;
			al = scaleFactor;
			el = d = f = 1.;
		}
		if (azi != 0) {
			Gamma = Math.asin(Math.sin(alpha) / d);
			projectionLongitude = lamc - Math.asin((.5 * (f - 1. / f)) *
			   Math.tan(Gamma)) / bl;
		} else {
			if (!spherical) {
				h = Math.pow(ProjectionMath.tsfn(phi1, Math.sin(phi1), e), bl);
				l = Math.pow(ProjectionMath.tsfn(phi2, Math.sin(phi2), e), bl);
			} else {
				h = Math.tan(.5 * (ProjectionMath.HALFPI - phi1));
				l = Math.tan(.5 * (ProjectionMath.HALFPI - phi2));
			}
			f = el / h;
			p = (l - h) / (l + h);
			j = el * el;
			j = (j - l * h) / (j + l * h);
			if ((con = lam1 - lam2) < -Math.PI)
				lam2 -= ProjectionMath.TWOPI;
			else if (con > Math.PI)
				lam2 += ProjectionMath.TWOPI;
			projectionLongitude = ProjectionMath.normalizeLongitude(.5 * (lam1 + lam2) - Math.atan(
			   j * Math.tan(.5 * bl * (lam1 - lam2)) / p) / bl);
			Gamma = Math.atan(2. * Math.sin(bl * ProjectionMath.normalizeLongitude(lam1 - projectionLongitude)) /
			   (f - 1. / f));
			alpha = Math.asin(d * Math.sin(Gamma));
		}
		singam = Math.sin(Gamma);
		cosgam = Math.cos(Gamma);
//		f = MapMath.param(params, "brot_conv").i ? Gamma : alpha;
		f = alpha;//FIXME
		sinrot = Math.sin(f);
		cosrot = Math.cos(f);
//		u_0 = MapMath.param(params, "bno_uoff").i ? 0. :
		u_0 = false ? 0. ://FIXME
			Math.abs(al * Math.atan(Math.sqrt(d * d - 1.) / cosrot) / bl);
		if (projectionLatitude < 0.)
			u_0 = - u_0;
	}

	public ProjCoordinate project(double lam, double phi, ProjCoordinate xy) {
		double con, q, s, ul, us, vl, vs;

		vl = Math.sin(bl * lam);
		if (Math.abs(Math.abs(phi) - ProjectionMath.HALFPI) <= EPS10) {
			ul = phi < 0. ? -singam : singam;
			us = al * phi / bl;
		} else {
			q = el / (!spherical ? Math.pow(ProjectionMath.tsfn(phi, Math.sin(phi), e), bl)
				: Math.tan(.5 * (ProjectionMath.HALFPI - phi)));
			s = .5 * (q - 1. / q);
			ul = 2. * (s * singam - vl * cosgam) / (q + 1. / q);
			con = Math.cos(bl * lam);
			if (Math.abs(con) >= TOL) {
				us = al * Math.atan((s * cosgam + vl * singam) / con) / bl;
				if (con < 0.)
					us += Math.PI * al / bl;
			} else
				us = al * bl * lam;
		}
		if (Math.abs(Math.abs(ul) - 1.) <= EPS10) throw new ProjectionException("Obl 3");
		vs = .5 * al * Math.log((1. - ul) / (1. + ul)) / bl;
		us -= u_0;
		if (!rot) {
			xy.x = us;
			xy.y = vs;
		} else {
			xy.x = vs * cosrot + us * sinrot;
			xy.y = us * cosrot - vs * sinrot;
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		double q, s, ul, us, vl, vs;

		if (! rot) {
			us = x;
			vs = y;
		} else {
			vs = x * cosrot - y * sinrot;
			us = y * cosrot + x * sinrot;
		}
		us += u_0;
		q = Math.exp(- bl * vs / al);
		s = .5 * (q - 1. / q);
		vl = Math.sin(bl * us / al);
		ul = 2. * (vl * cosgam + s * singam) / (q + 1. / q);
		if (Math.abs(Math.abs(ul) - 1.) < EPS10) {
			lp.x = 0.;
			lp.y = ul < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		} else {
			lp.y = el / Math.sqrt((1. + ul) / (1. - ul));
			if (!spherical) {
				lp.y = ProjectionMath.phi2(Math.pow(lp.y, 1. / bl), e);
			} else
				lp.y = ProjectionMath.HALFPI - 2. * Math.atan(lp.y);
			lp.x = - Math.atan2((s * cosgam -
				vl * singam), Math.cos(bl * us / al)) / bl;
		}
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Oblique Mercator";
	}

}
