package org.osgeo.proj4j.proj;

import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.ProjectionException;
import static org.osgeo.proj4j.util.ProjectionMath.EPS10;
import static org.osgeo.proj4j.util.ProjectionMath.HALFPI;
import static org.osgeo.proj4j.util.ProjectionMath.authlat;
import static org.osgeo.proj4j.util.ProjectionMath.authset;
import static org.osgeo.proj4j.util.ProjectionMath.qsfn;
import static java.lang.Math.abs;
import static java.lang.Math.asin;
import static java.lang.Math.sin;

/**
 * The Equal Area Cylindrical projection.
 */
public class EqualAreaCylindricalProjection extends Projection {

    double apa[];
    double qp;

	public EqualAreaCylindricalProjection() {
		minLatitude = Math.toRadians(-60);
		maxLatitude = Math.toRadians(60);
		minLongitude = Math.toRadians(-90);
		maxLongitude = Math.toRadians(90);
		initialize();
	}
	
    @Override
	public ProjCoordinate project(double lplam, double lpphi, ProjCoordinate out) {
        if (es != 0) {
            // e_forward
            out.x = scaleFactor * lplam;
            out.y = 0.5 * qsfn(sin(lpphi), e, one_es) / scaleFactor;
        } else {
            // s_forward
            out.x = scaleFactor * lplam;
            out.y = sin(lpphi) / scaleFactor;
        }
        return out;
    }

    @Override
    protected ProjCoordinate projectInverse(double x, double y, ProjCoordinate dst) {
        if (es != 0) {
            // e_inverse
            dst.x = authlat(asin(2d * y * scaleFactor / qp), apa);
            dst.y = x / scaleFactor;
        } else {
            // s_inverse
            double t;

            if ((t = abs(y *= scaleFactor)) - EPS10 <= 1.) {
                if (t >= 1.)
                    dst.x = y < 0. ? -HALFPI : HALFPI;
                else
                    dst.x = asin(y);
                dst.y = x / scaleFactor;
            } else throw new ProjectionException();
        }
        return dst;
    }

    @Override
    public void initialize() {
        super.initialize();
        if (es != 0) {
            apa = authset(es);
            qp = qsfn(1., e, one_es);
        }
    }

	public String toString() {
		return "Equal Area Cylindrical";
	}
}
