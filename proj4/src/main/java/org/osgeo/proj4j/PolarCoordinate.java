package org.osgeo.proj4j;

public final class PolarCoordinate {
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
