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

public class SimpleConicProjection extends ConicProjection {
	private double n;
	private double rho_c;
	private double rho_0;
	private double sig;
	private double c1, c2;
	private int	type;

	public final static int EULER = 0;
	public final static int MURD1 = 1;
	public final static int MURD2 = 2;
	public final static int MURD3 = 3;
	public final static int PCONIC = 4;
	public final static int TISSOT = 5;
	public final static int VITK1 = 6;
	private final static double EPS10 = 1.e-10;
	private final static double EPS = 1e-10;

	public SimpleConicProjection() {
		this( EULER );
	}
	
	public SimpleConicProjection(int type) {
		this.type = type;
		minLatitude = Math.toRadians(0);
		maxLatitude = Math.toRadians(80);
	}
	
	public String toString() {
		return "Simple Conic";
	}

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double rho;

		switch (type) {
		case MURD2:
			rho = rho_c + Math.tan(sig - lpphi);
			break;
		case PCONIC:
			rho = c2 * (c1 - Math.tan(lpphi - sig));
			break;
		default:
			rho = rho_c - lpphi;
			break;
		}
		out.x = rho * Math.sin( lplam *= n );
		out.y = rho_0 - rho * Math.cos(lplam);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double rho;

		rho = ProjectionMath.distance(xyx, out.y = rho_0 - xyy);
		if (n < 0.) {
			rho = - rho;
			out.x = - xyx;
			out.y = - xyy;
		}
		out.x = Math.atan2(xyx, xyy) / n;
		switch (type) {
		case PCONIC:
			out.y = Math.atan(c1 - rho / c2) + sig;
			break;
		case MURD2:
			out.y = sig - Math.atan(rho - rho_c);
			break;
		default:
			out.y = rho_c - rho;
		}
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public void initialize() {
		super.initialize();
		double del, cs, dummy;

		/* get common factors for simple conics */
		double p1, p2, d, s;
		int err = 0;

/*FIXME
		if (!pj_param(params, "tlat_1").i ||
			!pj_param(params, "tlat_2").i) {
			err = -41;
		} else {
			p1 = pj_param(params, "rlat_1").f;
			p2 = pj_param(params, "rlat_2").f;
			*del = 0.5 * (p2 - p1);
			sig = 0.5 * (p2 + p1);
			err = (Math.abs(*del) < EPS || Math.abs(sig) < EPS) ? -42 : 0;
			*del = *del;
		}
*/
		p1 = Math.toRadians(30);//FIXME
		p2 = Math.toRadians(60);//FIXME
		del = 0.5 * (p2 - p1);
		sig = 0.5 * (p2 + p1);
		err = (Math.abs(del) < EPS || Math.abs(sig) < EPS) ? -42 : 0;
		del = del;

		if (err != 0)
			throw new ProjectionException("Error "+err);

		switch (type) {
		case TISSOT:
			n = Math.sin(sig);
			cs = Math.cos(del);
			rho_c = n / cs + cs / n;
			rho_0 = Math.sqrt((rho_c - 2 * Math.sin(projectionLatitude))/n);
			break;
		case MURD1:
			rho_c = Math.sin(del)/(del * Math.tan(sig)) + sig;
			rho_0 = rho_c - projectionLatitude;
			n = Math.sin(sig);
			break;
		case MURD2:
			rho_c = (cs = Math.sqrt(Math.cos(del))) / Math.tan(sig);
			rho_0 = rho_c + Math.tan(sig - projectionLatitude);
			n = Math.sin(sig) * cs;
			break;
		case MURD3:
			rho_c = del / (Math.tan(sig) * Math.tan(del)) + sig;
			rho_0 = rho_c - projectionLatitude;
			n = Math.sin(sig) * Math.sin(del) * Math.tan(del) / (del * del);
			break;
		case EULER:
			n = Math.sin(sig) * Math.sin(del) / del;
			del *= 0.5;
			rho_c = del / (Math.tan(del) * Math.tan(sig)) + sig;	
			rho_0 = rho_c - projectionLatitude;
			break;
		case PCONIC:
			n = Math.sin(sig);
			c2 = Math.cos(del);
			c1 = 1./Math.tan(sig);
			if (Math.abs(del = projectionLatitude - sig) - EPS10 >= ProjectionMath.HALFPI)
				throw new ProjectionException("-43");
			rho_0 = c2 * (c1 - Math.tan(del));
maxLatitude = Math.toRadians(60);//FIXME
			break;
		case VITK1:
			n = (cs = Math.tan(del)) * Math.sin(sig) / del;
			rho_c = del / (cs * Math.tan(sig)) + sig;
			rho_0 = rho_c - projectionLatitude;
			break;
		}
	}
}
