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

public class PerspectiveProjection extends Projection {

	private double height;
	private double psinph0;
	private double pcosph0;
	private double p;
	private double rp;
	private double pn1;
	private double pfact;
	private double h;
	private double cg;
	private double sg;
	private double sw;
	private double cw;
	private int mode;
	private int tilt;

	private final static double EPS10 = 1.e-10;
	private final static int N_POLE = 0;
	private final static int S_POLE = 1;
	private final static int EQUIT = 2;
	private final static int OBLIQ = 3;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
		double  coslam, cosphi, sinphi;

		sinphi = Math.sin(lpphi);
		cosphi = Math.cos(lpphi);
		coslam = Math.cos(lplam);
		switch (mode) {
		case OBLIQ:
			xy.y = psinph0 * sinphi + pcosph0 * cosphi * coslam;
			break;
		case EQUIT:
			xy.y = cosphi * coslam;
			break;
		case S_POLE:
			xy.y = - sinphi;
			break;
		case N_POLE:
			xy.y = sinphi;
			break;
		}
//		if (xy.y < rp)
//			throw new ProjectionException("");
		xy.y = pn1 / (p - xy.y);
		xy.x = xy.y * cosphi * Math.sin(lplam);
		switch (mode) {
		case OBLIQ:
			xy.y *= (pcosph0 * sinphi -
			   psinph0 * cosphi * coslam);
			break;
		case EQUIT:
			xy.y *= sinphi;
			break;
		case N_POLE:
			coslam = - coslam;
		case S_POLE:
			xy.y *= cosphi * coslam;
			break;
		}
		if (tilt != 0) {
			double yt, ba;

			yt = xy.y * cg + xy.x * sg;
			ba = 1. / (yt * sw * h + cw);
			xy.x = (xy.x * cg - xy.y * sg) * cw * ba;
			xy.y = yt * ba;
		}
		return xy;
	}

	public boolean hasInverse() {
		return false;
	}

/*FIXME
INVERSE(s_inverse); /* spheroid * /
	double  rh, cosz, sinz;

	if (tilt) {
		double bm, bq, yt;

		yt = 1./(pn1 - xy.y * sw);
		bm = pn1 * xy.x * yt;
		bq = pn1 * xy.y * cw * yt;
		xy.x = bm * cg + bq * sg;
		xy.y = bq * cg - bm * sg;
	}
	rh = hypot(xy.x, xy.y);
	if ((sinz = 1. - rh * rh * pfact) < 0.) I_ERROR;
	sinz = (p - Math.sqrt(sinz)) / (pn1 / rh + rh / pn1);
	cosz = sqrt(1. - sinz * sinz);
	if (fabs(rh) <= EPS10) {
		lp.lam = 0.;
		lp.phi = phi0;
	} else {
		switch (mode) {
		case OBLIQ:
			lp.phi = Math.asin(cosz * sinph0 + xy.y * sinz * cosph0 / rh);
			xy.y = (cosz - sinph0 * sin(lp.phi)) * rh;
			xy.x *= sinz * cosph0;
			break;
		case EQUIT:
			lp.phi = Math.asin(xy.y * sinz / rh);
			xy.y = cosz * rh;
			xy.x *= sinz;
			break;
		case N_POLE:
			lp.phi = Math.asin(cosz);
			xy.y = -xy.y;
			break;
		case S_POLE:
			lp.phi = - Math.asin(cosz);
			break;
		}
		lp.lam = Math.atan2(xy.x, xy.y);
	}
	return (lp);
}
*/

	public void initialize() {
		super.initialize();
mode = EQUIT;
height = a;
tilt = 0;
//		if ((height = pj_param(params, "dh").f) <= 0.) E_ERROR(-30);
/*
		if (fabs(fabs(phi0) - Math.HALFPI) < EPS10)
			mode = phi0 < 0. ? S_POLE : N_POLE;
		else if (fabs(phi0) < EPS10)
			mode = EQUIT;
		else {
			mode = OBLIQ;
			psinph0 = Math.sin(phi0);
			pcosph0 = Math.cos(phi0);
		}
*/
		pn1 = height / a; /* normalize by radius */
		p = 1. + pn1;
		rp = 1. / p;
		h = 1. / pn1;
		pfact = (p + 1.) * h;
		es = 0.;
	}
/*FIXME
ENTRY0(nsper)
	tilt = 0;
ENDENTRY(setup(P))
ENTRY0(tpers)
	double omega, gamma;

	omega = pj_param(params, "dtilt").f * DEG_TO_RAD;
	gamma = pj_param(params, "dazi").f * DEG_TO_RAD;
	tilt = 1;
	cg = cos(gamma); sg = sin(gamma);
	cw = cos(omega); sw = sin(omega);
ENDENTRY(setup(P))
*/

	public String toString() {
		return "Perspective";
	}

}
