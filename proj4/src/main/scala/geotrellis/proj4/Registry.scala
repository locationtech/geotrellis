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

import scala.collection.immutable.HashMap

object Registry {

  val datums = Array(
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

  val ellipsoids = Array(
    Ellipsoid.SPHERE,
    new Ellipsoid("MERIT", 6378137.0, 0.0, 298.257, "MERIT 1983"),
    new Ellipsoid("SGS85", 6378136.0, 0.0, 298.257, "Soviet Geodetic System 85"),
    Ellipsoid.GRS80,
    new Ellipsoid("IAU76", 6378140.0, 0.0, 298.257, "IAU 1976"),
    Ellipsoid.AIRY,
    Ellipsoid.MOD_AIRY,
    new Ellipsoid("APL4.9", 6378137.0, 0.0, 298.25, "Appl. Physics. 1965"),
    new Ellipsoid("NWL9D", 6378145.0, 298.25, 0.0, "Naval Weapons Lab., 1965"),
    new Ellipsoid("andrae", 6377104.43, 300.0, 0.0, "Andrae 1876 (Den., Iclnd.)"),
    new Ellipsoid("aust_SA", 6378160.0, 0.0, 298.25, "Australian Natl & S. Amer. 1969"),
    new Ellipsoid("GRS67", 6378160.0, 0.0, 298.2471674270, "GRS 67 (IUGG 1967)"),
    Ellipsoid.BESSEL,
    new Ellipsoid("bess_nam", 6377483.865, 0.0, 299.1528128, "Bessel 1841 (Namibia)"),
    Ellipsoid.CLARKE_1866,
    Ellipsoid.CLARKE_1880,
    new Ellipsoid("CPM", 6375738.7, 0.0, 334.29, "Comm. des Poids et Mesures 1799"),
    new Ellipsoid("delmbr", 6376428.0, 0.0, 311.5, "Delambre 1810 (Belgium)"),
    new Ellipsoid("engelis", 6378136.05, 0.0, 298.2566, "Engelis 1985"),
    Ellipsoid.EVEREST,
    new Ellipsoid("evrst48", 6377304.063, 0.0, 300.8017, "Everest 1948"),
    new Ellipsoid("evrst56", 6377301.243, 0.0, 300.8017, "Everest 1956"),
    new Ellipsoid("evrst69", 6377295.664, 0.0, 300.8017, "Everest 1969"),
    new Ellipsoid("evrstSS", 6377298.556, 0.0, 300.8017, "Everest (Sabah & Sarawak)"),
    new Ellipsoid("fschr60", 6378166.0, 0.0, 298.3, "Fischer (Mercury Datum) 1960"),
    new Ellipsoid("fschr60m", 6378155.0, 0.0, 298.3, "Modified Fischer 1960"),
    new Ellipsoid("fschr68", 6378150.0, 0.0, 298.3, "Fischer 1968"),
    new Ellipsoid("helmert", 6378200.0, 0.0, 298.3, "Helmert 1906"),
    new Ellipsoid("hough", 6378270.0, 0.0, 297.0, "Hough"),
    Ellipsoid.INTERNATIONAL,
    Ellipsoid.INTERNATIONAL_1967,
    Ellipsoid.KRASSOVSKY,
    new Ellipsoid("kaula", 6378163.0, 0.0, 298.24, "Kaula 1961"),
    new Ellipsoid("lerch", 6378139.0, 0.0, 298.257, "Lerch 1979"),
    new Ellipsoid("mprts", 6397300.0, 0.0, 191.0, "Maupertius 1738"),
    new Ellipsoid("plessis", 6376523.0, 6355863.0, 0.0, "Plessis 1817 France)"),
    new Ellipsoid("SEasia", 6378155.0, 6356773.3205, 0.0, "Southeast Asia"),
    new Ellipsoid("walbeck", 6376896.0, 6355834.8467, 0.0, "Walbeck"),
    Ellipsoid.WGS60,
    Ellipsoid.WGS66,
    Ellipsoid.WGS72,
    Ellipsoid.WGS84,
    new Ellipsoid("NAD27", 6378249.145, 0.0, 293.4663, "NAD27: Clarke 1880 mod."),
    new Ellipsoid("NAD83", 6378137.0, 0.0, 298.257222101, "NAD83: GRS 1980 (IUGG, 1980)")
  )

  def getEllipsoid(name: String) = ellipsoids.filter(_.shortName == name).headOption


  private val projectionRegistry = HashMap[String, Class](
    "aea" -> AlbersProjection.class,
    "aeqd" -> EquidistantAzimuthalProjection.class,
    "airy" -> AiryProjection.class,
    "aitoff" -> AitoffProjection.class,
    "alsk" -> Projection.class,
    "apian" -> Projection.class,
    "august" -> AugustProjection.class,
    "bacon" -> Projection.class,
    "bipc" -> BipolarProjection.class,
    "boggs" -> BoggsProjection.class,
    "bonne" -> BonneProjection.class,
    "cass" -> CassiniProjection.class,
    "cc" -> CentralCylindricalProjection.class,
    "cea" -> Projection.class,
    "collg" -> CollignonProjection.class,
    "crast" -> CrasterProjection.class,
    "denoy" -> DenoyerProjection.class,
    "eck1" -> Eckert1Projection.class,
    "eck2" -> Eckert2Projection.class,
    "eck4" -> Eckert4Projection.class,
    "eck5" -> Eckert5Projection.class,
    "eck6" -> Eckert6Projection.class,
    "eqc" -> PlateCarreeProjection.class,
    "eqdc" -> EquidistantConicProjection.class,
    "euler" -> EulerProjection.class,
    "fahey" -> FaheyProjection.class,
    "fouc" -> FoucautProjection.class,
    "fouc_s" -> FoucautSinusoidalProjection.class,
    "gall" -> GallProjection.class,
    "gnom" -> GnomonicAzimuthalProjection.class,
    "goode" -> GoodeProjection.class,
    "hammer" -> HammerProjection.class,
    "hatano" -> HatanoProjection.class,
    "kav5" -> KavraiskyVProjection.class,
    "laea" -> LambertAzimuthalEqualAreaProjection.class,
    "lagrng" -> LagrangeProjection.class,
    "larr" -> LarriveeProjection.class,
    "lask" -> LaskowskiProjection.class,
    "latlong" -> LongLatProjection.class,
    "longlat" -> LongLatProjection.class,
    "lcc" -> LambertConformalConicProjection.class,
    "leac" -> LambertEqualAreaConicProjection.class,
    "loxim" -> LoximuthalProjection.class,
    "lsat" -> LandsatProjection.class,
    "mbt_fps" -> McBrydeThomasFlatPolarSine2Projection.class,
    "mbtfpp" -> McBrydeThomasFlatPolarParabolicProjection.class,
    "mbtfpq" -> McBrydeThomasFlatPolarQuarticProjection.class,
    "merc" -> MercatorProjection.class,
    "mill" -> MillerProjection.class,
    "moll" -> MolleweideProjection.class,
    "murd1" -> Murdoch1Projection.class,
    "murd2" -> Murdoch2Projection.class,
    "murd3" -> Murdoch3Projection.class,
    "nell" -> NellProjection.class,
    "nicol" -> NicolosiProjection.class,
    "nsper" -> PerspectiveProjection.class,
    "omerc" -> ObliqueMercatorProjection.class,
    "ortho" -> OrthographicAzimuthalProjection.class,
    "pconic" -> PerspectiveConicProjection.class,
    "poly" -> PolyconicProjection.class,
    "putp2" -> PutninsP2Projection.class,
    "putp4p" -> PutninsP4Projection.class,
    "putp5" -> PutninsP5Projection.class,
    "putp5p" -> PutninsP5PProjection.class,
    "qua_aut" -> QuarticAuthalicProjection.class,
    "robin" -> RobinsonProjection.class,
    "rpoly" -> RectangularPolyconicProjection.class,
    "sinu" -> SinusoidalProjection.class,
    "somerc" -> SwissObliqueMercatorProjection.class,
    "stere" -> StereographicAzimuthalProjection.class,
    "sterea" -> ObliqueStereographicAlternativeProjection.class,
    "tcc" -> TranverseCentralCylindricalProjection.class,
    "tcea" -> TransverseCylindricalEqualArea.class,
    "tmerc" -> TransverseMercatorProjection.class,
    "urmfps" -> UrmaevFlatPolarSinusoidalProjection.class,
    "utm" -> TransverseMercatorProjection.class,
    "vandg" -> VanDerGrintenProjection.class,
    "vitk1" -> VitkovskyProjection.class,
    "wag1" -> Wagner1Projection.class,
    "wag2" -> Wagner2Projection.class,
    "wag3" -> Wagner3Projection.class,
    "wag4" -> Wagner4Projection.class,
    "wag5" -> Wagner5Projection.class,
    "wag7" -> Wagner7Projection.class,
    "weren" -> WerenskioldProjection.class,
    "wintri" -> WinkelTripelProjection.class
  )
}

class Registry {

  import Registry._

  def getProjection(name: String): Option[Projection] = projectionRegistry.get(name) match {
    case Some(clas) => try {
      val projection = clas.newInstance
      projection.setName(name)
      Some(Projection)
    } catch {
      case iae: IllegalAccessException => {
        iae.printStackTrace
        None
      }
      case ie: InstantiationException => {
        ie.printStackTrace
        None
      }
    }
    case None => None
  }

}
