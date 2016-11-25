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

package org.osgeo.proj4j.datum;

import java.io.Serializable;
import org.osgeo.proj4j.ProjCoordinate;
import org.osgeo.proj4j.util.ProjectionMath;
import org.osgeo.proj4j.units.AngleFormat;

/**
 * A PrimeMeridian represents a constant offset from Greenwich in radians of
 * longitude.
 */
// The list of named prime meridians is defined in proj.4 in pj_datums.c
public final class PrimeMeridian implements Serializable {
    private final String name;
    private final double offsetFromGreenwich;

    private static PrimeMeridian GREENWICH = new PrimeMeridian("greenwich", 0);
    private static PrimeMeridian[] wellKnownMeridians = {
        east("greenwich", 0, 0, 0),
        west("lisbon",    9, 7, 54.862),
        east("paris",     2,20,14.025),
        west("bogota",    74,04,51.3),
        west("madrid",    3,41,16.58),
        east("rome",      12,27,8.4),
        east("bern",      7,26,22.5),
        east("jakarta",   106,48,27.79),
        west("ferro",     17,40,0),
        east("brussels",  4,22,4.71),
        east("stockholm", 18,3,29.8),
        east("athens",    23,42,58.815),
        east("oslo",      10,43,22.5)
    };

    private static PrimeMeridian east(String name, double deg, double min, double sec) {
        double longitude = ((sec / 60. + min) / 60. + deg) * ProjectionMath.DTR;
        return new PrimeMeridian(name, longitude);
    }

    private static PrimeMeridian west(String name, double deg, double min, double sec) {
        return east(name, -deg, -min, -sec);
    }

    public static PrimeMeridian forName(String name) {
        for (PrimeMeridian pm : wellKnownMeridians) {
            if (pm.getName().equals(name)) return pm;
        }

        try {
            return new PrimeMeridian("user-provided", Double.valueOf(name) * ProjectionMath.DTR);
        } catch (NumberFormatException e) {
            // passthrough
        }

        return GREENWICH;
    }

    private PrimeMeridian(String name, double offsetFromGreenwich) {
        this.name = name;
        this.offsetFromGreenwich = offsetFromGreenwich;
    }

    public String getName() {
        return name;
    }

    public void toGreenwich(ProjCoordinate coord) {
        coord.x += this.offsetFromGreenwich;
    }

    public void fromGreenwich(ProjCoordinate coord) {
        coord.x -= this.offsetFromGreenwich;
    }

    @Override
    public int hashCode() {
        return new Double(offsetFromGreenwich).hashCode();
    }

    @Override
    public boolean equals(Object that) {
        if (that instanceof PrimeMeridian) {
            return offsetFromGreenwich == ((PrimeMeridian)that).offsetFromGreenwich;

        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "PM[" + name + ": " + offsetFromGreenwich + "]";
    }
}
