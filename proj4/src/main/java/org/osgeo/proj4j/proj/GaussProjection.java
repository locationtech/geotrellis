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

public class GaussProjection extends Projection {

	private final static int MAX_ITER = 20;
	private final static double DEL_TOL = 1.0e-14;

  private double C;
  private double K;
//  private double e;
  protected double rc;
  protected double phic0;
  private double ratexp;

	public GaussProjection() {
	}

	public ProjCoordinate project(double x, double y, ProjCoordinate out) {
    out.y = 2.0 * Math.atan( K * Math.pow(Math.tan(0.5 * y + ProjectionMath.QUARTERPI), this.C) * srat(this.e * Math.sin(y), this.ratexp) ) - ProjectionMath.HALFPI;
    out.x = C * x;
    return out;
	}

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate out) {
    double lon = x / this.C;
    double lat = y;
    double num = Math.pow(Math.tan(0.5 * lat + ProjectionMath.QUARTERPI)/this.K, 1./this.C);
    int i;
    for (i = MAX_ITER; i > 0; --i) {
      lat = 2.0 * Math.atan(num * srat(this.e * Math.sin(y), -0.5 * this.e)) - ProjectionMath.HALFPI;
      if (Math.abs(lat - y) < DEL_TOL) break;
      y = lat;
    } 
    /* convergence failed */
    if (i <= 0) {
      throw new ProjectionException(this, ProjectionException.ERR_17);
    }
    out.x = lon;
    out.y = lat;
		return out;
	}

	public void initialize() {
		super.initialize();
    double sphi = Math.sin(projectionLatitude);
    double cphi = Math.cos(projectionLatitude);  
    cphi *= cphi;
    rc = Math.sqrt(1.0 - es) / (1.0 - es * sphi * sphi);
    C = Math.sqrt(1.0 + es * cphi * cphi / (1.0 - es));
    phic0 = Math.asin(sphi / C);
    ratexp = 0.5 * C * e;
    K = Math.tan(0.5 * phic0 + ProjectionMath.QUARTERPI) / (Math.pow(Math.tan(0.5*projectionLatitude + ProjectionMath.QUARTERPI), this.C) * srat(e*sphi, ratexp));
	}

  private static double srat(double esinp, double exp) {
    return Math.pow((1.0 - esinp) / (1.0 + esinp), exp);
}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Gauss";
	}

}

