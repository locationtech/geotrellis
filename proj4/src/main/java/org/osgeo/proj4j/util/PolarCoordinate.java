package org.osgeo.proj4j.util;

import java.io.Serializable;

public final class PolarCoordinate implements Serializable {
    public double lam, phi;
    public PolarCoordinate(PolarCoordinate that) {
        this(that.lam, that.phi);
    }

    public PolarCoordinate(double lam, double phi) {
        this.lam = lam;
        this.phi = phi;
    }

    @Override
    public String toString() {
        return String.format("<λ%f, φ%f>", lam, phi);
    }

    @Override
    public int hashCode() {
        return new Double(lam).hashCode() | (17 * new Double(phi).hashCode());
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof PolarCoordinate) {
            PolarCoordinate c = (PolarCoordinate) that;
            return lam == c.lam && phi == c.phi;
        } else {
            return false;
        }
    }
}
