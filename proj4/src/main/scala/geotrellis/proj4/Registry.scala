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

import geotrellis.proj4.proj.ProjectionBuilders._
import geotrellis.proj4.proj._

object Registry {

  def apply: Registry = new Registry()

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

  private val projectionBuilderRegistry = HashMap[String, ProjectionBuilder](
    albersProjectionName -> albersProjectionBuilder,
    equidistantAzimuthalProjectionName -> equidistantAzimuthalProjectionBuilder,
    airyProjectionName -> airyProjecitonBuilder,
    aitoffProjectionName -> aitoffProjectionBuilder,
    alskProjectionName -> alskProjectionBuilder,
    apianProjectionName -> apianProjectionBuilder,
    augustProjectionName -> augustProjectionBuilder,
    baconProjectionName -> baconProjectionBuilder,
    bipolarProjectionName -> bipolarProjectionBuilder,
    boggsProjectionName -> boggsProjectionBuilder,
    bonneProjectionName -> bonneProjectionBuilder,
    cassiniProjectionName -> cassiniProjectionBuilder,
    centralCylindricalProjectionName -> centralCylindricalProjectionBuilder,
    ceaProjectionName -> ceaProjectionBuilder,
    collignonProjectionName -> collignonProjectionBuilder,
    crasterProjectionName -> crasterProjectionBuilder,
    denoyerProjectionName -> denoyerProjectionBuilder,
    eckert1ProjectionName -> eckert1ProjectionBuilder,
    eckert2ProjectionName -> eckert2ProjectionBuilder,
    eckert4ProjectionName -> eckert4ProjectionBuilder,
    eckert5ProjectionName -> eckert5ProjectionBuilder,
    eckert6ProjectionName -> eckert6ProjectionBuilder,
    plateCarreeProjectionName -> plateCarreeProjectionBuilder,
    equidistantConicProjectionName -> equidistantConicProjectionBuilder,
    eulerProjectionName -> eulerProjectionBuilder,
    faheyProjectionName -> faheyProjectionBuilder,
    foucautProjectionName -> foucautProjectionBuilder,
    foucautSinusoidalProjectionName -> foucautSinusoidalProjectionBuilder,
    gallProjectionName -> gallProjectionBuilder,
    gnomonicAzimuthalProjectionName -> gnomonicAzimuthalProjectionBuilder,
    goodeProjectionName -> goodeProjectionBuilder,
    hammerProjectionName -> hammerProjectionBuilder,
    hatanoProjectionName -> hatanoProjectionBuilder,
    kavraiskyProjectionName -> kavraiskyProjectionBuilder,
    lambertAzimuthalEqualAreaProjectionName ->
      lambertAzimuthalEqualAreaProjectionBuilder,
    lagrangeProjectionName -> lagrangeProjectionBuilder,
    larriveeProjectionName -> larriveeProjectionBuilder,
    laskowskiProjectionName -> laskowskiProjectionBuilder,
    latLongProjectionName -> latLongProjectionBuilder,
    longLatProjectionName -> longLatProjectionBuilder,
    lambertConformalConicProjectionName -> lambertConformalConicProjectionBuilder,
    lambertEqualAreaConicProjectionName -> lambertEqualAreaConicProjectionBuilder,
    loximuthalProjectionName -> loximuthalProjectionBuilder,
    landsatProjectionName -> landsatProjectionBuilder,
    mcBrydeThomasFlatPolarSine2ProjectionName ->
      mcBrydeThomasFlatPolarSine2ProjectionBuilder,
    mcBrydeThomasFlatPolarParabolicProjectionName ->
      mcBrydeThomasFlatPolarParabolicProjectionBuilder,
    mcBrydeThomasFlatPolarQuarticProjectionName ->
      mcBrydeThomasFlatPolarQuarticProjectionBuilder,
    mercatorProjectionName -> mercatorProjectionBuilder,
    millerProjectionName -> millerProjectionBuilder,
    molleweideProjectionName -> molleweideProjectionBuilder,
    murdoch1ProjectionName -> murdoch1ProjectionBuilder,
    murdoch2ProjectionName -> murdoch2ProjectionBuilder,
    murdoch3ProjectionName -> murdoch3ProjectionBuilder,
    nellProjectionName -> nellProjectionBuilder,
    nicolosiProjectionName -> nicolosiProjectionBuilder,
    perspectiveProjectionName -> perspectiveProjectionBuilder,
    obliqueMercatorProjectionName -> obliqueMercatorProjectionBuilder,
    ortographicAzimuthalProjectionName -> ortographicAzimuthalProjectionBuilder,
    perspectiveConicProjectionName -> perspectiveConicProjectionBuilder,
    polyconicProjectionName -> polyconicProjectionBuilder,
    putninsP2ProjectionName -> putninsP2ProjectionBuilder,
    putninsP4PProjectionName -> putninsP4PProjectionBuilder,
    putninsP5ProjectionName -> putninsP5ProjectionBuilder,
    putninsP5PProjectionName -> putninsP5PProjectionBuilder,
    quarticAuthalicProjectionName -> quarticAuthalicProjectionBuilder,
    robinsonProjectionName -> robinsonProjectionBuilder,
    rectangularPolyconicProjectionName -> rectangularPolyconicProjectionBuilder,
    sinusoidalProjectionName -> sinusoidalProjectionBuilder,
    swissObliqueMercatorProjectionName -> swissObliqueMercatorProjectionBuilder,
    stereographicAzimuthalProjectionName -> stereographicAzimuthalProjectionBuilder,
    obliqueStereographicAlternativeProjectionName ->
      obliqueStereographicAlternativeProjectionBuilder,
    transverseCentralCylindricalProjectionName ->
      transverseCentralCylindricalProjectionBuilder,
    transverseCylindricalEqualAreaProjectionName ->
      transverseCylindricalEqualAreaProjectionBuilder,
    tmercProjectionName -> tmercProjectionBuilder,
    urmaevFlatPolarSinusoidalProjectionName ->
      urmaevFlatPolarSinusoidalProjectionBuilder,
    vanDerGrintenProjectionName -> vanDerGrintenProjectionBuilder,
    vitkovskyProjectionName -> vitkovskyProjectionBuilder,
    wagner1ProjectionName -> wagner1ProjectionBuilder,
    wagner2ProjectionName -> wagner2ProjectionBuilder,
    wagner3ProjectionName -> wagner3ProjectionBuilder,
    wagner4ProjectionName -> wagner4ProjectionBuilder,
    wagner5ProjectionName -> wagner5ProjectionBuilder,
    wagner7ProjectionName -> wagner7ProjectionBuilder,
    werenskioldProjectionName -> werenskioldProjectionBuilder,
    winkelTripelProjectionName -> winkelTripelProjectionBuilder
  )
}

class Registry {

  import Registry._

  def getProjectionBuilder(name: String): Option[ProjectionBuilder] =
    projectionBuilderRegistry.get(name)

  def getEllipsoid(name: String): Option[Ellipsoid] =
    ellipsoids.filter(_.shortName == name).headOption

  def getDatum(code: String): Option[Datum] =
    datums.filter(_.code == code).headOption

}
