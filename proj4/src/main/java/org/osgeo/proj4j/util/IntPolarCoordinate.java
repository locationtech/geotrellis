package org.osgeo.proj4j.util;

import java.io.Serializable;

public final class IntPolarCoordinate implements Serializable {
    public int lam, phi;
    public IntPolarCoordinate(IntPolarCoordinate that) {
        this(that.lam, that.phi);
    }

    public IntPolarCoordinate(int lam, int phi) {
        this.lam = lam;
        this.phi = phi;
    }

    @Override
    public String toString() {
        return String.format("ILP %x:%x", lam, phi);
    }

    @Override
    public int hashCode() {
        return lam | (17 * phi);
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof IntPolarCoordinate) {
            IntPolarCoordinate c = (IntPolarCoordinate) that;
            return lam == c.lam && phi == c.phi;
        } else {
            return false;
        }
    }
}
