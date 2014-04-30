package geotrellis.feature.srs

class WebMercator extends SRS {
  val name = "Spherical Mercator EPSG:900913"

  override def toEllipsoidal(x: Double, y: Double): (Double, Double) = ???

  override def toCartesian(x: Double, y: Double): (Double, Double) = ???
}
