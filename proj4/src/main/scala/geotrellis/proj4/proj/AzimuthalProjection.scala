package geotrellis.proj4.proj

import geotrellis.proj4.util.ProjectionMath

object AzimuthalProjection {

  lazy val NorthPole = 1
  lazy val SouthPole = 2
  lazy val Equator = 3
  lazy val Oblique = 4

}

abstract class AzimuthalProjection(
  pb: ProjectionBuilder,
  val mapRadius: Double = 90.0) extends Projection {

  import AzimuthalProjection._

  val projectionLatitude = math.toRadians(45.0)

  val projectionLongitude = math.toRadians(45.0)

  val (mode, sinPhi, cosPhi) =
    if (math.abs(math.abs(projectionLatitude) - math.Pi / 2) < Eps10)
      (if (projectionLatitude < 0) SouthPole else NorthPole, 0.0, 0.0)
    else if (math.abs(projectionLatitude) > Eps10)
      (Oblique, math.sin(projectionLatitude), math.cos(projectionLatitude))
    else (Equator, 0.0, 0.0)

  def inside(longitude: Double, latitude: Double): Boolean =
    ProjectionMath.greatCircleDistance(
      math.toRadians(longitude),
      math.toRadians(latitude),
      projectionLongitude,
      projectionLatitude
    ) < math.toRadians(mapRadius)

}
