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

public class LandsatProjection extends Projection {

	private double a2, a4, b, c1, c3;
	private double q, t, u, w, p22, sa, ca, xj, rlm, rlm2;

	private final static double TOL = 1e-7;
	private final static double PI_HALFPI = 4.71238898038468985766;
	private final static double TWOPI_HALFPI = 7.85398163397448309610;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
		int l, nn;
		double lamt = 0, xlam, sdsq, c, d, s, lamdp = 0, phidp, lampp, tanph,
			lamtp, cl, sd, sp, fac, sav, tanphi;

		if (lpphi > ProjectionMath.HALFPI)
			lpphi = ProjectionMath.HALFPI;
		else if (lpphi < -ProjectionMath.HALFPI)
			lpphi = -ProjectionMath.HALFPI;
		lampp = lpphi >= 0. ? ProjectionMath.HALFPI : PI_HALFPI;
		tanphi = Math.tan(lpphi);
		for (nn = 0;;) {
			sav = lampp;
			lamtp = lplam + p22 * lampp;
			cl = Math.cos(lamtp);
			if (Math.abs(cl) < TOL)
				lamtp -= TOL;
			fac = lampp - Math.sin(lampp) * (cl < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI);
			for (l = 50; l > 0; --l) {
				lamt = lplam + p22 * sav;
				if (Math.abs(c = Math.cos(lamt)) < TOL)
					lamt -= TOL;
				xlam = (one_es * tanphi * sa + Math.sin(lamt) * ca) / c;
				lamdp = Math.atan(xlam) + fac;
				if (Math.abs(Math.abs(sav) - Math.abs(lamdp)) < TOL)
					break;
				sav = lamdp;
			}
			if (l == 0 || ++nn >= 3 || (lamdp > rlm && lamdp < rlm2))
				break;
			if (lamdp <= rlm)
				lampp = TWOPI_HALFPI;
			else if (lamdp >= rlm2)
				lampp = ProjectionMath.HALFPI;
		}
		if (l != 0) {
			sp = Math.sin(lpphi);
			phidp = ProjectionMath.asin((one_es * ca * sp - sa * Math.cos(lpphi) * 
				Math.sin(lamt)) / Math.sqrt(1. - es * sp * sp));
			tanph = Math.log(Math.tan(ProjectionMath.QUARTERPI + .5 * phidp));
			sd = Math.sin(lamdp);
			sdsq = sd * sd;
			s = p22 * sa * Math.cos(lamdp) * Math.sqrt((1. + t * sdsq)
				 / ((1. + w * sdsq) * (1. + q * sdsq)));
			d = Math.sqrt(xj * xj + s * s);
			xy.x = b * lamdp + a2 * Math.sin(2. * lamdp) + a4 *
				Math.sin(lamdp * 4.) - tanph * s / d;
			xy.y = c1 * sd + c3 * Math.sin(lamdp * 3.) + tanph * xj / d;
		} else
			xy.x = xy.y = Double.POSITIVE_INFINITY;
		return xy;
	}

/*
	public Point2D.Double projectInverse(double xyx, double xyy, Point2D.Double out) {
		int nn;
		double lamt, sdsq, s, lamdp, phidp, sppsq, dd, sd, sl, fac, scl, sav, spp;

		lamdp = xy.x / b;
		nn = 50;
		do {
			sav = lamdp;
			sd = Math.sin(lamdp);
			sdsq = sd * sd;
			s = p22 * sa * Math.cos(lamdp) * sqrt((1. + t * sdsq)
				 / ((1. + w * sdsq) * (1. + q * sdsq)));
			lamdp = xy.x + xy.y * s / xj - a2 * Math.sin(
				2. * lamdp) - a4 * Math.sin(lamdp * 4.) - s / xj * (
				c1 * Math.sin(lamdp) + c3 * Math.sin(lamdp * 3.));
			lamdp /= b;
		} while (Math.abs(lamdp - sav) >= TOL && --nn);
		sl = Math.sin(lamdp);
		fac = exp(sqrt(1. + s * s / xj / xj) * (xy.y - 
			c1 * sl - c3 * Math.sin(lamdp * 3.)));
		phidp = 2. * (Math.atan(fac) - FORTPI);
		dd = sl * sl;
		if (Math.abs(Math.cos(lamdp)) < TOL)
			lamdp -= TOL;
		spp = Math.sin(phidp);
		sppsq = spp * spp;
		lamt = Math.atan(((1. - sppsq * rone_es) * Math.tan(lamdp) * 
			ca - spp * sa * sqrt((1. + q * dd) * (
			1. - sppsq) - sppsq * u) / Math.cos(lamdp)) / (1. - sppsq 
			* (1. + u)));
		sl = lamt >= 0. ? 1. : -1.;
		scl = Math.cos(lamdp) >= 0. ? 1. : -1;
		lamt -= HALFPI * (1. - scl) * sl;
		lp.lam = lamt - p22 * lamdp;
		if (Math.abs(sa) < TOL)
			lp.phi = aasin(spp / sqrt(one_es * one_es + es * sppsq));
		else
			lp.phi = Math.atan((Math.tan(lamdp) * Math.cos(lamt) - ca * Math.sin(lamt)) /
				(one_es * sa));
		return lp;
	}
*/

	private void seraz0(double lam, double mult) {
		double sdsq, h, s, fc, sd, sq, d__1;

		lam *= DTR;
		sd = Math.sin(lam);
		sdsq = sd * sd;
		s = p22 * sa * Math.cos(lam) * Math.sqrt((1. + t * sdsq) / ((
			1. + w * sdsq) * (1. + q * sdsq)));
		d__1 = 1. + q * sdsq;
		h = Math.sqrt((1. + q * sdsq) / (1. + w * sdsq)) * ((1. + 
			w * sdsq) / (d__1 * d__1) - p22 * ca);
		sq = Math.sqrt(xj * xj + s * s);
		b += fc = mult * (h * xj - s * s) / sq;
		a2 += fc * Math.cos(lam + lam);
		a4 += fc * Math.cos(lam * 4.);
		fc = mult * s * (h + xj) / sq;
		c1 += fc * Math.cos(lam);
		c3 += fc * Math.cos(lam * 3.);
	}

	public void initialize() {
		super.initialize();
		int land, path;
		double lam, alf, esc, ess;

//FIXME		land = pj_param(params, "ilsat").i;
land = 1;
		if (land <= 0 || land > 5)
			throw new ProjectionException("-28");
//FIXME		path = pj_param(params, "ipath").i;
path = 120;
		if (path <= 0 || path > (land <= 3 ? 251 : 233))
			throw new ProjectionException("-29");
		if (land <= 3) {
			projectionLongitude = DTR * 128.87 - ProjectionMath.TWOPI / 251. * path;
			p22 = 103.2669323;
			alf = DTR * 99.092;
		} else {
			projectionLongitude = DTR * 129.3 - ProjectionMath.TWOPI / 233. * path;
			p22 = 98.8841202;
			alf = DTR * 98.2;
		}
		p22 /= 1440.;
		sa = Math.sin(alf);
		ca = Math.cos(alf);
		if (Math.abs(ca) < 1e-9)
			ca = 1e-9;
		esc = es * ca * ca;
		ess = es * sa * sa;
		w = (1. - esc) * rone_es;
		w = w * w - 1.;
		q = ess * rone_es;
		t = ess * (2. - es) * rone_es * rone_es;
		u = esc * rone_es;
		xj = one_es * one_es * one_es;
		rlm = Math.PI * (1. / 248. + .5161290322580645);
		rlm2 = rlm + ProjectionMath.TWOPI;
		a2 = a4 = b = c1 = c3 = 0.;
		seraz0(0., 1.);
		for (lam = 9.; lam <= 81.0001; lam += 18.)
			seraz0(lam, 4.);
		for (lam = 18; lam <= 72.0001; lam += 18.)
			seraz0(lam, 2.);
		seraz0(90., 1.);
		a2 /= 30.;
		a4 /= 60.;
		b /= 30.;
		c1 /= 15.;
		c3 /= 45.;
	}
	
	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Landsat";
	}

}

