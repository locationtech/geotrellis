package geotrellis

package object proj4 {
  type Transform = (Double, Double) => (Double, Double)
  type ProjParams = Map[String, Option[String]]

  // TODO: Remove after port
  implicit def proj4jToPort(coord: org.osgeo.proj4j.ProjCoordinate): ProjCoordinate =
    ProjCoordinate(coord.x, coord.y, coord.z)

  implicit def portToproj4j(coord: ProjCoordinate): org.osgeo.proj4j.ProjCoordinate =
    new org.osgeo.proj4j.ProjCoordinate(coord.x, coord.y, coord.z)
}
