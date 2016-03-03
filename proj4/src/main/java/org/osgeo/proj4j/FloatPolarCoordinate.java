package org.osgeo.proj4j;

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
}
