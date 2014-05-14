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

public class Wagner7Projection extends Projection {

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		double theta, ct, D;

		theta = Math.asin(out.y = 0.90630778703664996 * Math.sin(lpphi));
		out.x = 2.66723 * (ct = Math.cos(theta)) * Math.sin(lplam /= 3.);
		out.y *= 1.24104 * (D = 1/(Math.sqrt(0.5 * (1 + ct * Math.cos(lplam)))));
		out.x *= D;
		return out;
	}

	/**
	 * Returns true if this projection is equal area
	 */
	public boolean isEqualArea() {
		return true;
	}

	public String toString() {
		return "Wagner VII";
	}

}
