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

public class ObliqueStereographicAlternativeProjection extends GaussProjection {

  private double sinc0;
  private double cosc0;
  private double R2;
  
	public ObliqueStereographicAlternativeProjection() {
	}
	
  public ProjCoordinate OLDproject(double x, double y, ProjCoordinate out) {
    super.project(x, y, out);
    double px = out.x;
    double py = out.y;
    double sinc = Math.sin(py);
    double cosc = Math.cos(py);
    double cosl = Math.cos(px);
    double k = scaleFactor * R2 / (1.0 + sinc0 * sinc + cosc0 * cosc * cosl);
    out.x = k * cosc * Math.sin(px);
    out.y = k * (this.cosc0 * sinc - this.sinc0 * cosc * cosl);
    return out;
  }

  public ProjCoordinate project(double lplamIn, double lpphiIn, ProjCoordinate out) {
    super.project(lplamIn, lpphiIn, out);
    double lplam = out.x;
    double lpphi = out.y;
    double sinc = Math.sin(lpphi);
    double cosc = Math.cos(lpphi);
    double cosl = Math.cos(lplam);
    double k = scaleFactor * R2 / (1. + sinc0 * sinc + cosc0 * cosc * cosl);
    out.x = k * cosc * Math.sin(lplam);
    out.y = k * (cosc0 * sinc - sinc0 * cosc * cosl);
    return out;
  }

	public ProjCoordinate projectInverse(double x, double y, ProjCoordinate out) {
	  double xyx = x / scaleFactor;
	  double xyy = y / scaleFactor;
	  double rho = Math.sqrt(xyx * xyx + xyy * xyy);
	  double lpphi;
	  double lplam;
	  if (rho != 0) {
	    double c = 2. * Math.atan2(rho, R2);
	    double sinc = Math.sin(c);
	    double cosc = Math.cos(c);
	    lpphi = Math.asin(cosc * sinc0 + xyy * sinc * cosc0 / rho);
	    lplam = Math.atan2(xyx * sinc, rho * cosc0 * cosc -
	      xyy * sinc0 * sinc);
	  } else {
	    lpphi = phic0;
	    lplam = 0.;
	  }
	  return super.projectInverse(lplam, lpphi, out);
	}

	public boolean hasInverse() {
		return true;
	}

	public void initialize() {
		super.initialize();
    sinc0 = Math.sin(phic0);
    cosc0 = Math.cos(phic0);
    R2 = 2.0 * rc;
	}

	public String toString() {
		return "Oblique Stereographic Alternative";
	}

}
