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

package geotrellis.proj4

import geotrellis.proj4.datum._
import geotrellis.proj4.proj._

import org.osgeo.proj4j.proj._

object Registry {

  lazy val datums = Array(
    Datum.WGS84,
    Datum.GGRS87,
    Datum.NAD27,
    Datum.NAD83,
    Datum.POTSDAM,
    Datum.CARTHAGE,
    Datum.HERMANNSKOGEL,
    Datum.IRE65,
    Datum.NZGD49,
    Datum.OSEB36
  )

  lazy val ellipsoids = Array(
    Ellipsoid.SPHERE,
    Ellipsoid("MERIT", 6378137.0, 0.0, 298.257, "MERIT 1983"),
    Ellipsoid("SGS85", 6378136.0, 0.0, 298.257, "Soviet Geodetic System 85"),
    Ellipsoid.GRS80,
    Ellipsoid("IAU76", 6378140.0, 0.0, 298.257, "IAU 1976"),
    Ellipsoid.AIRY,
    Ellipsoid.MOD_AIRY,
    Ellipsoid("APL4.9", 6378137.0, 0.0, 298.25, "Appl. Physics. 1965"),
    Ellipsoid("NWL9D", 6378145.0, 298.25, 0.0, "Naval Weapons Lab., 1965"),
    Ellipsoid("andrae", 6377104.43, 300.0, 0.0, "Andrae 1876 (Den., Iclnd.)"),
    Ellipsoid("aust_SA", 6378160.0, 0.0, 298.25, "Australian Natl & S. Amer. 1969"),
    Ellipsoid("GRS67", 6378160.0, 0.0, 298.2471674270, "GRS 67 (IUGG 1967)"),
    Ellipsoid.BESSEL,
    Ellipsoid("bess_nam", 6377483.865, 0.0, 299.1528128, "Bessel 1841 (Namibia)"),
    Ellipsoid.CLARKE_1866,
    Ellipsoid.CLARKE_1880,
    Ellipsoid("CPM", 6375738.7, 0.0, 334.29, "Comm. des Poids et Mesures 1799"),
    Ellipsoid("delmbr", 6376428.0, 0.0, 311.5, "Delambre 1810 (Belgium)"),
    Ellipsoid("engelis", 6378136.05, 0.0, 298.2566, "Engelis 1985"),
    Ellipsoid.EVEREST,
    Ellipsoid("evrst48", 6377304.063, 0.0, 300.8017, "Everest 1948"),
    Ellipsoid("evrst56", 6377301.243, 0.0, 300.8017, "Everest 1956"),
    Ellipsoid("evrst69", 6377295.664, 0.0, 300.8017, "Everest 1969"),
    Ellipsoid("evrstSS", 6377298.556, 0.0, 300.8017, "Everest (Sabah & Sarawak)"),
    Ellipsoid("fschr60", 6378166.0, 0.0, 298.3, "Fischer (Mercury Datum) 1960"),
    Ellipsoid("fschr60m", 6378155.0, 0.0, 298.3, "Modified Fischer 1960"),
    Ellipsoid("fschr68", 6378150.0, 0.0, 298.3, "Fischer 1968"),
    Ellipsoid("helmert", 6378200.0, 0.0, 298.3, "Helmert 1906"),
    Ellipsoid("hough", 6378270.0, 0.0, 297.0, "Hough"),
    Ellipsoid.INTERNATIONAL,
    Ellipsoid.INTERNATIONAL_1967,
    Ellipsoid.KRASSOVSKY,
    Ellipsoid("kaula", 6378163.0, 0.0, 298.24, "Kaula 1961"),
    Ellipsoid("lerch", 6378139.0, 0.0, 298.257, "Lerch 1979"),
    Ellipsoid("mprts", 6397300.0, 0.0, 191.0, "Maupertius 1738"),
    Ellipsoid("plessis", 6376523.0, 6355863.0, 0.0, "Plessis 1817 France)"),
    Ellipsoid("SEasia", 6378155.0, 6356773.3205, 0.0, "Southeast Asia"),
    Ellipsoid("walbeck", 6376896.0, 6355834.8467, 0.0, "Walbeck"),
    Ellipsoid.WGS60,
    Ellipsoid.WGS66,
    Ellipsoid.WGS72,
    Ellipsoid.WGS84,
    Ellipsoid("NAD27", 6378249.145, 0.0, 293.4663, "NAD27: Clarke 1880 mod."),
    Ellipsoid("NAD83", 6378137.0, 0.0, 298.257222101, "NAD83: GRS 1980 (IUGG, 1980)")
  )

  private lazy val projectionTypes: Map[String, ProjectionType] =
    Map(
      "aea" -> AlbersType,
      "aeqd" -> EquidistantAzimuthalType,
      "airy" -> AiryType,
      "aitoff" -> AitoffType,
      "alsk" -> DefaultProjectionType,
      "apian" -> DefaultProjectionType,
      "august" -> AugustType,
      "bacon" -> DefaultProjectionType,
      "bipc" -> BipolarType,
      "boggs" -> BoggsType,
      "bonne" -> BonneType,
      "cass" -> CassiniType,
      "cc" -> CentralCylindricalType,
      "cea" -> DefaultProjectionType,
      "collg" -> CollignonType,
      "crast" -> CrasterType,
      "denoy" -> DenoyerType,
      "eck1" -> Eckert1Type,
      "eck2" -> Eckert2Type,
      "eck4" -> Eckert4Type,
      "eck5" -> Eckert5Type,
      "eck6" -> Eckert6Type,
      "eqc" -> PlateCarreeType,
      "eqdc" -> EquidistantConicType,
      "euler" -> EulerType,
      "fahey" -> FaheyType,
      "fouc" -> FoucautType,
      "fouc_s" -> FoucautSinusoidalType,
      "gall" -> GallType,
      "gnom" -> GnomonicAzimuthalType,
      "goode" -> GoodeType,
      "hammer" -> HammerType,
      "hatano" -> HatanoType,
      "kav5" -> KavraiskyVType,
      "laea" -> LambertAzimuthalEqualAreaType,
      "lagrng" -> LagrangeType,
      "larr" -> LarriveeType,
      "lask" -> LaskowskiType,
      "latlong" -> LongLatType,
      "longlat" -> LongLatType,
      "lcc" -> LambertConformalConicType,
      "leac" -> LambertEqualAreaConicType,
      "loxim" -> LoximuthalType,
      "lsat" -> LandsatType,
      "mbt_fps" -> McBrydeThomasFlatPolarSine2Type,
      "mbtfpp" -> McBrydeThomasFlatPolarParabolicType,
      "mbtfpq" -> McBrydeThomasFlatPolarQuarticType,
      "merc" -> MercatorType,
      "mill" -> MillerType,
      "moll" -> MolleweideType,
      "murd1" -> Murdoch1Type,
      "murd2" -> Murdoch2Type,
      "murd3" -> Murdoch3Type,
      "nell" -> NellType,
      "nicol" -> NicolosiType,
      "nsper" -> PerspectiveType,
      "omerc" -> ObliqueMercatorType,
      "ortho" -> OrthographicAzimuthalType,
      "pconic" -> PerspectiveConicType,
      "poly" -> PolyconicType,
      "putp2" -> PutninsP2Type,
      "putp4p" -> PutninsP4Type,
      "putp5" -> PutninsP5Type,
      "putp5p" -> PutninsP5PType,
      "qua_aut" -> QuarticAuthalicType,
      "robin" -> RobinsonType,
      "rpoly" -> RectangularPolyconicType,
      "sinu" -> SinusoidalType,
      "somerc" -> SwissObliqueMercatorType,
      "stere" -> StereographicAzimuthalType,
      "sterea" -> ObliqueStereographicAlternativeType,
      "tcc" -> TranverseCentralCylindricalType,
      "tcea" -> TransverseCylindricalEqualAreaType,
      "tmerc" -> TransverseMercatorType,
      "urmfps" -> UrmaevFlatPolarSinusoidalType,
      "utm" -> TransverseMercatorType,
      "vandg" -> VanDerGrintenType,
      "vitk1" -> VitkovskyType,
      "wag1" -> Wagner1Type,
      "wag2" -> Wagner2Type,
      "wag3" -> Wagner3Type,
      "wag4" -> Wagner4Type,
      "wag5" -> Wagner5Type,
      "wag7" -> Wagner7Type,
      "weren" -> WerenskioldType,
      "wintri" -> WinkelTripelType
    )


  def getProjectionType(name: String): Option[ProjectionType] =
    projectionTypes.get(name)

  def getEllipsoid(name: String): Option[Ellipsoid] =
    ellipsoids.filter(_.shortName == name).headOption

  def getDatum(code: String): Option[Datum] =
    datums.filter(_.code == code).headOption

}
