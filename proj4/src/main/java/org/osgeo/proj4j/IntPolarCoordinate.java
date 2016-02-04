package org.osgeo.proj4j;

public final class IntPolarCoordinate {
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
}
