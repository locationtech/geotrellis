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

public class BipolarProjection extends Projection {

	private boolean	noskew;

	private final static double EPS = 1e-10;
	private final static double EPS10 = 1e-10;
	private final static double ONEEPS = 1.000000001;
	private final static int NITER = 10;
	private final static double lamB = -.34894976726250681539;
	private final static double n = .63055844881274687180;
	private final static double F = 1.89724742567461030582;
	private final static double Azab = .81650043674686363166;
	private final static double Azba = 1.82261843856185925133;
	private final static double T = 1.27246578267089012270;
	private final static double rhoc = 1.20709121521568721927;
	private final static double cAzc = .69691523038678375519;
	private final static double sAzc = .71715351331143607555;
	private final static double C45 = .70710678118654752469;
	private final static double S45 = .70710678118654752410;
	private final static double C20 = .93969262078590838411;
	private final static double S20 = -.34202014332566873287;
	private final static double R110 = 1.91986217719376253360;
	private final static double R104 = 1.81514242207410275904;

	public BipolarProjection() {
		minLatitude = Math.toRadians(-80);
		maxLatitude = Math.toRadians(80);
		projectionLongitude = Math.toRadians(-90);
		minLongitude = Math.toRadians(-90);
		maxLongitude = Math.toRadians(90);
	}
	
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double cphi, sphi, tphi, t, al, Az, z, Av, cdlam, sdlam, r;
		boolean tag;

		cphi = Math.cos(lpphi);
		sphi = Math.sin(lpphi);
		cdlam = Math.cos(sdlam = lamB - lplam);
		sdlam = Math.sin(sdlam);
		if (Math.abs(Math.abs(lpphi) - ProjectionMath.HALFPI) < EPS10) {
			Az = lpphi < 0. ? Math.PI : 0.;
			tphi = Double.MAX_VALUE;
		} else {
			tphi = sphi / cphi;
			Az = Math.atan2(sdlam , C45 * (tphi - cdlam));
		}
		if (tag = (Az > Azba)) {
			cdlam = Math.cos(sdlam = lplam + R110);
			sdlam = Math.sin(sdlam);
			z = S20 * sphi + C20 * cphi * cdlam;
			if (Math.abs(z) > 1.) {
				if (Math.abs(z) > ONEEPS)
					throw new ProjectionException("F");
				else z = z < 0. ? -1. : 1.;
			} else
				z = Math.acos(z);
			if (tphi != Double.MAX_VALUE)
				Az = Math.atan2(sdlam, (C20 * tphi - S20 * cdlam));
			Av = Azab;
			out.y = rhoc;
		} else {
			z = S45 * (sphi + cphi * cdlam);
			if (Math.abs(z) > 1.) {
				if (Math.abs(z) > ONEEPS)
					throw new ProjectionException("F");
				else z = z < 0. ? -1. : 1.;
			} else
				z = Math.acos(z);
			Av = Azba;
			out.y = -rhoc;
		}
		if (z < 0.) throw new ProjectionException("F");
		r = F * (t = Math.pow(Math.tan(.5 * z), n));
		if ((al = .5 * (R104 - z)) < 0.)
			throw new ProjectionException("F");
		al = (t + Math.pow(al, n)) / T;
		if (Math.abs(al) > 1.) {
			if (Math.abs(al) > ONEEPS)
				throw new ProjectionException("F");
			else al = al < 0. ? -1. : 1.;
		} else
			al = Math.acos(al);
		if (Math.abs(t = n * (Av - Az)) < al)
			r /= Math.cos(al + (tag ? t : -t));
		out.x = r * Math.sin(t);
		out.y += (tag ? -r : r) * Math.cos(t);
		if (noskew) {
			t = out.x;
			out.x = -out.x * cAzc - out.y * sAzc; 
			out.y = -out.y * cAzc + t * sAzc; 
		}
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double t, r, rp, rl, al, z = 0, fAz, Az, s, c, Av;
		boolean neg;
		int i;

		if (noskew) {
			t = xyx;
			out.x = -xyx * cAzc + xyy * sAzc; 
			out.y = -xyy * cAzc - t * sAzc; 
		}
		if (neg = (xyx < 0.)) {
			out.y = rhoc - xyy;
			s = S20;
			c = C20;
			Av = Azab;
		} else {
			out.y += rhoc;
			s = S45;
			c = C45;
			Av = Azba;
		}
		rl = rp = r = ProjectionMath.distance(xyx, xyy);
		fAz = Math.abs(Az = Math.atan2(xyx, xyy));
		for (i = NITER; i > 0; --i) {
			z = 2. * Math.atan(Math.pow(r / F,1 / n));
			al = Math.acos((Math.pow(Math.tan(.5 * z), n) +
			   Math.pow(Math.tan(.5 * (R104 - z)), n)) / T);
			if (fAz < al)
				r = rp * Math.cos(al + (neg ? Az : -Az));
			if (Math.abs(rl - r) < EPS)
				break;
			rl = r;
		}
		if (i == 0) throw new ProjectionException("I");
		Az = Av - Az / n;
		out.y = Math.asin(s * Math.cos(z) + c * Math.sin(z) * Math.cos(Az));
		out.x = Math.atan2(Math.sin(Az), c / Math.tan(z) - s * Math.cos(Az));
		if (neg)
			out.x -= R110;
		else
			out.x = lamB - out.x;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public void initialize() { // bipc
		super.initialize();
//		noskew = pj_param(params, "bns").i;//FIXME
	}

	public String toString() {
		return "Bipolar Conic of Western Hemisphere";
	}

}
