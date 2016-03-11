package org.osgeo.proj4j.util;

import java.io.Serializable;

public final class FloatPolarCoordinate implements Serializable {
    public float lam, phi;
    public FloatPolarCoordinate(FloatPolarCoordinate that) {
        this(that.lam, that.phi);
    }

    public FloatPolarCoordinate(float lam, float phi) {
        this.lam = lam;
        this.phi = phi;
    }

    @Override
    public int hashCode() {
        return new Float(lam).hashCode() | (17 * new Float(phi).hashCode());
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof FloatPolarCoordinate) {
            FloatPolarCoordinate c = (FloatPolarCoordinate) that;
            return lam == c.lam && phi == c.phi;
        } else {
            return false;
        }
    }
}
