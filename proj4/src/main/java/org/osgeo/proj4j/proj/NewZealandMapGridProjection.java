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
import org.osgeo.proj4j.util.Complex;
import org.osgeo.proj4j.util.ProjectionMath;
import static org.osgeo.proj4j.util.ProjectionMath.EPS10;
import static org.osgeo.proj4j.util.ProjectionMath.HALFPI;
import static org.osgeo.proj4j.util.ProjectionMath.authlat;
import static org.osgeo.proj4j.util.ProjectionMath.authset;
import static org.osgeo.proj4j.util.ProjectionMath.qsfn;
import static org.osgeo.proj4j.util.ProjectionMath.zpoly1;
import static org.osgeo.proj4j.util.ProjectionMath.zpoly1d;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.sin;

/**
 * The New Zealand Map Grid projection.
 */
public class NewZealandMapGridProjection extends Projection {

    private final static Complex bf[] = {
        new Complex(.7557853228, 0.0),
        new Complex(.249204646, .003371507),
        new Complex(-.001541739, .041058560),
        new Complex(-.10162907, .01727609),
        new Complex(-.26623489, -.36249218),
        new Complex(-.6870983, -1.1651967)
    };
                 
    private final static double tphi[] = { 1.5627014243, .5185406398, -.03333098, -.1052906, -.0368594, .007317, .01220, .00394, -.0013 };
                 
    private final static double tpsi[] = { .6399175073, -.1358797613, .063294409, -.02526853, .0117879, -.0055161, .0026906, -.001333, .00067, -.00034 };

    private final static double SECS_TO_RAD = ProjectionMath.DTR / 3600d;
    private final static double RAD_TO_SECS = ProjectionMath.RTD * 3600d;

	public NewZealandMapGridProjection() {
		initialize();
	}
	
    @Override
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
        Complex p = new Complex(0, 0);

        lpphi = (lpphi - projectionLatitude) * RAD_TO_SECS;
        for (int i = tpsi.length - 1; i >= 0; --i) 
            p.r = tpsi[i] + lpphi * p.r;
        p.r *= lpphi;
        p.i = lplam;
        zpoly1(p, bf);
        out.x = p.i;
        out.y = p.r;
        return out;
    }

    @Override
    protected ProjCoordinate projectInverse(double x, double y, ProjCoordinate dst) {
        int nn, i;
        Complex p = new Complex(y, x), f, fp = new Complex(0,0), dp = new Complex(0,0);
        double den;
        double[] C;

        for (nn = 20; nn > 0 ;--nn) {
            f = zpoly1d(p, bf, fp);
            f.r -= y;
            f.i -= x;
            den = fp.r * fp.r + fp.i * fp.i;
            p.r += dp.r = -(f.r * fp.r + f.i * fp.i) / den;
            p.i += dp.i = -(f.i * fp.r - f.r * fp.i) / den;
            if ((abs(dp.r) + abs(dp.i)) <= EPS10)
                break;
        }
        if (nn > 0) {
            dst.x = p.i;
            dst.y = tphi[tphi.length - 1];
            for (i = tphi.length - 1; i > 0; i--) {
                dst.y = tphi[i] + p.r * dst.y;
            }
            dst.y = projectionLongitude + p.r * dst.x * SECS_TO_RAD;
        } else
            dst.y = dst.x = Double.NaN;
        return dst;
    }

    @Override
    public void initialize() {
        super.initialize();
        // Force to International major axis
        a = 6378388.0;
        projectionLongitude = ProjectionMath.DTR * 173d;
        projectionLatitude = ProjectionMath.DTR * -41.;
        falseEasting = 2510000d;
        falseNorthing = 6023150d;
    }
  
  	public String toString() {
  		return "New Zealand Map Grid";
  	}
}
