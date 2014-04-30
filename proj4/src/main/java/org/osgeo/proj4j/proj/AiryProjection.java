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

public class AiryProjection extends Projection {

	private double p_halfpi;
	private double sinph0;
	private double cosph0;
	private double Cb;
	private int mode;
	private boolean no_cut = true;	/* do not cut at hemisphere limit */

	private final static double EPS = 1.e-10;
	private final static int N_POLE = 0;
	private final static int S_POLE = 1;
	private final static int EQUIT = 2;
	private final static int OBLIQ = 3;

	public AiryProjection() {
		minLatitude = Math.toRadians(-60);
		maxLatitude = Math.toRadians(60);
		minLongitude = Math.toRadians(-90);
		maxLongitude = Math.toRadians(90);
		initialize();
	}
	
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double sinlam, coslam, cosphi, sinphi, t, s, Krho, cosz;

		sinlam = Math.sin(lplam);
		coslam = Math.cos(lplam);
		switch (mode) {
		case EQUIT:
		case OBLIQ:
			sinphi = Math.sin(lpphi);
			cosphi = Math.cos(lpphi);
			cosz = cosphi * coslam;
			if (mode == OBLIQ)
				cosz = sinph0 * sinphi + cosph0 * cosz;
			if (!no_cut && cosz < -EPS)
				throw new ProjectionException("F");
			s = 1. - cosz;
			if (Math.abs(s) > EPS) {
				t = 0.5 * (1. + cosz);
				Krho = -Math.log(t)/s - Cb / t;
			} else
				Krho = 0.5 - Cb;
			out.x = Krho * cosphi * sinlam;
			if (mode == OBLIQ)
				out.y = Krho * (cosph0 * sinphi -
					sinph0 * cosphi * coslam);
			else
				out.y = Krho * sinphi;
			break;
		case S_POLE:
		case N_POLE:
			out.y = Math.abs(p_halfpi - lpphi);
			if (!no_cut && (lpphi - EPS) > ProjectionMath.HALFPI)
				throw new ProjectionException("F");
			if ((out.y *= 0.5) > EPS) {
				t = Math.tan(lpphi);
				Krho = -2.*(Math.log(Math.cos(lpphi)) / t + t * Cb);
				out.x = Krho * sinlam;
				out.y = Krho * coslam;
				if (mode == N_POLE)
					out.y = -out.y;
			} else
				out.x = out.y = 0.;
		}
		return out;
	}

	public void initialize() { // airy
		super.initialize();

		double beta;

//		no_cut = pj_param(params, "bno_cut").i;
//		beta = 0.5 * (MapMath.HALFPI - pj_param(params, "rlat_b").f);
		no_cut = false;//FIXME
		beta = 0.5 * (ProjectionMath.HALFPI - 0);//FIXME
		if (Math.abs(beta) < EPS)
			Cb = -0.5;
		else {
			Cb = 1./Math.tan(beta);
			Cb *= Cb * Math.log(Math.cos(beta));
		}
		if (Math.abs(Math.abs(projectionLatitude) - ProjectionMath.HALFPI) < EPS)
			if (projectionLatitude < 0.) {
				p_halfpi = -ProjectionMath.HALFPI;
				mode = S_POLE;
			} else {
				p_halfpi =  ProjectionMath.HALFPI;
				mode = N_POLE;
			}
		else {
			if (Math.abs(projectionLatitude) < EPS)
				mode = EQUIT;
			else {
				mode = OBLIQ;
				sinph0 = Math.sin(projectionLatitude);
				cosph0 = Math.cos(projectionLatitude);
			}
		}
	}

	public String toString() {
		return "Airy";
	}

}
