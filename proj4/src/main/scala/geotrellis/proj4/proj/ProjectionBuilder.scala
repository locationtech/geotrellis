package geotrellis.proj4.proj

import geotrellis.proj4._
import geotrellis.proj4.units.{Unit, Units}
import geotrellis.proj4.datum.Ellipsoid

import org.osgeo.proj4j.proj.Projection

import monocle._

@Lenses("_")
case class ProjectionBuilder(
  alpha: Double = Double.NaN,
  lonc: Double = Double.NaN,
  projectionLatitude: Double = 0.0,
  projectionLongitude: Double = 0.0,
  projectionLatitude1: Double = 0.0,
  projectionLatitude2: Double = 0.0,
  trueScaleLatitude: Double = 0.0,
  falseEasting: Double = 0.0,
  falseNorthing: Double = 0.0,
  scaleFactor: Double = 1.0,
  unit: Unit = Units.METRES,
  fromMetres: Double = 1,
  southernHemisphere: Boolean = false,
  utmZone: Option[Int] = None,
  ellipsoid: Ellipsoid = Ellipsoid.SPHERE,
  projectionType: ProjectionType
) {

  def build: Projection = {
    null
  }

}
