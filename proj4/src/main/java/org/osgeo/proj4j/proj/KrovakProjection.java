/*
 * Copyright 2016 Martin Davis, Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.ProjectionException;
import org.osgeo.proj4j.util.ProjectionMath;
import static java.lang.Math.*;

/**
 * The Krovak projection.
 *
 * While Krovak defines parameters for the azimuth and the latitude of the
 * pseudo-standard parallel, these are hardcoded in this implementation.
 *
 * @see http://www.ihsenergy.com/epsg/guid7.html#1.4.3
 */
public class KrovakProjection extends Projection {

	public KrovakProjection() {
		minLatitude = Math.toRadians(-60);
		maxLatitude = Math.toRadians(60);
		minLongitude = Math.toRadians(-90);
		maxLongitude = Math.toRadians(90);
		initialize();
	}
	
    @Override
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
        /* Constants, identical to inverse transform function */
        double s45, s90, e2, e, alfa, uq, u0, g, k, k1, n0, ro0, ad, a, s0, n;
        double gfi, u, fi0, deltav, s, d, eps, ro;


        s45 = 0.785398163397448;    /* 45deg */
        s90 = 2 * s45;
        fi0 = projectionLatitude;    /* Latitude of projection centre 49deg 30' */

        /* Ellipsoid is used as Parameter in for.c and inv.c, therefore a must 
           be set to 1 here.
           Ellipsoid Bessel 1841 a = 6377397.155m 1/f = 299.1528128,
           e2=0.006674372230614;
           */
        a =  1; /* 6377397.155; */
        /* e2 = P->es;*/       /* 0.006674372230614; */
        e2 = 0.006674372230614;
        e = sqrt(e2);

        alfa = sqrt(1. + (e2 * pow(cos(fi0), 4)) / (1. - e2));

        uq = 1.04216856380474;      /* DU(2, 59, 42, 42.69689) */
        u0 = asin(sin(fi0) / alfa);
        g = pow(   (1. + e * sin(fi0)) / (1. - e * sin(fi0)) , alfa * e / 2.  );

        k = tan( u0 / 2. + s45) / pow  (tan(fi0 / 2. + s45) , alfa) * g;

        k1 = scaleFactor;
        n0 = a * sqrt(1. - e2) / (1. - e2 * pow(sin(fi0), 2));
        s0 = 1.37008346281555;       /* Latitude of pseudo standard parallel 78deg 30'00" N */
        n = sin(s0);
        ro0 = k1 * n0 / tan(s0);
        ad = s90 - uq;

        /* Transformation */

        gfi =pow ( ((1. + e * sin(lpphi)) /
                    (1. - e * sin(lpphi))) , (alfa * e / 2.));

        u= 2. * (atan(k * pow( tan(lpphi / 2. + s45), alfa) / gfi)-s45);

        deltav = - lplam * alfa;

        s = asin(cos(ad) * sin(u) + sin(ad) * cos(u) * cos(deltav));
        d = asin(cos(u) * sin(deltav) / cos(s));
        eps = n * d;
        ro = ro0 * pow(tan(s0 / 2. + s45) , n) / pow(tan(s / 2. + s45) , n)   ;

        /* x and y are reverted! */
        out.y = ro * cos(eps) / a;
        out.x = ro * sin(eps) / a;

        // TODO: Is the 'czech' parameter used?
        // if( !pj_param(P->ctx, P->params, "tczech").i )
        // {
        //     out.y *= -1.0;
        //     out.x *= -1.0;
        // }

        return out;
    }

    @Override
    protected ProjCoordinate projectInverse(double x, double y, ProjCoordinate dst) {
        /* calculate lat/lon from xy */

        /* Constants, identisch wie in der Umkehrfunktion */
        double s45, s90, fi0, e2, e, alfa, uq, u0, g, k, k1, n0, ro0, ad, a, s0, n;
        double u, deltav, s, d, eps, ro, fi1, xy0;
        int ok;

        s45 = 0.785398163397448;    /* 45deg */
        s90 = 2 * s45;
        fi0 = projectionLatitude;    /* Latitude of projection centre 49deg 30' */


        /* Ellipsoid is used as Parameter in for.c and inv.c, therefore a must 
           be set to 1 here.
           Ellipsoid Bessel 1841 a = 6377397.155m 1/f = 299.1528128,
           e2=0.006674372230614;
           */
        a = 1; /* 6377397.155; */
        /* e2 = P->es; */      /* 0.006674372230614; */
        e2 = 0.006674372230614;
        e = sqrt(e2);

        alfa = sqrt(1. + (e2 * pow(cos(fi0), 4)) / (1. - e2));
        uq = 1.04216856380474;      /* DU(2, 59, 42, 42.69689) */
        u0 = asin(sin(fi0) / alfa);
        g = pow(   (1. + e * sin(fi0)) / (1. - e * sin(fi0)) , alfa * e / 2.  );

        k = tan( u0 / 2. + s45) / pow  (tan(fi0 / 2. + s45) , alfa) * g;

        k1 = scaleFactor;
        n0 = a * sqrt(1. - e2) / (1. - e2 * pow(sin(fi0), 2));
        s0 = 1.37008346281555;       /* Latitude of pseudo standard parallel 78deg 30'00" N */
        n = sin(s0);
        ro0 = k1 * n0 / tan(s0);
        ad = s90 - uq;


        /* Transformation */
        /* revert y, x*/
        xy0=dst.x;
        dst.x=dst.y;
        dst.y=xy0;

        // if( !pj_param(P->ctx, P->params, "tczech").i )
        // {
        //     xy.x *= -1.0;
        //     xy.y *= -1.0;
        // }

        ro = sqrt(dst.x * dst.x + dst.y * dst.y);
        eps = atan2(dst.y, dst.x);
        d = eps / sin(s0);
        s = 2. * (atan(  pow(ro0 / ro, 1. / n) * tan(s0 / 2. + s45)) - s45);

        u = asin(cos(ad) * sin(s) - sin(ad) * cos(s) * cos(d));
        deltav = asin(cos(s) * sin(d) / cos(u));

        dst.x = projectionLongitude - deltav / alfa;

        /* ITERATION FOR lp.phi */
        fi1 = u;

        ok = 0;
        do
        {
            dst.y = 2. * ( atan( pow( k, -1. / alfa)  *
                        pow( tan(u / 2. + s45) , 1. / alfa)  *
                        pow( (1. + e * sin(fi1)) / (1. - e * sin(fi1)) , e / 2.)
                        )  - s45);

            if (abs(fi1 - dst.y) < 0.000000000000001) ok=1;
            fi1 = dst.y;

        }
        while (ok==0);

        dst.x -= projectionLongitude;

        return dst;
    }

	public String toString() {
		return "Krovak";
	}
}
