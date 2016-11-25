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
