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

public class HatanoProjection extends Projection {

	private final static int NITER = 20;
	private final static double EPS = 1e-7;
	private final static double ONETOL = 1.000001;
	private final static double CN = 2.67595;
	private final static double CS = 2.43763;
	private final static double RCN = 0.37369906014686373063;
	private final static double RCS = 0.41023453108141924738;
	private final static double FYCN = 1.75859;
	private final static double FYCS = 1.93052;
	private final static double RYCN = 0.56863737426006061674;
	private final static double RYCS = 0.51799515156538134803;
	private final static double FXC = 0.85;
	private final static double RXC = 1.17647058823529411764;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double th1, c;
		int i;

		c = Math.sin(lpphi) * (lpphi < 0. ? CS : CN);
		for (i = NITER; i > 0; --i) {
			lpphi -= th1 = (lpphi + Math.sin(lpphi) - c) / (1. + Math.cos(lpphi));
			if (Math.abs(th1) < EPS) break;
		}
		out.x = FXC * lplam * Math.cos(lpphi *= .5);
		out.y = Math.sin(lpphi) * (lpphi < 0. ? FYCS : FYCN);
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		double th;

		th = xyy * ( xyy < 0. ? RYCS : RYCN);
		if (Math.abs(th) > 1.)
			if (Math.abs(th) > ONETOL)	throw new ProjectionException("I");
			else			th = th > 0. ? ProjectionMath.HALFPI : - ProjectionMath.HALFPI;
		else
			th = Math.asin(th);
		out.x = RXC * xyx / Math.cos(th);
		th += th;
		out.y = (th + Math.sin(th)) * (xyy < 0. ? RCS : RCN);
		if (Math.abs(out.y) > 1.)
			if (Math.abs(out.y) > ONETOL)	throw new ProjectionException("I");
			else			out.y = out.y > 0. ? ProjectionMath.HALFPI : - ProjectionMath.HALFPI;
		else
			out.y = Math.asin(out.y);
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Hatano Asymmetrical Equal Area";
	}

}
