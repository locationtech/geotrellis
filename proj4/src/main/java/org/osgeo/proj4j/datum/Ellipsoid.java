/*
Copyright 2006 Jerry Huxtable

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

package org.osgeo.proj4j.datum;

/**
 * A class representing a geographic reference ellipsoid 
 * (or more correctly an oblate spheroid), 
 * used to model the shape of the surface of the earth.
 * <p>
 * An oblate spheroid is a geometric surface formed
 * by the rotation of an ellipse about its minor axis.
 * In geodesy this is used as a convenient approximation to the
 * geoid, the true shape of the earth's surface.
 * <p>
 * An ellipsoid is defined by the following parameters:
 * <ul>
 * <li><i>a</i>, the equatorial radius or semi-major axis
 * (see {@link #getA()})
 * </ul>
 * and one of:
 * <ul>
 * <li><i>b</i>, the polar radius or semi-minor axis 
 * (see {@link #getB()})
 * <li><i>f</i>, the reciprocal flattening
 * (<i>f = (a - b) / a</i>)
 * </ul>
 * In order to be used as a model of the geoid,
 * the exact positioning of an ellipsoid
 * relative to the geoid surface needs to be specified.
 * This is provided by means of a geodetic {@link Datum}.
 * <p>
 * Notable ellipsoids in common use include 
 * {@link #CLARKE_1866}, {@link #GRS80}, and {@link #WGS84}.
 *   
 * @see Datum
 */
public class Ellipsoid implements Cloneable
{

  public String name;

  public String shortName;

  public double equatorRadius = 1.0;

  public double poleRadius = 1.0;

  public double eccentricity = 1.0;

  public double eccentricity2 = 1.0;

  // From: USGS PROJ package.
  public final static Ellipsoid INTERNATIONAL = new Ellipsoid("intl",
      6378388.0, 0.0, 297.0, "International 1909 (Hayford)");

  public final static Ellipsoid BESSEL = new Ellipsoid("bessel", 6377397.155,
      0.0, 299.1528128, "Bessel 1841");

  public final static Ellipsoid CLARKE_1866 = new Ellipsoid("clrk66",
      6378206.4, 6356583.8, 0.0, "Clarke 1866");

  public final static Ellipsoid CLARKE_1880 = new Ellipsoid("clrk80",
      6378249.145, 0.0, 293.4663, "Clarke 1880 mod.");

  public final static Ellipsoid AIRY = new Ellipsoid("airy", 6377563.396,
      6356256.910, 0.0, "Airy 1830");

  public final static Ellipsoid WGS60 = new Ellipsoid("WGS60", 6378165.0, 0.0,
      298.3, "WGS 60");

  public final static Ellipsoid WGS66 = new Ellipsoid("WGS66", 6378145.0, 0.0,
      298.25, "WGS 66");

  public final static Ellipsoid WGS72 = new Ellipsoid("WGS72", 6378135.0, 0.0,
      298.26, "WGS 72");

  public final static Ellipsoid WGS84 = new Ellipsoid("WGS84", 6378137.0, 0.0,
      298.257223563, "WGS 84");

  public final static Ellipsoid KRASSOVSKY = new Ellipsoid("krass", 6378245.0,
      0.0, 298.3, "Krassovsky, 1942");

  public final static Ellipsoid EVEREST = new Ellipsoid("evrst30", 6377276.345,
      0.0, 300.8017, "Everest 1830");

  public final static Ellipsoid INTERNATIONAL_1967 = new Ellipsoid("new_intl",
      6378157.5, 6356772.2, 0.0, "New International 1967");

  public final static Ellipsoid GRS80 = new Ellipsoid("GRS80", 6378137.0, 0.0,
      298.257222101, "GRS 1980 (IUGG, 1980)");

  public final static Ellipsoid AUSTRALIAN = new Ellipsoid("australian",
      6378160.0, 6356774.7, 298.25, "Australian");

  public final static Ellipsoid MERIT = new Ellipsoid("MERIT", 6378137.0, 0.0,
      298.257, "MERIT 1983");

  public final static Ellipsoid SGS85 = new Ellipsoid("SGS85", 6378136.0, 0.0,
      298.257, "Soviet Geodetic System 85");

  public final static Ellipsoid IAU76 = new Ellipsoid("IAU76", 6378140.0, 0.0,
      298.257, "IAU 1976");

  public final static Ellipsoid APL4_9 = new Ellipsoid("APL4.9", 6378137.0,
      0.0, 298.25, "Appl. Physics. 1965");

  public final static Ellipsoid NWL9D = new Ellipsoid("NWL9D", 6378145.0, 0.0,
      298.25, "Naval Weapons Lab., 1965");

  public final static Ellipsoid MOD_AIRY = new Ellipsoid("mod_airy",
      6377340.189, 6356034.446, 0.0, "Modified Airy");

  public final static Ellipsoid ANDRAE = new Ellipsoid("andrae", 6377104.43,
      0.0, 300.0, "Andrae 1876 (Den., Iclnd.)");

  public final static Ellipsoid AUST_SA = new Ellipsoid("aust_SA", 6378160.0,
      0.0, 298.25, "Australian Natl & S. Amer. 1969");

  public final static Ellipsoid GRS67 = new Ellipsoid("GRS67", 6378160.0, 0.0,
      298.2471674270, "GRS 67 (IUGG 1967)");

  public final static Ellipsoid BESS_NAM = new Ellipsoid("bess_nam",
      6377483.865, 0.0, 299.1528128, "Bessel 1841 (Namibia)");

  public final static Ellipsoid CPM = new Ellipsoid("CPM", 6375738.7, 0.0,
      334.29, "Comm. des Poids et Mesures 1799");

  public final static Ellipsoid DELMBR = new Ellipsoid("delmbr", 6376428.0,
      0.0, 311.5, "Delambre 1810 (Belgium)");

  public final static Ellipsoid ENGELIS = new Ellipsoid("engelis", 6378136.05,
      0.0, 298.2566, "Engelis 1985");

  public final static Ellipsoid EVRST48 = new Ellipsoid("evrst48", 6377304.063,
      0.0, 300.8017, "Everest 1948");

  public final static Ellipsoid EVRST56 = new Ellipsoid("evrst56", 6377301.243,
      0.0, 300.8017, "Everest 1956");

  public final static Ellipsoid EVRTS69 = new Ellipsoid("evrst69", 6377295.664,
      0.0, 300.8017, "Everest 1969");

  public final static Ellipsoid EVRTSTSS = new Ellipsoid("evrstSS",
      6377298.556, 0.0, 300.8017, "Everest (Sabah & Sarawak)");

  public final static Ellipsoid FRSCH60 = new Ellipsoid("fschr60", 6378166.0,
      0.0, 298.3, "Fischer (Mercury Datum) 1960");

  public final static Ellipsoid FSRCH60M = new Ellipsoid("fschr60m", 6378155.0,
      0.0, 298.3, "Modified Fischer 1960");

  public final static Ellipsoid FSCHR68 = new Ellipsoid("fschr68", 6378150.0,
      0.0, 298.3, "Fischer 1968");

  public final static Ellipsoid HELMERT = new Ellipsoid("helmert", 6378200.0,
      0.0, 298.3, "Helmert 1906");

  public final static Ellipsoid HOUGH = new Ellipsoid("hough", 6378270.0, 0.0,
      297.0, "Hough");

  public final static Ellipsoid INTL = new Ellipsoid("intl", 6378388.0, 0.0,
      297.0, "International 1909 (Hayford)");

  public final static Ellipsoid KAULA = new Ellipsoid("kaula", 6378163.0, 0.0,
      298.24, "Kaula 1961");

  public final static Ellipsoid LERCH = new Ellipsoid("lerch", 6378139.0, 0.0,
      298.257, "Lerch 1979");

  public final static Ellipsoid MPRTS = new Ellipsoid("mprts", 6397300.0, 0.0,
      191.0, "Maupertius 1738");

  public final static Ellipsoid PLESSIS = new Ellipsoid("plessis", 6376523.0,
      6355863.0, 0.0, "Plessis 1817 France)");

  public final static Ellipsoid SEASIA = new Ellipsoid("SEasia", 6378155.0,
      6356773.3205, 0.0, "Southeast Asia");

  public final static Ellipsoid WALBECK = new Ellipsoid("walbeck", 6376896.0,
      6355834.8467, 0.0, "Walbeck");

  public final static Ellipsoid NAD27 = new Ellipsoid("NAD27", 6378249.145,
      0.0, 293.4663, "NAD27: Clarke 1880 mod.");

  public final static Ellipsoid NAD83 = new Ellipsoid("NAD83", 6378137.0, 0.0,
      298.257222101, "NAD83: GRS 1980 (IUGG, 1980)");

  public final static Ellipsoid SPHERE = new Ellipsoid("sphere", 6371008.7714,
      6371008.7714, 0.0, "Sphere");

  public final static Ellipsoid[] ellipsoids = { 
    BESSEL, 
    CLARKE_1866,
    CLARKE_1880, 
    AIRY, 
    WGS60, 
    WGS66, 
    WGS72, 
    WGS84, 
    KRASSOVSKY, 
    EVEREST,
    INTERNATIONAL_1967, 
    GRS80, 
    AUSTRALIAN, 
    MERIT, 
    SGS85, 
    IAU76, 
    APL4_9,
    NWL9D, 
    MOD_AIRY, 
    ANDRAE, 
    AUST_SA, 
    GRS67, 
    BESS_NAM, 
    CPM, 
    DELMBR, 
    ENGELIS,
    EVRST48, 
    EVRST56, 
    EVRTS69, 
    EVRTSTSS, 
    FRSCH60, 
    FSRCH60M, 
    FSCHR68, 
    HELMERT,
    HOUGH, 
    INTL, 
    KAULA, 
    LERCH, 
    MPRTS, 
    PLESSIS, 
    SEASIA, 
    WALBECK, 
    NAD27, 
    NAD83,
    SPHERE };

  public Ellipsoid()
  {
  }

  /**
   * Creates a new Ellipsoid. 
   * One of of poleRadius or reciprocalFlattening must
   * be specified, the other must be zero
   */
  public Ellipsoid(String shortName, double equatorRadius, double poleRadius,
      double reciprocalFlattening, String name)
  {
    this.shortName = shortName;
    this.name = name;
    this.equatorRadius = equatorRadius;
    this.poleRadius = poleRadius;

    if (poleRadius == 0.0 && reciprocalFlattening == 0.0)
      throw new IllegalArgumentException(
          "One of poleRadius or reciprocalFlattening must be specified");
    // don't check for only one of poleRadius or reciprocalFlattening to be
    // specified,
    // since some defs actually supply two

    // reciprocalFlattening takes precedence over poleRadius
    if (reciprocalFlattening != 0) {
      double flattening = 1.0 / reciprocalFlattening;
      double f = flattening;
      eccentricity2 = 2 * f - f * f;
      this.poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricity2);
    }
    else {
      eccentricity2 = 1.0 - (poleRadius * poleRadius)
          / (equatorRadius * equatorRadius);
    }
    eccentricity = Math.sqrt(eccentricity2);
  }

  public Ellipsoid(String shortName, double equatorRadius,
      double eccentricity2, String name)
  {
    this.shortName = shortName;
    this.name = name;
    this.equatorRadius = equatorRadius;
    setEccentricitySquared(eccentricity2);
  }

  public Object clone()
  {
    try {
      Ellipsoid e = (Ellipsoid) super.clone();
      return e;
    }
    catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getName()
  {
    return name;
  }

  public void setShortName(String shortName)
  {
    this.shortName = shortName;
  }

  public String getShortName()
  {
    return shortName;
  }

  public void setEquatorRadius(double equatorRadius)
  {
    this.equatorRadius = equatorRadius;
  }

  public double getEquatorRadius()
  {
    return equatorRadius;
  }

  public double getA()
  {
    return equatorRadius;
  }

  public double getB()
  {
    return poleRadius;
  }

  public void setEccentricitySquared(double eccentricity2)
  {
    this.eccentricity2 = eccentricity2;
    poleRadius = equatorRadius * Math.sqrt(1.0 - eccentricity2);
    eccentricity = Math.sqrt(eccentricity2);
  }

  public double getEccentricitySquared()
  {
    return eccentricity2;
  }

  public boolean isEqual(Ellipsoid e)
  {
    return equatorRadius == e.equatorRadius && eccentricity2 == e.eccentricity2;
  }

  public boolean isEqual(Ellipsoid e, double e2Tolerance)
  {
    if (equatorRadius != e.equatorRadius) return false; 
    if (Math.abs(eccentricity2 
        - e.eccentricity2 ) > e2Tolerance) return false;
    return true;
  }

  public String toString()
  {
    return name;
  }

}
