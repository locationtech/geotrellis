/*
Copyright 2006 Martin Davis

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
 * This file was converted from the PROJ.4 source.
 */
package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.ProjectionException;
import org.osgeo.proj4j.datum.Ellipsoid;
import org.osgeo.proj4j.util.ProjectionMath;

/**
* Swiss Oblique Mercator Projection algorithm is taken from the USGS PROJ.4 package.
*/
public class SwissObliqueMercatorProjection extends Projection {

  private static final int NITER = 6;

	private double  K, c, hlf_e, kR, cosp0, sinp0;
  private double phi0;
	
	public SwissObliqueMercatorProjection() {
		//initialize();
	}
	
	public void initialize() {
		super.initialize();
	  double cp, phip0, sp;

    phi0 = projectionLatitude;
    
	  hlf_e = 0.5 * e;
	  cp = Math.cos(phi0);
	  cp *= cp;
	  c = Math.sqrt(1 + es * cp * cp * rone_es);
	  sp = Math.sin(phi0);
	  cosp0 = Math.cos( phip0 = Math.asin(sinp0 = sp / c) );
	  sp *= e;
	  K = Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * phip0)) - c * (
	      Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * phi0)) - hlf_e *
	      Math.log((1. + sp) / (1. - sp)));
	  kR = scaleFactor * Math.sqrt(one_es) / (1. - sp * sp);
	}

	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate xy) {
	  double phip, lamp, phipp, lampp, sp, cp;

	  sp = e * Math.sin(lpphi);
	  phip = 2.* Math.atan( Math.exp( c * (
	      Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * lpphi)) - hlf_e * Math.log((1. + sp)/(1. - sp)))
	    + K)) - ProjectionMath.HALFPI;
	  lamp = c * lplam;
	  cp = Math.cos(phip);
	  phipp = Math.asin(cosp0 * Math.sin(phip) - sinp0 * cp * Math.cos(lamp));
	  lampp = Math.asin(cp * Math.sin(lamp) / Math.cos(phipp));
	  xy.x = kR * lampp;
	  xy.y = kR * Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * phipp));
	  return xy;
	}

	public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate lp) {
	  double phip, lamp, phipp, lampp, cp, esp, con, delp;
	  int i;
	  double lplam, lpphi;

	  phipp = 2. * (Math.atan(Math.exp(xyy / kR)) - ProjectionMath.FORTPI);
	  lampp = xyx / kR;
	  cp = Math.cos(phipp);
	  phip = Math.asin(cosp0 * Math.sin(phipp) + sinp0 * cp * Math.cos(lampp));
	  lamp = Math.asin(cp * Math.sin(lampp) / Math.cos(phip));
	  con = (K - Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * phip)))/c;
	  for (i = NITER; i != 0 ; --i) {
	    esp = e * Math.sin(phip);
	    delp = (con + Math.log(Math.tan(ProjectionMath.FORTPI + 0.5 * phip)) - hlf_e *
	        Math.log((1. + esp)/(1. - esp)) ) *
	      (1. - esp * esp) * Math.cos(phip) * rone_es;
	    phip -= delp;
	    if (Math.abs(delp) < ProjectionMath.EPS10)
	      break;
	  }
	  if (i != 0) {
	    lpphi = phip;
	    lplam = lamp / c;
	  } else {
	    throw new ProjectionException("I_ERROR");
	  }
    lp.x = lplam;
    lp.y = lpphi;
	  
		return lp;
	}

	public boolean hasInverse() {
		return true;
	}

	public String toString() {
		return "Swiss Oblique Mercator";
	}

}
