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
