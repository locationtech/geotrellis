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


//public class ModStereoProjection extends Projection {
//
//	private final static int MAX_ITER = 10;
//	private final static double LOOP_TOL = 1e-7;
//
//	public Point2D.Double project(double lplam, double lpphi, Point2D.Double xy) {
//		double sinlon, coslon, esphi, chi, schi, cchi, s;
//		COMPLEX p;
//
//		sinlon = Math.sin(lplam);
//		coslon = Math.cos(lplam);
//		esphi = P->e * Math.sin(lpphi);
//		chi = 2. * Math.atan(Math.tan((MapMath.HALFPI + lpphi) * .5) *
//			pow((1. - esphi) / (1. + esphi), P->e * .5)) - MapMath.HALFPI;
//		schi = sin(chi);
//		cchi = cos(chi);
//		s = 2. / (1. + P->schio * schi + P->cchio * cchi * coslon);
//		p.r = s * cchi * sinlon;
//		p.i = s * (P->cchio * schi - P->schio * cchi * coslon);
//		p = pj_zpoly1(p, P->zcoeff, P->n);
//		xy.x = p.r;
//		xy.y = p.i;
//		return xy;
//	}
//
//	public Point2D.Double projectInverse(double xyx, double xyy, Point2D.Double lp) {
//		int nn;
//		COMPLEX p, fxy, fpxy, dp;
//		double den, rh, z, sinz, cosz, chi, phi, dphi, esphi;
//
//		p.r = xy.x;
//		p.i = xy.y;
//		for (nn = 20; nn ;--nn) {
//			fxy = pj_zpolyd1(p, P->zcoeff, P->n, &fpxy);
//			fxy.r -= xy.x;
//			fxy.i -= xy.y;
//			den = fpxy.r * fpxy.r + fpxy.i * fpxy.i;
//			dp.r = -(fxy.r * fpxy.r + fxy.i * fpxy.i) / den;
//			dp.i = -(fxy.i * fpxy.r - fxy.r * fpxy.i) / den;
//			p.r += dp.r;
//			p.i += dp.i;
//			if ((Math.abs(dp.r) + Math.abs(dp.i)) <= EPSLN)
//				break;
//		}
//		if (nn) {
//			rh = Math.hypot(p.r, p.i);
//			z = 2. * Math.atan(.5 * rh);
//			sinz = Math.sin(z);
//			cosz = Math.cos(z);
//			lp.lam = P->lam0;
//			if (Math.abs(rh) <= EPSLN) {
//				lp.phi = P->phi0;
//				return lp;
//			}
//			chi = Math.asin(cosz * P->schio + p.i * sinz * P->cchio / rh);
//			phi = chi;
//			for (nn = 20; nn ;--nn) {
//				esphi = P->e * sin(phi);
//				dphi = 2. * Math.atan(Math.tan((MapMath.HALFPI + chi) * .5) *
//					Math.pow((1. + esphi) / (1. - esphi), P->e * .5)) - HALFPI - phi;
//				phi += dphi;
//				if (fabs(dphi) <= EPSLN)
//					break;
//			}
//		}
//		if (nn) {
//			lp.phi = phi;
//			lp.lam = Math.atan2(p.r * sinz, rh * P->cchio * cosz - p.i * 
//				P->schio * sinz);
//		} else
//			lp.lam = lp.phi = Double.POSITIVE_INFINITY;
//		return lp;
//	}
//
//	public void initialize() {
//		super.initialize();
//
//		double esphi, chio;
//
//		if (P->es) {
//			esphi = P->e * sin(P->phi0);
//			chio = 2. * atan(tan((HALFPI + P->phi0) * .5) *
//				pow((1. - esphi) / (1. + esphi), P->e * .5)) - HALFPI;
//		} else
//			chio = P->phi0;
//		P->schio = sin(chio);
//		P->cchio = cos(chio);
//	}
//
//ENTRY0(mil_os)
//	static COMPLEX /* Miller Oblated Stereographic */
//AB[] = {
//	0.924500,	0.,
//	0.,			0.,
//	0.019430,	0.
//};
//
//	P->n = 2;
//	P->lam0 = DEG_TO_RAD * 20.;
//	P->phi0 = DEG_TO_RAD * 18.;
//	P->zcoeff = AB;
//	P->es = 0.;
//ENDENTRY(setup(P))
//ENTRY0(lee_os)
//	static COMPLEX /* Lee Oblated Stereographic */
//AB[] = {
//	0.721316,	0.,
//	0.,			0.,
//	-0.0088162,	 -0.00617325
//};
//
//	P->n = 2;
//	P->lam0 = DEG_TO_RAD * -165.;
//	P->phi0 = DEG_TO_RAD * -10.;
//	P->zcoeff = AB;
//	P->es = 0.;
//ENDENTRY(setup(P))
//ENTRY0(gs48)
//	static COMPLEX /* 48 United States */
//AB[] = {
//	0.98879,	0.,
//	0.,			0.,
//	-0.050909,	0.,
//	0.,			0.,
//	0.075528,	0.
//};
//
//	P->n = 4;
//	P->lam0 = DEG_TO_RAD * -96.;
//	P->phi0 = DEG_TO_RAD * -39.;
//	P->zcoeff = AB;
//	P->es = 0.;
//	P->a = 6370997.;
//ENDENTRY(setup(P))
//ENTRY0(alsk)
//	static COMPLEX
//ABe[] = { /* Alaska ellipsoid */
//	.9945303,	0.,
//	.0052083,	-.0027404,
//	.0072721,	.0048181,
//	-.0151089,	-.1932526,
//	.0642675,	-.1381226,
//	.3582802,	-.2884586},
//ABs[] = { /* Alaska sphere */
//	.9972523,	0.,
//	.0052513,	-.0041175,
//	.0074606,	.0048125,
//	-.0153783,	-.1968253,
//	.0636871,	-.1408027,
//	.3660976,	-.2937382
//};
//
//	P->n = 5;
//	P->lam0 = DEG_TO_RAD * -152.;
//	P->phi0 = DEG_TO_RAD * 64.;
//	if (P->es) { /* fixed ellipsoid/sphere */
//		P->zcoeff = ABe;
//		P->a = 6378206.4;
//		P->e = sqrt(P->es = 0.00676866);
//	} else {
//		P->zcoeff = ABs;
//		P->a = 6370997.;
//	}
//ENDENTRY(setup(P))
//ENTRY0(gs50)
//	static COMPLEX
//ABe[] = { /* GS50 ellipsoid */
//	.9827497,	0.,
//	.0210669,	.0053804,
//	-.1031415,	-.0571664,
//	-.0323337,	-.0322847,
//	.0502303,	.1211983,
//	.0251805,	.0895678,
//	-.0012315,	-.1416121,
//	.0072202,	-.1317091,
//	-.0194029,	.0759677,
//	-.0210072,	.0834037
//},
//ABs[] = { /* GS50 sphere */
//	.9842990,	0.,
//	.0211642,	.0037608,
//	-.1036018,	-.0575102,
//	-.0329095,	-.0320119,
//	.0499471,	.1223335,
//	.0260460,	.0899805,
//	.0007388,	-.1435792,
//	.0075848,	-.1334108,
//	-.0216473,	.0776645,
//	-.0225161,	.0853673
//};
//
//	P->n = 9;
//	P->lam0 = DEG_TO_RAD * -120.;
//	P->phi0 = DEG_TO_RAD * 45.;
//	if (P->es) { /* fixed ellipsoid/sphere */
//		P->zcoeff = ABe;
//		P->a = 6378206.4;
//		P->e = sqrt(P->es = 0.00676866);
//	} else {
//		P->zcoeff = ABs;
//		P->a = 6370997.;
//	}
//ENDENTRY(setup(P))
//	}
//	
//	public boolean hasInverse() {
//		return true;
//	}
//
//	public String toString() {
//		return "Nell";
//	}
//
//}
