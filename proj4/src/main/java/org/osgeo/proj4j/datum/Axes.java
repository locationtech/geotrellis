package org.osgeo.proj4j.datum;

import org.osgeo.proj4j.ProjCoordinate;
import java.io.Serializable;

public final class Axes implements Serializable {
    private enum Axis {
        Easting {
            public double fromENU(ProjCoordinate c) {
                return c.x;
            }
            public void toENU(double x, ProjCoordinate c) {
                c.x = x;
            }
        },
        Westing {
            public double fromENU(ProjCoordinate c) {
                return -c.x;
            }
            public void toENU(double x, ProjCoordinate c) {
                c.x = -x;
            }
        },
        Northing {
            public double fromENU(ProjCoordinate c) {
                return c.y;
            }
            public void toENU(double y, ProjCoordinate c) {
                c.y = y;
            }
        },
        Southing {
            public double fromENU(ProjCoordinate c) {
                return -c.y;
            }
            public void toENU(double y, ProjCoordinate c) {
                c.y = -y;
            }
        },
        Up {
            public double fromENU(ProjCoordinate c) {
                return c.z;
            }
            public void toENU(double z, ProjCoordinate c) {
                c.z = z;
            }
        },
        Down {
            public double fromENU(ProjCoordinate c) {
                return c.z;
            }
            public void toENU(double z, ProjCoordinate c) {
                c.z = -z;
            }
        };

        static Axis fromChar(char c) {
            switch(c) {
                case 'e': return Easting;
                case 'n': return Northing;
                case 'u': return Up;
                case 'w': return Westing;
                case 's': return Southing;
                case 'd': return Down;
            }
            throw new IllegalArgumentException();
        }

        public abstract double fromENU(ProjCoordinate c);
        public abstract void toENU(double x, ProjCoordinate c);
    }

    public final static Axes ENU = 
        new Axes(Axis.Easting, Axis.Northing, Axis.Up);

    private final Axis x, y, z;

    private Axes(Axis x, Axis y, Axis z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public static Axes fromString(String spec) {
        if (spec.length() != 3) {
            throw new Error();
        }

        Axis x = Axis.fromChar(spec.charAt(0));
        Axis y = Axis.fromChar(spec.charAt(1));
        Axis z = Axis.fromChar(spec.charAt(2));

        return new Axes(x, y, z);
    }

    public void fromENU(ProjCoordinate coord) {
        double x = this.x.fromENU(coord);
        double y = this.y.fromENU(coord);
        double z = this.z.fromENU(coord);
        coord.x = x;
        coord.y = y;
        coord.z = z;
    }

    public void toENU(ProjCoordinate coord) {
        double x = coord.x;
        double y = coord.y;
        double z = coord.z;
        this.x.toENU(x, coord);
        this.y.toENU(y, coord);
        this.z.toENU(z, coord);
    }
}
