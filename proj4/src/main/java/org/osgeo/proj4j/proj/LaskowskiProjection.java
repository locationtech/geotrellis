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


public class LaskowskiProjection extends Projection {

	private final static double a10 =  0.975534;
	private final static double a12 = -0.119161;
	private final static double a32 = -0.0143059;
	private final static double a14 = -0.0547009;
	private final static double b01 =  1.00384;
	private final static double b21 =  0.0802894;
	private final static double b03 =  0.0998909;
	private final static double b41 =  0.000199025;
	private final static double b23 = -0.0285500;
	private final static double b05 = -0.0491032;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double l2, p2;

		l2 = lplam * lplam;
		p2 = lpphi * lpphi;
		out.x = lplam * (a10 + p2 * (a12 + l2 * a32 + p2 * a14));
		out.y = lpphi * (b01 + l2 * (b21 + p2 * b23 + l2 * b41) +
			p2 * (b03 + p2 * b05));
		return out;
	}

	public String toString() {
		return "Laskowski";
	}

}
