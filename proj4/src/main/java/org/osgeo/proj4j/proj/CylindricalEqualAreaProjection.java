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

public class CylindricalEqualAreaProjection extends Projection {

	private double qp;
	private double[] apa;
	private double trueScaleLatitude;

	public CylindricalEqualAreaProjection() {
		this(0.0, 0.0, 0.0);
	}
	
	public CylindricalEqualAreaProjection(double projectionLatitude, double projectionLongitude, double trueScaleLatitude) {
		this.projectionLatitude = projectionLatitude;
		this.projectionLongitude = projectionLongitude;
		this.trueScaleLatitude = trueScaleLatitude;
		initialize();
	}
	
	public void initialize() {
		super.initialize();
		double t = trueScaleLatitude;

		scaleFactor = Math.cos(t);
		if (es != 0) {
			t = Math.sin(t);
			scaleFactor /= Math.sqrt(1. - es * t * t);
			apa = ProjectionMath.authset(es);
			qp = ProjectionMath.qsfn(1., e, one_es);
		}
	}
	
	public ProjCoordinate project(double lam, double phi, ProjCoordinate xy) {
		if (spherical) {
			xy.x = scaleFactor * lam;
			xy.y = Math.sin(phi) / scaleFactor;
		} else {
			xy.x = scaleFactor * lam;
			xy.y = .5 * ProjectionMath.qsfn(Math.sin(phi), e, one_es) / scaleFactor;
		}
		return xy;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate lp) {
		if (spherical) {
			double t;

			if ((t = Math.abs(y *= scaleFactor)) - EPS10 <= 1.) {
				if (t >= 1.)
					lp.y = y < 0. ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
				else
					lp.y = Math.asin(y);
				lp.x = x / scaleFactor;
			} else throw new ProjectionException();
		} else {
			lp.y = ProjectionMath.authlat(Math.asin( 2. * y * scaleFactor / qp), apa);
			lp.x = x / scaleFactor;
		}
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

	public boolean isRectilinear() {
		return true;
	}

}

