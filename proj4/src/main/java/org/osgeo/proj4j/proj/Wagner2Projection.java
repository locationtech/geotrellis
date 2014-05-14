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
import org.osgeo.proj4j.util.ProjectionMath;

public class Wagner2Projection extends Projection {

	private final static double C_x = 0.92483;
	private final static double C_y = 1.38725;
	private final static double C_p1 = 0.88022;
	private final static double C_p2 = 0.88550;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.y = ProjectionMath.asin(C_p1 * Math.sin(C_p2 * lpphi));
		out.x = C_x * lplam * Math.cos(lpphi);
		out.y = C_y * lpphi;
		return out;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
		out.y = xyy / C_y;
		out.x = xyx / (C_x * Math.cos(out.y));
		out.y = ProjectionMath.asin(Math.sin(out.y) / C_p1) / C_p2;
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Wagner II";
	}
}
