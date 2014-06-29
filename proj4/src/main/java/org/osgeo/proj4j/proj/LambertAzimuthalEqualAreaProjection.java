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

public class LambertAzimuthalEqualAreaProjection extends Projection {

  private static final int N_POLE = 0;
  private static final int S_POLE = 1;
  private static final int EQUIT = 2;
  private static final int OBLIQ = 3;

  private int mode = 0;
  private double phi0;
  private double  sinb1; 
  private double  cosb1; 
  private double  xmf; 
  private double  ymf; 
  private double  mmf; 
  private double  qp; 
  private double  dd; 
  private double  rq; 
  private double[] apa;
  private double  sinph0;
  private double  cosph0;
  
	public LambertAzimuthalEqualAreaProjection() {
		this( false );
	}

	public LambertAzimuthalEqualAreaProjection( boolean south ) {
		//minLatitude = Math.toRadians(0);
		//maxLatitude = Math.toRadians(90);
		//projectionLatitude1 = south ? -ProjectionMath.QUARTERPI : ProjectionMath.QUARTERPI;
		//projectionLatitude2 = south ? -ProjectionMath.HALFPI : ProjectionMath.HALFPI;
		//initialize();
	}

	 public void initialize() {
	    super.initialize();
	    double t;

	    phi0 = projectionLatitude;

	    if (Math.abs((t = Math.abs(phi0)) - ProjectionMath.HALFPI) < ProjectionMath.EPS10) {
	      mode = phi0 < 0. ? S_POLE : N_POLE;
	    }
	    else if (Math.abs(t) < ProjectionMath.EPS10) {
	      mode = EQUIT;
	    }
	    else {
	      mode = OBLIQ;
	    }
	    if (!spherical) {
	      double sinphi;

	      e = Math.sqrt(es);
	      qp = ProjectionMath.qsfn(1., e, one_es);
	      mmf = .5 / (1. - es);
	      apa = ProjectionMath.authset(es);
	      switch (mode) {
	      case N_POLE:
	      case S_POLE:
	        dd = 1.;
	        break;
	      case EQUIT:
	        dd = 1. / (rq = Math.sqrt(.5 * qp));
	        xmf = 1.;
	        ymf = .5 * qp;
	        break;
	      case OBLIQ:
	        rq = Math.sqrt(.5 * qp);
	        sinphi = Math.sin(phi0);
	        sinb1 = ProjectionMath.qsfn(sinphi, e, one_es) / qp;
	        cosb1 = Math.sqrt(1. - sinb1 * sinb1);
	        dd = Math.cos(phi0) / (Math.sqrt(1. - es * sinphi * sinphi) *
	           rq * cosb1);
	        ymf = (xmf = rq) / dd;
	        xmf *= dd;
	        break;
	      }
	    } else {
	      if (mode == OBLIQ) {
	        sinph0 = Math.sin(phi0);
	        cosph0 = Math.cos(phi0);
	      }
	    }
	 	}

	  public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
      if (spherical) {
        project_s(lplam, lpphi, out);
      }
      else {
        project_e(lplam, lpphi, out);
      }
      return out;
	  }
	  
    public void project_s(double lplam, double lpphi, ProjCoordinate out) {
      double  coslam, cosphi, sinphi;

      sinphi = Math.sin(lpphi);
      cosphi = Math.cos(lpphi);
      coslam = Math.cos(lplam);
      switch (mode) {
      case EQUIT:
      case OBLIQ:
        if (mode == EQUIT)
          out.y = 1. + cosphi * coslam;
        else 
          out.y = 1. + sinph0 * sinphi + cosph0 * cosphi * coslam;

        if (out.y <= EPS10) throw new ProjectionException("F");
        out.x = (out.y = Math.sqrt(2. / out.y)) * cosphi * Math.sin(lplam);
        out.y *= mode == EQUIT ? sinphi :
           cosph0 * sinphi - sinph0 * cosphi * coslam;
        break;
      case N_POLE:
        coslam = -coslam;
      case S_POLE:
        if (Math.abs(lpphi + phi0) < EPS10) throw new ProjectionException("F");;
        out.y = ProjectionMath.QUARTERPI - lpphi * .5;
        out.y = 2. * (mode == S_POLE ? Math.cos(out.y) : Math.sin(out.y));
        out.x = out.y * Math.sin(lplam);
        out.y *= coslam;
        break;
      }
    }
    
    public void project_e(double lplam, double lpphi, ProjCoordinate out) {
      double coslam, sinlam, sinphi, q, sinb=0.0, cosb=0.0, b=0.0;

      coslam = Math.cos(lplam);
      sinlam = Math.sin(lplam);
      sinphi = Math.sin(lpphi);
      q = ProjectionMath.qsfn(sinphi, e, one_es);
      if (mode == OBLIQ || mode == EQUIT) {
        sinb = q / qp;
        cosb = Math.sqrt(1. - sinb * sinb);
      }
      switch (mode) {
      case OBLIQ:
        b = 1. + sinb1 * sinb + cosb1 * cosb * coslam;
        break;
      case EQUIT:
        b = 1. + cosb * coslam;
        break;
      case N_POLE:
        b = ProjectionMath.HALFPI + lpphi;
        q = qp - q;
        break;
      case S_POLE:
        b = lpphi - ProjectionMath.HALFPI;
        q = qp + q;
        break;
      }
      if (Math.abs(b) < EPS10) throw new ProjectionException("F");
      
      switch (mode) {
      case OBLIQ:
      case EQUIT:
        if (mode == OBLIQ) {
          out.y = ymf * ( b = Math.sqrt(2. / b) )
             * (cosb1 * sinb - sinb1 * cosb * coslam);
        }
        else {
          out.y = (b = Math.sqrt(2. / (1. + cosb * coslam))) * sinb * ymf; 
        }
        out.x = xmf * b * cosb * sinlam;
        break;
      case N_POLE:
      case S_POLE:
        if (q >= 0.) {
          out.x = (b = Math.sqrt(q)) * sinlam;
          out.y = coslam * (mode == S_POLE ? b : -b);
        } else
          out.x = out.y = 0.;
        break;
      }
    }
  
    public ProjCoordinate projectInverse(double xyx, double xyy, ProjCoordinate out) {
      if (spherical) {
        projectInverse_s(xyx, xyy, out);
      }
      else {
        projectInverse_e(xyx, xyy, out);
      }
      return out;
    }
    
    public void projectInverse_s(double xyx, double xyy, ProjCoordinate out) {
      double  cosz=0.0, rh, sinz=0.0;
      double lpphi, lplam;
      
      rh = Math.hypot(xyx, xyy);
      if ((lpphi = rh * .5 ) > 1.) throw new ProjectionException("I_ERROR");
      lpphi = 2. * Math.asin(lpphi);
      if (mode == OBLIQ || mode == EQUIT) {
        sinz = Math.sin(lpphi);
        cosz = Math.cos(lpphi);
      }
      switch (mode) {
      case EQUIT:
        lpphi = Math.abs(rh) <= EPS10 ? 0. : Math.asin(xyy * sinz / rh);
        xyx *= sinz;
        xyy = cosz * rh;
        break;
      case OBLIQ:
        lpphi = Math.abs(rh) <= EPS10 ? phi0 :
          Math.asin(cosz * sinph0 + xyy * sinz * cosph0 / rh);
        xyx *= sinz * cosph0;
        xyy = (cosz - Math.sin(lpphi) * sinph0) * rh;
        break;
      case N_POLE:
        xyy = -xyy;
        lpphi = ProjectionMath.HALFPI - lpphi;
        break;
      case S_POLE:
        lpphi -= ProjectionMath.HALFPI;
        break;
      }
      lplam = (xyy == 0. && (mode == EQUIT || mode == OBLIQ)) ?
        0. : Math.atan2(xyx, xyy);
      out.x = lplam;
      out.y = lpphi;
    }
    
    public void projectInverse_e(double xyx, double xyy, ProjCoordinate out) {
      double lpphi, lplam;
      double cCe, sCe, q, rho, ab=0.0;

      switch (mode) {
      case EQUIT:
      case OBLIQ:
        if ((rho = Math.hypot(xyx /= dd, xyy *=  dd)) < EPS10) {
          lplam = 0.;
          lpphi = phi0;
          out.x = lplam;
          out.y = lpphi;
          return;
        }
        cCe = Math.cos(sCe = 2. * Math.asin(.5 * rho / rq));
        xyx *= (sCe = Math.sin(sCe));
        if (mode == OBLIQ) {
          q = qp * (ab = cCe * sinb1 + xyy * sCe * cosb1 / rho);
          xyy = rho * cosb1 * cCe - xyy * sinb1 * sCe;
        } else {
          q = qp * (ab = xyy * sCe / rho);
          xyy = rho * cCe;
        }
        break;
      case N_POLE:
        xyy = -xyy;
      case S_POLE:
        if (0 == (q = (xyx * xyx + xyy * xyy)) ) {
          lplam = 0.;
          lpphi = phi0;
          out.x = lplam;
          out.y = lpphi;
          return;
        }
        /*
        q = P->qp - q;
        */
        ab = 1. - q / qp;
        if (mode == S_POLE)
          ab = - ab;
        break;
      }
      lplam = Math.atan2(xyx, xyy);
      lpphi = ProjectionMath.authlat(Math.asin(ab), apa);
      out.x = lplam;
      out.y = lpphi;
    }
    
	  /**
	   * Returns true if this projection is equal area
	   */
	  public boolean isEqualArea() {
	    return true;
	  }

	  public boolean hasInverse() {
	    return true;
	  }
	  
	  public String toString() {
	    return "Lambert Azimuthal Equal Area";
	  }


}
