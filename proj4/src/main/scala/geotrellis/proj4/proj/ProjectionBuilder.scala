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

package geotrellis.proj4.proj

import monocle.syntax._
import monocle.Macro._

import geotrellis.proj4.util.ProjectionMath._
import geotrellis.proj4.datum.Ellipsoid

case class ProjectionCoordinates(
  minLatitude: Double = - math.Pi / 2,
  minLongitude: Double = -math.Pi,
  maxLatitude: Double = math.Pi / 2,
  maxLongitude: Double = math.Pi,
  projectionLatitude: Double = 0.0,
  projectionLongitude: Double = 0.0,
  projectionLatitude1: Double = 0.0,
  projectionLatitude2: Double = 0.0,
  trueScaleLatitude: Double = 0.0
)

case class ProjectionBuilder(
  name: String,
  coordinates: ProjectionCoordinates = ProjectionCoordinates(),
  alpha: Double = Double.NaN,
  lonC: Double = Double.NaN,
  scaleFactor: Double = 1.0,
  falseEasting: Double = 0,
  falseNorthing: Double = 0,
  isSouth: Boolean = false,
  ellipsoid: Ellipsoid = Ellipsoid.SPHERE,
  fromMetres: Double = 1,
  unitOption: Option[Unit] = None,
  zoneOption: Option[Int] = None
) {

  import ProjectionBuilderLenses._

  def setAlphaDegrees(alpha: Double): ProjectionBuilder =
    this |-> alphaLens set DTR * alpha

  def setLonCDegrees(lonC: Double): ProjectionBuilder =
    this |-> lonCLens set DTR * lonC

  def setProjectionLatitudeDegrees(
    projectionLatitude: Double): ProjectionBuilder =
    this |-> projectionLatitudeLens set DTR * coordinates.projectionLatitude

  def setProjectionLongitudeDegrees(
    projectionLongitude: Double): ProjectionBuilder =
    this |-> projectionLongitudeLens set DTR * coordinates.projectionLongitude

  def setProjectionLatitude1Degrees(
    projectionLatitude: Double): ProjectionBuilder =
    this |-> projectionLatitude1Lens set DTR * coordinates.projectionLatitude1

  def setProjectionLatitude2Degrees(
    projectionLatitude: Double): ProjectionBuilder =
    this |-> projectionLatitude2Lens set DTR * coordinates.projectionLatitude2

  def setTrueScaleLatitudeDegrees(
    trueScaleLatitude: Double): ProjectionBuilder =
    this |-> trueScaleLatitudeLens set DTR * coordinates.trueScaleLatitude

  def setUTMZone(zone: Int): ProjectionBuilder = {
    var pb = this

    pb = pb |-> zoneOptionLens set Some(zone)
    val pLong = (zone - 1 + 0.5) * math.Pi / 30 - math.Pi
    pb = pb |-> projectionLongitudeLens set pLong
    pb = pb |-> projectionLatitudeLens set 0.0
    pb = pb |-> scaleFactorLens set 0.9996
    pb = pb |-> falseEastingLens set 500000
    val falseNorthing = if (pb.isSouth) 10000000.0 else 0.0
    pb = pb |-> falseNorthingLens set falseNorthing

    pb
  }

  def build: Projection = {
    null // TODO: implement
  }

}

object ProjectionBuilderLenses {

  val nameLens = mkLens[ProjectionBuilder, String]("name")

  val coordinatesLens =
    mkLens[ProjectionBuilder, ProjectionCoordinates]("coordinates")

  val minLatitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("minLatitude")

  val minLongitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("minLongitude")

  val maxLatitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("maxLatitude")

  val maxLongitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("maxLongitude")

  val projectionLatitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("projectionLatitude")

  val projectionLongitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("projectionLongitude")

  val projectionLatitude1Lens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("projectionLatitude1")

  val projectionLatitude2Lens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("projectionLatitude2")

  val trueScaleLatitudeLens =
    coordinatesLens |-> mkLens[ProjectionCoordinates, Double]("trueScaleLatitude")

  val alphaLens = mkLens[ProjectionBuilder, Double]("alpha")

  val lonCLens = mkLens[ProjectionBuilder, Double]("lonC")

  val scaleFactorLens = mkLens[ProjectionBuilder, Double]("scaleFactor")

  val falseEastingLens = mkLens[ProjectionBuilder, Double]("falseEasting")

  val falseNorthingLens = mkLens[ProjectionBuilder, Double]("falseNorthing")

  val isSouthLens = mkLens[ProjectionBuilder, Boolean]("isSouth")

  val ellipsoidLens = mkLens[ProjectionBuilder, Ellipsoid]("ellipsoid")

  val fromMetresLens = mkLens[ProjectionBuilder, Double]("fromMetres")

  val unitOptionLens = mkLens[ProjectionBuilder, Option[Unit]]("unitOption")

  val zoneOptionLens = mkLens[ProjectionBuilder, Option[Int]]("zoneOption")
}

object ProjectionBuilders {

  import ProjectionBuilderLenses._

  val albersProjectionName: String = "aea"

  val albersProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = albersProjectionName)

  val equidistantAzimuthalProjectionName: String = "aeqd"

  val equidistanceAzimuthalProjectionBuilder: ProjectionBuilder =
    setAzimuthalProjectionProperties(
      ProjectionBuilder(name = equidistantAzimuthalProjectionName),
      math.toRadians(90),
      math.toRadians(0)
    )

  val airyProjectionName: String = "airy"

  val airyProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = airyProjectionName)

    projectionBuilder = projectionBuilder |-> minLatitudeLens set math.toRadians(-60)
    projectionBuilder = projectionBuilder |-> maxLatitudeLens set math.toRadians(60)
    projectionBuilder = projectionBuilder |-> minLongitudeLens set math.toRadians(-90)
    projectionBuilder = projectionBuilder |-> maxLongitudeLens set math.toRadians(90)

    projectionBuilder
  }

  val aitoffProjectionName: String = "aitoff"

  val aitoffProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = aitoffProjectionName)

    projectionBuilder = projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder = projectionBuilder |-> maxLatitudeLens set math.toRadians(80)
    projectionBuilder = projectionBuilder |-> projectionLatitude1Lens set degToRad(45.5)
    projectionBuilder = projectionBuilder |-> projectionLatitude1Lens set degToRad(29.5)

    projectionBuilder
  }

  val alskProjectionName: String = "alsk"

  val alskProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(alskProjectionName)

  val apianProjectionName: String = "apian"

  val apianProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(apianProjectionName)

  val augustProjectionName: String = "august"

  val augustProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = augustProjectionName)

  val baconProjectionName: String = "bacon"

  val baconProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = baconProjectionName)

  val bipolarProjectionName: String = "bipc"

  val bipolarProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = bipolarProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(-80)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)
    projectionBuilder =
      projectionBuilder |-> projectionLongitudeLens set math.toRadians(-90)
    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-90)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(90)

    projectionBuilder
  }

  val boggsProjectionName: String = "boggs"

  val boggsProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = boggsProjectionName)

  val bonneProjectionName: String = "bonne"

  val bonneProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = bonneProjectionName)

  val cassiniProjectionName: String = "cass"

  val cassiniProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = cassiniProjectionName)

    projectionBuilder =
      projectionBuilder |-> projectionLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> projectionLongitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-90)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(90)

    projectionBuilder
  }

  val centralCylindricalProjectionName: String = "cc"

  val centralCylindricalProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = centralCylindricalProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(-80)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)

    projectionBuilder
  }

  val ceaProjectionName: String = "cea"

  val ceaProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = ceaProjectionName)

  val collignonProjectionName: String = "collg"

  val collignonProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = collignonProjectionName)

  val crasterProjectionName: String = "crast"

  val crasterProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = crasterProjectionName)

  val denoyerProjectionName: String = "denoy"

  val denoyerProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = denoyerProjectionName)

  val eckert1ProjectionName: String = "eck1"

  val eckert1ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = eckert1ProjectionName)

  val eckert2ProjectionName: String = "eck2"

  val eckert2ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = eckert2ProjectionName)

  val eckert4ProjectionName: String = "eck4"

  val eckert4ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = eckert4ProjectionName)

  val eckert5ProjectionName: String = "eck5"

  val eckert5ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = eckert5ProjectionName)

  val eckert6ProjectionName: String = "eck6"

  val eckert6ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = eckert6ProjectionName)

  val plateCarreeProjectionName: String = "eqc"

  val plateCarreeProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = plateCarreeProjectionName)

  val equidistantConicProjectionName: String = "eqdc"

  val equidistantConicProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = equidistantConicProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(10)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(70)
    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-90)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(90)

    projectionBuilder
  }

  val eulerProjectionName: String = "euler"

  val eulerProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = eulerProjectionName)
    )

  val faheyProjectionName: String = "fahey"

  val faheyProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = faheyProjectionName)

  val foucautProjectionName: String = "fouc"

  val foucautProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = foucautProjectionName)

  val foucautSinusoidalProjectionName: String = "fouc_s"

  val foucautSinusoidalProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = foucautSinusoidalProjectionName)

  val gallProjectionName: String = "gall"

  val gallProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = gallProjectionName)

  val gnomonicAzimuthalProjectionName: String = "gnom"

  val gnomonicAzimuthalProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = gnomonicAzimuthalProjectionName)

    projectionBuilder = setAzimuthalProjectionProperties(
      projectionBuilder,
      math.toRadians(90),
      math.toRadians(0)
    )

    projectionBuilder = projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder = projectionBuilder |-> maxLatitudeLens set math.toRadians(90)

    projectionBuilder
  }

  val goodeProjectionName: String = "goode"

  val goodeProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = goodeProjectionName)

  val hammerProjectionName: String = "hammer"

  val hammerProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = hammerProjectionName)

  val hatanoProjectionName: String = "hatano"

  val hatanoProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = hatanoProjectionName)

  val kavraiskyVProjectionName: String = "kav5"

  val kavraiskyVProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = kavraiskyVProjectionName)

  val lambertAzimuthalEqualAreaProjectionName: String = "laea"

  val lambertAzimuthalEqualAreaProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = lambertAzimuthalEqualAreaProjectionName)

  val lagrangeProjectionName: String = "lagrng"

  val lagrangeProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = lagrangeProjectionName)

  val larriveeProjectionName: String = "larr"

  val larriveeProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = larriveeProjectionName)

  val laskowskiProjectionName: String = "lask"

  val laskowskiProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = laskowskiProjectionName)

  val latLongProjectionName: String = "latlong"

  val latLongProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = latLongProjectionName)

  val longLatProjectionName: String = "longlat"

  val longLatProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = longLatProjectionName)

  val lambertConformalConicProjectionName: String = "lcc"

  val lambertConformalConicProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = lambertConformalConicProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)
    projectionBuilder =
      projectionBuilder |-> projectionLatitudeLens set QUARTER_PI
    projectionBuilder =
      projectionBuilder |-> projectionLatitude1Lens set 0
    projectionBuilder =
      projectionBuilder |-> projectionLatitude2Lens set 0

    projectionBuilder
  }

  val lambertEqualAreaConicProjectionName: String = "leac"

  val lambertEqualAreaConicProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = lambertEqualAreaConicProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(90)
    projectionBuilder =
      projectionBuilder |-> projectionLatitude1Lens set QUARTER_PI
    projectionBuilder =
      projectionBuilder |-> projectionLatitude2Lens set math.Pi / 2

    projectionBuilder
  }

  val loximuthalProjectionName: String = "loxim"

  val loximuthalProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = loximuthalProjectionName)

  val landsatProjectionName: String = "lsat"

  val landsatProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = landsatProjectionName)

  val mcBrydeThomasFlatPolarSine2ProjectionName: String = "mbt_fps"

  val mcBrydeThomasFlatPolarSine2ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = mcBrydeThomasFlatPolarSine2ProjectionName)

  val mcBrydeThomasFlatPolarParabolicProjectionName: String = "mbtfpp"

  val mcBrydeThomasFlatPolarParabolicProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = mcBrydeThomasFlatPolarParabolicProjectionName)

  val mcBrydeThomasFlatPolarQuarticProjectionName: String = "mbtfpq"

  val mcBrydeThomasFlatPolarQuarticProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = mcBrydeThomasFlatPolarQuarticProjectionName)

  val mercatorProjectionName: String = "merc"

  val mercatorProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = mercatorProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(-85)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(85)

    projectionBuilder
  }

  val millerProjectionName: String = "mill"

  val millerProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = millerProjectionName)

  val molleweideProjectionName: String = "moll"

  val molleweideProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = molleweideProjectionName)

  val murdoch1ProjectionName: String = "murd1"

  val murdoch1ProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = murdoch1ProjectionName)
    )

  val murdoch2ProjectionName: String = "murd2"

  val murdoch2ProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = murdoch2ProjectionName)
    )

  val murdoch3ProjectionName: String = "murd3"

  val murdoch3ProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = murdoch3ProjectionName)
    )

  val nellProjectionName: String = "nell"

  val nellProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = nellProjectionName)

  val nicolosiProjectionName: String = "nicol"

  val nicolosiProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = nicolosiProjectionName)

  val perspectiveProjectionName: String = "nsper"

  val perspectiveProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = perspectiveProjectionName)

  val obliqueMercatorProjectionName: String = "omerc"

  val obliqueMercatorProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = obliqueMercatorProjectionName)

    projectionBuilder =
      projectionBuilder |-> ellipsoidLens set Ellipsoid.WGS84
    projectionBuilder =
      projectionBuilder |-> projectionLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> projectionLongitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-60)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(60)
    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(-80)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)
    projectionBuilder =
      projectionBuilder |-> alphaLens set math.toRadians(-45)

    projectionBuilder
  }

  val ortographicAzimuthalProjectionName: String = "ortho"

  val equidistanceAzimuthalProjectionBuilder: ProjectionBuilder =
    setAzimuthalProjectionProperties(
      ProjectionBuilder(name = ortographicAzimuthalProjectionName)
    )

  val perspectiveConicProjectionName: String = "pconic"

  val perspectiveConicProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = perspectiveConicProjectionName)
    )

  val polyconicProjectionName: String = "poly"

  val polyconicProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = polyconicProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-60)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(60)
    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)

    projectionBuilder
  }

  val putninsP2ProjectionName: String = "putp2"

  val putninsP2ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = putninsP2ProjectionName)

  val putninsP4PProjectionName: String = "putp4p"

  val putninsP4PProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = putninsP4PProjectionName)

  val putninsP5ProjectionName: String = "putp5"

  val putninsP5ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = putninsP5ProjectionName)

  val putninsP5PProjectionName: String = "putp5p"

  val putninsP5PProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = putninsP5PProjectionName)

  val quarticAuthalicProjectionName: String = "qua_aut"

  val quarticAuthalicProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = quarticAuthalicProjectionName)

  val robinsonProjectionName: String = "robin"

  val robinsonProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = robinsonProjectionName)

  val rectangularPolyconicProjectionName: String = "rpoly"

  val rectangularPolyconicProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = rectangularPolyconicProjectionName)

  val sinusoidalProjectionName: String = "sinu"

  val sinusoidalProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = sinusoidalProjectionName)

  val swissObliqueMercatorProjectionName: String = "somerc"

  val swissObliqueMercatorProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = swissObliqueMercatorProjectionName)

  val stereographicAzimuthalProjectionName: String = "stere"

  val stereographicAzimuthalProjectionBuilder: ProjectionBuilder =
    setAzimuthalProjectionProperties(
      ProjectionBuilder(name = stereographicAzimuthalProjectionName),
      math.toRadians(90),
      math.toRadians(0)
    )

  val obliqueStereographicAlternativeProjectionName: String = "sterea"

  val obliqueStereographicAlternativeProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = obliqueStereographicAlternativeProjectionName)

  val transverseCentralCylindricalProjectionName: String = "tcc"

  val transverseCentralCylindricalProjectionBuilder: ProjectionBuilder = {
    var projectionBuilder =
      ProjectionBuilder(name = transverseCentralCylindricalProjectionName)

    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-60)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(60)

    projectionBuilder
  }

  val transverseCylindricalEqualAreaProjectionName: String = "tcea"

  val transverseCylindricalEqualAreaProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = transverseCylindricalEqualAreaProjectionName)

  val tmercProjectionName: String = "tmerc"

  val tmercProjectionBuilder: ProjectionBuilder =
    transverseMercatorProjectionBuilder(tmercProjectionName)

  val urmaevFlatPolarSinusoidalProjectionName: String = "urmfps"

  val urmaevFlatPolarSinusoidalProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = urmaevFlatPolarSinusoidalProjectionName)

  val utmProjectionName: String = "utm"

  val utmProjectionBuilder: ProjectionBuilder =
    transverseMercatorProjectionBuilder(utmProjectionName)

  val vanDerGrintenProjectionName: String = "vang"

  val vanDerGrintenProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = vanDerGrintenProjectionName)

  val vitkovskyProjectionName: String = "vitk1"

  val vitkovskyProjectionBuilder: ProjectionBuilder =
    setSimpleConicProjectionProperties(
      ProjectionBuilder(name = vitkovskyProjectionName)
    )

  val wagner1ProjectionName: String = "wag1"

  val wagner1ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner1ProjectionName)

  val wagner2ProjectionName: String = "wag2"

  val wagner2ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner2ProjectionName)

  val wagner3ProjectionName: String = "wag3"

  val wagner3ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner3ProjectionName)

  val wagner4ProjectionName: String = "wag4"

  val wagner4ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner4ProjectionName)

  val wagner5ProjectionName: String = "wag5"

  val wagner5ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner5ProjectionName)

  val wagner7ProjectionName: String = "wag7"

  val wagner7ProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = wagner7ProjectionName)

  val werenskioldProjectionName: String = "weren"

  val werenskioldProjectionBuilder: ProjectionBuilder =
    ProjectionBuilder(name = werenskioldProjectionName)

  val winkelTripelProjectionName: String = "wintri"

  val winkelTripelProjectionBuilder: ProjectionBuilder =
    aitoffProjectionBuilder |-> nameLens set winkelTripelProjectionName

  private def setAzimuthalProjectionProperties(
    pb: ProjectionBuilder): ProjectionBuilder =
    setAzimuthalProjectionProperties(pb, math.toRadians(45), math.toRadians(45))

  private def setAzimuthalProjectionProperties(
    pb: ProjectionBuilder,
    projectionLatitude: Double,
    projectionLongitude: Double): ProjectionBuilder = {
    var projectionBuilder = pb

    projectionBuilder =
      projectionBuilder |-> projectionLatitudeLens set projectionLatitude
    projectionBuilder =
      projectionBuilder |-> projectionLongitudeLens set projectionLongitude

    projectionBuilder
  }

  private def setSimpleConicProjectionProperties(
    pb: ProjectionBuilder): ProjectionBuilder = {
    var projectionBuilder = pb
    projectionBuilder =
      projectionBuilder |-> minLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> maxLatitudeLens set math.toRadians(80)

    projectionBuilder
  }

  private def transverseMercatorProjectionBuilder(
    projectionName: String): ProjectionBuilder = {
    var projectionBuilder = ProjectionBuilder(name = projectionName)

    projectionBuilder =
      projectionBuilder |-> ellipsoidLens set Ellipsoid.GRS80
    projectionBuilder =
      projectionBuilder |-> projectionLatitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> projectionLongitudeLens set math.toRadians(0)
    projectionBuilder =
      projectionBuilder |-> minLongitudeLens set math.toRadians(-90)
    projectionBuilder =
      projectionBuilder |-> maxLongitudeLens set math.toRadians(90)

    projectionBuilder
  }

}
