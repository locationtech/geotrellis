package geotrellis.feature.srs

class LatLng extends SRS {
  val name = "WGS84 Datum EPSG:4326"

  override def toEllipsoidal(x: Double, y: Double): (Double, Double) = ???

  override def toCartesian(x: Double, y: Double): (Double, Double) = ???
}
