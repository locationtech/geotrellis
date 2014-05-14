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

public class MercatorProjection extends CylindricalProjection {
	
	public MercatorProjection() {
		minLatitude = ProjectionMath.degToRad(-85);
		maxLatitude = ProjectionMath.degToRad(85);
	}
	
	public ProjCoordinate project(double lam, double phi, ProjCoordinate out) {
		if (spherical) {
			out.x = scaleFactor * lam;
			out.y = scaleFactor * Math.log(Math.tan(ProjectionMath.QUARTERPI + 0.5 * phi));
		} else {
			out.x = scaleFactor * lam;
			out.y = -scaleFactor * Math.log(ProjectionMath.tsfn(phi, Math.sin(phi), e));
		}
		return out;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate out) {
		if (spherical) {
			out.y = ProjectionMath.HALFPI - 2. * Math.atan(Math.exp(-y / scaleFactor));
			out.x = x / scaleFactor;
		} else {
			out.y = ProjectionMath.phi2(Math.exp(-y / scaleFactor), e);
			out.x = x / scaleFactor;
		}
		return out;
	}

	public boolean hasInverse() {
		return true;
	}

	public boolean isRectilinear() {
		return true;
	}

	/**
	 * Returns the ESPG code for this projection, or 0 if unknown.
	 */
	public int getEPSGCode() {
		return 9804;
	}

	public String toString() {
		return "Mercator";
	}

}
