/*
 * Copyright (c) 2014 Azavea.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.proj4.datum

import geotrellis.proj4.ProjCoordinate

object Ellipsoid {
  lazy val NULL = Ellipsoid("NULL", "NULL", Double.NaN, Double.NaN, Double.NaN)

  def apply(
    shortName: String,
    equatorRadius: Double,
    poleRadiusInput: Double,
    reciprocalFlattening: Double,
    name: String
  ): Ellipsoid = {
    if (poleRadiusInput == 0.0 && reciprocalFlattening == 0.0)
      throw new IllegalArgumentException(
        "One of poleRadius or reciprocalFlattening must be specified"
      )

    val (poleRadius, eccentrity2) =
      if (reciprocalFlattening != 0.0) {
        val flattening = 1.0 / reciprocalFlattening
        val eccentrity2 = 2 * flattening - flattening * flattening
        val poleRadius = equatorRadius * math.sqrt(1.0 - eccentrity2)

        (poleRadius, eccentrity2)
      } else {
        val eccentrity2 =
          1.0 - (poleRadiusInput * poleRadiusInput) / (equatorRadius * equatorRadius)
        (poleRadiusInput, eccentrity2)
      }

    val eccentricity = math.sqrt(eccentrity2)

    apply(name, shortName, equatorRadius, poleRadius, eccentricity, eccentrity2)
  }

  def apply(
    shortName: String,
    equatorRadius: Double,
    eccentricity2: Double,
    name: String
  ): Ellipsoid = {
    val poleRadius = equatorRadius * math.sqrt(1 - eccentricity2)
    val eccentricity = math.sqrt(eccentricity2)

    apply(name, shortName, equatorRadius, poleRadius, eccentricity, eccentricity2)
  }

  val INTERNATIONAL = Ellipsoid("intl",
    6378388.0, 0.0, 297.0, "International 1909 (Hayford)")

  val BESSEL = Ellipsoid("bessel", 6377397.155,
    0.0, 299.1528128, "Bessel 1841")

  val CLARKE_1866 = Ellipsoid("clrk66",
    6378206.4, 6356583.8, 0.0, "Clarke 1866")

  val CLARKE_1880 = Ellipsoid("clrk80",
    6378249.145, 0.0, 293.4663, "Clarke 1880 mod.")

  val AIRY = Ellipsoid("airy", 6377563.396,
    6356256.910, 0.0, "Airy 1830")

  val WGS60 = Ellipsoid("WGS60", 6378165.0, 0.0,
    298.3, "WGS 60")

  val WGS66 = Ellipsoid("WGS66", 6378145.0, 0.0,
    298.25, "WGS 66")

  val WGS72 = Ellipsoid("WGS72", 6378135.0, 0.0,
    298.26, "WGS 72")

  val WGS84 = Ellipsoid("WGS84", 6378137.0, 0.0,
    298.257223563, "WGS 84")

  val KRASSOVSKY = Ellipsoid("krass", 6378245.0,
    0.0, 298.3, "Krassovsky, 1942")

  val EVEREST = Ellipsoid("evrst30", 6377276.345,
    0.0, 300.8017, "Everest 1830")

  val INTERNATIONAL_1967 = Ellipsoid("new_intl",
    6378157.5, 6356772.2, 0.0, "International 1967")

  val GRS80 = Ellipsoid("GRS80", 6378137.0, 0.0,
    298.257222101, "GRS 1980 (IUGG, 1980)")

  val AUSTRALIAN = Ellipsoid("australian",
    6378160.0, 6356774.7, 298.25, "Australian")

  val MERIT = Ellipsoid("MERIT", 6378137.0, 0.0,
    298.257, "MERIT 1983")

  val SGS85 = Ellipsoid("SGS85", 6378136.0, 0.0,
    298.257, "Soviet Geodetic System 85")

  val IAU76 = Ellipsoid("IAU76", 6378140.0, 0.0,
    298.257, "IAU 1976")

  val APL4_9 = Ellipsoid("APL4.9", 6378137.0,
    0.0, 298.25, "Appl. Physics. 1965")

  val NWL9D = Ellipsoid("NWL9D", 6378145.0, 0.0,
    298.25, "Naval Weapons Lab., 1965")

  val MOD_AIRY = Ellipsoid("mod_airy",
    6377340.189, 6356034.446, 0.0, "Modified Airy")

  val ANDRAE = Ellipsoid("andrae", 6377104.43,
    0.0, 300.0, "Andrae 1876 (Den., Iclnd.)")

  val AUST_SA = Ellipsoid("aust_SA", 6378160.0,
    0.0, 298.25, "Australian Natl & S. Amer. 1969")

  val GRS67 = Ellipsoid("GRS67", 6378160.0, 0.0,
    298.2471674270, "GRS 67 (IUGG 1967)")

  val BESS_NAM = Ellipsoid("bess_nam",
    6377483.865, 0.0, 299.1528128, "Bessel 1841 (Namibia)")

  val CPM = Ellipsoid("CPM", 6375738.7, 0.0,
    334.29, "Comm. des Poids et Mesures 1799")

  val DELMBR = Ellipsoid("delmbr", 6376428.0,
    0.0, 311.5, "Delambre 1810 (Belgium)")

  val ENGELIS = Ellipsoid("engelis", 6378136.05,
    0.0, 298.2566, "Engelis 1985")

  val EVRST48 = Ellipsoid("evrst48", 6377304.063,
    0.0, 300.8017, "Everest 1948")

  val EVRST56 = Ellipsoid("evrst56", 6377301.243,
    0.0, 300.8017, "Everest 1956")

  val EVRTS69 = Ellipsoid("evrst69", 6377295.664,
    0.0, 300.8017, "Everest 1969")

  val EVRTSTSS = Ellipsoid("evrstSS",
    6377298.556, 0.0, 300.8017, "Everest (Sabah & Sarawak)")

  val FRSCH60 = Ellipsoid("fschr60", 6378166.0,
    0.0, 298.3, "Fischer (Mercury Datum) 1960")

  val FSRCH60M = Ellipsoid("fschr60m", 6378155.0,
    0.0, 298.3, "Modified Fischer 1960")

  val FSCHR68 = Ellipsoid("fschr68", 6378150.0,
    0.0, 298.3, "Fischer 1968")

  val HELMERT = Ellipsoid("helmert", 6378200.0,
    0.0, 298.3, "Helmert 1906")

  val HOUGH = Ellipsoid("hough", 6378270.0, 0.0,
    297.0, "Hough")

  val INTL = Ellipsoid("intl", 6378388.0, 0.0,
    297.0, "International 1909 (Hayford)")

  val KAULA = Ellipsoid("kaula", 6378163.0, 0.0,
    298.24, "Kaula 1961")

  val LERCH = Ellipsoid("lerch", 6378139.0, 0.0,
    298.257, "Lerch 1979")

  val MPRTS = Ellipsoid("mprts", 6397300.0, 0.0,
    191.0, "Maupertius 1738")

  val PLESSIS = Ellipsoid("plessis", 6376523.0,
    6355863.0, 0.0, "Plessis 1817 France)")

  val SEASIA = Ellipsoid("SEasia", 6378155.0,
    6356773.3205, 0.0, "Southeast Asia")

  val WALBECK = Ellipsoid("walbeck", 6376896.0,
    6355834.8467, 0.0, "Walbeck")

  val NAD27 = Ellipsoid("NAD27", 6378249.145,
    0.0, 293.4663, "NAD27: Clarke 1880 mod.")

  val NAD83 = Ellipsoid("NAD83", 6378137.0, 0.0,
    298.257222101, "NAD83: GRS 1980 (IUGG, 1980)")

  val SPHERE = Ellipsoid("sphere", 6371008.7714,
    6371008.7714, 0.0, "Sphere")

  val ellipsoids = Vector(
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
    SPHERE)

}

case class Ellipsoid(
  name: String,
  shortName: String,
  equatorRadius: Double = 1.0,
  poleRadius: Double = 1.0,
  eccentricity: Double = 1.0,
  eccentricity2: Double = 1.0
) {
  override def equals(that: Any) = that match {
    case e: Ellipsoid => 
      equatorRadius == e.equatorRadius && eccentricity2 == e.eccentricity2
    case _ => false
  }

  def equals(e: Ellipsoid, epsilon: Double) =  (equatorRadius != e.equatorRadius
    || math.abs(eccentricity2 - e.eccentricity2) > epsilon)

  def a: Double = equatorRadius

  def b: Double = poleRadius

  def eccentricitySquared: Double = eccentricity2

}
