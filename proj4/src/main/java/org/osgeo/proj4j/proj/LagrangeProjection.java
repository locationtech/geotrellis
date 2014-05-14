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

public class LagrangeProjection extends Projection {

	// Parameters
	private double hrw;
	private double rw = 1.4;
	private double a1;
	private double phi1;

	private final static double TOL = 1e-10;

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
		double v, c;

		if ( Math.abs(Math.abs(lpphi) - ProjectionMath.HALFPI) < TOL) {
			xy.x = 0;
			xy.y = lpphi < 0 ? -2. : 2.;
		} else {
			lpphi = Math.sin(lpphi);
			v = a1 * Math.pow((1. + lpphi)/(1. - lpphi), hrw);
			if ((c = 0.5 * (v + 1./v) + Math.cos(lplam *= rw)) < TOL)
				throw new ProjectionException();
			xy.x = 2. * Math.sin(lplam) / c;
			xy.y = (v - 1./v) / c;
		}
		return xy;
	}

	public void setW( double w ) {
		this.rw = w;
	}
	
	public double getW() {
		return rw;
	}

	public void initialize() {
		super.initialize();
		if (rw <= 0)
			throw new ProjectionException("-27");
		hrw = 0.5 * (rw = 1. / rw);
		phi1 = projectionLatitude1;
		if (Math.abs(Math.abs(phi1 = Math.sin(phi1)) - 1.) < TOL)
			throw new ProjectionException("-22");
		a1 = Math.pow((1. - phi1)/(1. + phi1), hrw);
	}

	/**
	 * Returns true if this projection is conformal
	 */
	public boolean isConformal() {
		return true;
	}
	
	public boolean hasInverse() {
		return false;
	}

	public String toString() {
		return "Lagrange";
	}

}
