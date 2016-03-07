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
}
