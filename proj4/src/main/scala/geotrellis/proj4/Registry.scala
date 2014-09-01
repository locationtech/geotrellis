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

  private lazy val projectionRegistry = Map(
    "aea" -> classOf[AlbersProjection],
    "aeqd" -> classOf[EquidistantAzimuthalProjection],
    "airy" -> classOf[AiryProjection],
    "aitoff" -> classOf[AitoffProjection],
    "alsk" -> classOf[Projection],
    "apian" -> classOf[Projection],
    "august" -> classOf[AugustProjection],
    "bacon" -> classOf[Projection],
    "bipc" -> classOf[BipolarProjection],
    "boggs" -> classOf[BoggsProjection],
    "bonne" -> classOf[BonneProjection],
    "cass" -> classOf[CassiniProjection],
    "cc" -> classOf[CentralCylindricalProjection],
    "cea" -> classOf[Projection],
    "collg" -> classOf[CollignonProjection],
    "crast" -> classOf[CrasterProjection],
    "denoy" -> classOf[DenoyerProjection],
    "eck1" -> classOf[Eckert1Projection],
    "eck2" -> classOf[Eckert2Projection],
    "eck4" -> classOf[Eckert4Projection],
    "eck5" -> classOf[Eckert5Projection],
    "eck6" -> classOf[Eckert6Projection],
    "eqc" -> classOf[PlateCarreeProjection],
    "eqdc" -> classOf[EquidistantConicProjection],
    "euler" -> classOf[EulerProjection],
    "fahey" -> classOf[FaheyProjection],
    "fouc" -> classOf[FoucautProjection],
    "fouc_s" -> classOf[FoucautSinusoidalProjection],
    "gall" -> classOf[GallProjection],
    "gnom" -> classOf[GnomonicAzimuthalProjection],
    "goode" -> classOf[GoodeProjection],
    "hammer" -> classOf[HammerProjection],
    "hatano" -> classOf[HatanoProjection],
    "kav5" -> classOf[KavraiskyVProjection],
    "laea" -> classOf[LambertAzimuthalEqualAreaProjection],
    "lagrng" -> classOf[LagrangeProjection],
    "larr" -> classOf[LarriveeProjection],
    "lask" -> classOf[LaskowskiProjection],
    "latlong" -> classOf[LongLatProjection],
    "longlat" -> classOf[LongLatProjection],
    "lcc" -> classOf[LambertConformalConicProjection],
    "leac" -> classOf[LambertEqualAreaConicProjection],
    "loxim" -> classOf[LoximuthalProjection],
    "lsat" -> classOf[LandsatProjection],
    "mbt_fps" -> classOf[McBrydeThomasFlatPolarSine2Projection],
    "mbtfpp" -> classOf[McBrydeThomasFlatPolarParabolicProjection],
    "mbtfpq" -> classOf[McBrydeThomasFlatPolarQuarticProjection],
    "merc" -> classOf[MercatorProjection],
    "mill" -> classOf[MillerProjection],
    "moll" -> classOf[MolleweideProjection],
    "murd1" -> classOf[Murdoch1Projection],
    "murd2" -> classOf[Murdoch2Projection],
    "murd3" -> classOf[Murdoch3Projection],
    "nell" -> classOf[NellProjection],
    "nicol" -> classOf[NicolosiProjection],
    "nsper" -> classOf[PerspectiveProjection],
    "omerc" -> classOf[ObliqueMercatorProjection],
    "ortho" -> classOf[OrthographicAzimuthalProjection],
    "pconic" -> classOf[PerspectiveConicProjection],
    "poly" -> classOf[PolyconicProjection],
    "putp2" -> classOf[PutninsP2Projection],
    "putp4p" -> classOf[PutninsP4Projection],
    "putp5" -> classOf[PutninsP5Projection],
    "putp5p" -> classOf[PutninsP5PProjection],
    "qua_aut" -> classOf[QuarticAuthalicProjection],
    "robin" -> classOf[RobinsonProjection],
    "rpoly" -> classOf[RectangularPolyconicProjection],
    "sinu" -> classOf[SinusoidalProjection],
    "somerc" -> classOf[SwissObliqueMercatorProjection],
    "stere" -> classOf[StereographicAzimuthalProjection],
    "sterea" -> classOf[ObliqueStereographicAlternativeProjection],
    "tcc" -> classOf[TranverseCentralCylindricalProjection],
    "tcea" -> classOf[TransverseCylindricalEqualArea],
    "tmerc" -> classOf[TransverseMercatorProjection],
    "urmfps" -> classOf[UrmaevFlatPolarSinusoidalProjection],
    "utm" -> classOf[TransverseMercatorProjection],
    "vandg" -> classOf[VanDerGrintenProjection],
    "vitk1" -> classOf[VitkovskyProjection],
    "wag1" -> classOf[Wagner1Projection],
    "wag2" -> classOf[Wagner2Projection],
    "wag3" -> classOf[Wagner3Projection],
    "wag4" -> classOf[Wagner4Projection],
    "wag5" -> classOf[Wagner5Projection],
    "wag7" -> classOf[Wagner7Projection],
    "weren" -> classOf[WerenskioldProjection],
    "wintri" -> classOf[WinkelTripelProjection]
  )

  private lazy val projectionBuilderRegistry: Map[String, () => ProjectionBuilder] = 
    Map(
      ("aea" , () => new AlbersProjectionBuilder)
      // "aeqd" -> classOf[EquidistantAzimuthalProjection],
      // "airy" -> classOf[AiryProjection],
      // "aitoff" -> classOf[AitoffProjection],
      // "alsk" -> classOf[Projection],
      // "apian" -> classOf[Projection],
      // "august" -> classOf[AugustProjection],
      // "bacon" -> classOf[Projection],
      // "bipc" -> classOf[BipolarProjection],
      // "boggs" -> classOf[BoggsProjection],
      // "bonne" -> classOf[BonneProjection],
      // "cass" -> classOf[CassiniProjection],
      // "cc" -> classOf[CentralCylindricalProjection],
      // "cea" -> classOf[Projection],
      // "collg" -> classOf[CollignonProjection],
      // "crast" -> classOf[CrasterProjection],
      // "denoy" -> classOf[DenoyerProjection],
      // "eck1" -> classOf[Eckert1Projection],
      // "eck2" -> classOf[Eckert2Projection],
      // "eck4" -> classOf[Eckert4Projection],
      // "eck5" -> classOf[Eckert5Projection],
      // "eck6" -> classOf[Eckert6Projection],
      // "eqc" -> classOf[PlateCarreeProjection],
      // "eqdc" -> classOf[EquidistantConicProjection],
      // "euler" -> classOf[EulerProjection],
      // "fahey" -> classOf[FaheyProjection],
      // "fouc" -> classOf[FoucautProjection],
      // "fouc_s" -> classOf[FoucautSinusoidalProjection],
      // "gall" -> classOf[GallProjection],
      // "gnom" -> classOf[GnomonicAzimuthalProjection],
      // "goode" -> classOf[GoodeProjection],
      // "hammer" -> classOf[HammerProjection],
      // "hatano" -> classOf[HatanoProjection],
      // "kav5" -> classOf[KavraiskyVProjection],
      // "laea" -> classOf[LambertAzimuthalEqualAreaProjection],
      // "lagrng" -> classOf[LagrangeProjection],
      // "larr" -> classOf[LarriveeProjection],
      // "lask" -> classOf[LaskowskiProjection],
      // "latlong" -> classOf[LongLatProjection],
      // "longlat" -> classOf[LongLatProjection],
      // "lcc" -> classOf[LambertConformalConicProjection],
      // "leac" -> classOf[LambertEqualAreaConicProjection],
      // "loxim" -> classOf[LoximuthalProjection],
      // "lsat" -> classOf[LandsatProjection],
      // "mbt_fps" -> classOf[McBrydeThomasFlatPolarSine2Projection],
      // "mbtfpp" -> classOf[McBrydeThomasFlatPolarParabolicProjection],
      // "mbtfpq" -> classOf[McBrydeThomasFlatPolarQuarticProjection],
      // "merc" -> classOf[MercatorProjection],
      // "mill" -> classOf[MillerProjection],
      // "moll" -> classOf[MolleweideProjection],
      // "murd1" -> classOf[Murdoch1Projection],
      // "murd2" -> classOf[Murdoch2Projection],
      // "murd3" -> classOf[Murdoch3Projection],
      // "nell" -> classOf[NellProjection],
      // "nicol" -> classOf[NicolosiProjection],
      // "nsper" -> classOf[PerspectiveProjection],
      // "omerc" -> classOf[ObliqueMercatorProjection],
      // "ortho" -> classOf[OrthographicAzimuthalProjection],
      // "pconic" -> classOf[PerspectiveConicProjection],
      // "poly" -> classOf[PolyconicProjection],
      // "putp2" -> classOf[PutninsP2Projection],
      // "putp4p" -> classOf[PutninsP4Projection],
      // "putp5" -> classOf[PutninsP5Projection],
      // "putp5p" -> classOf[PutninsP5PProjection],
      // "qua_aut" -> classOf[QuarticAuthalicProjection],
      // "robin" -> classOf[RobinsonProjection],
      // "rpoly" -> classOf[RectangularPolyconicProjection],
      // "sinu" -> classOf[SinusoidalProjection],
      // "somerc" -> classOf[SwissObliqueMercatorProjection],
      // "stere" -> classOf[StereographicAzimuthalProjection],
      // "sterea" -> classOf[ObliqueStereographicAlternativeProjection],
      // "tcc" -> classOf[TranverseCentralCylindricalProjection],
      // "tcea" -> classOf[TransverseCylindricalEqualArea],
      // "tmerc" -> classOf[TransverseMercatorProjection],
      // "urmfps" -> classOf[UrmaevFlatPolarSinusoidalProjection],
      // "utm" -> classOf[TransverseMercatorProjection],
      // "vandg" -> classOf[VanDerGrintenProjection],
      // "vitk1" -> classOf[VitkovskyProjection],
      // "wag1" -> classOf[Wagner1Projection],
      // "wag2" -> classOf[Wagner2Projection],
      // "wag3" -> classOf[Wagner3Projection],
      // "wag4" -> classOf[Wagner4Projection],
      // "wag5" -> classOf[Wagner5Projection],
      // "wag7" -> classOf[Wagner7Projection],
      // "weren" -> classOf[WerenskioldProjection],
      // "wintri" -> classOf[WinkelTripelProjection]
    )


  def getProjectionBuilder(name: String): Option[ProjectionBuilder] =
    projectionBuilderRegistry.get(name) match {
      case Some(builder) => Some(builder())
      case None => None
    }

  // def getProjection(name: String): Option[Projection] = 
  //   projectionRegistry.get(name) match {
  //     case Some(clas) => try {
  //       val projection = clas.newInstance
  //       projection.setName(name)
  //       Some(projection)
  //     } catch {
  //       case iae: IllegalAccessException => {
  //         iae.printStackTrace
  //         None
  //       }
  //       case ie: InstantiationException => {
  //         ie.printStackTrace
  //         None
  //       }
  //     }
  //     case None => None
  //   }

  def getEllipsoid(name: String): Option[Ellipsoid] =
    ellipsoids.filter(_.shortName == name).headOption

  def getDatum(code: String): Option[Datum] =
    datums.filter(_.code == code).headOption

}
