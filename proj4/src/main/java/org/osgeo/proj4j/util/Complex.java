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

public final class Complex {
    public double r;
    public double i;
    public Complex(double r, double i) {
        this.r = r;
        this.i = i;
    }
    public Complex(Complex that) {
        this(that.r, that.i);
    }

    public int hashCode() {
        return Double.valueOf(r).hashCode() | 37 * Double.valueOf(i).hashCode();
    }

    public boolean equals(Object that) {
        if (that instanceof Complex)  {
            return ((Complex) that).r == r && ((Complex)that).i == i;
        } else {
            return false;
        }
    }
}
