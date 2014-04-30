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

public class LarriveeProjection extends Projection {

	private final static double SIXTH = .16666666666666666;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
		out.x = 0.5 * lplam * (1. + Math.sqrt(Math.cos(lpphi)));
		out.y = lpphi / (Math.cos(0.5 * lpphi) * Math.cos(SIXTH * lplam));
		return out;
	}

	public String toString() {
		return "Larrivee";
	}

}
