package geotrellis.proj4

object LatLng extends CRS { 
  lazy val crs = factory.createFromName("EPSG:4326")

  private val toWMConstant = 6378137.0 * 0.017453292519943295

  override def alternateTransform(other: CRS): Option[(Double, Double) => (Double, Double)] =
    other match {
      case WebMercator => Some(toWebMercator)
      case _ => None
    }

  def toWebMercator(x: Double, y: Double): (Double, Double) = {
    val wmX = toWMConstant * x
    val y2 = y * 0.017453292519943295
    val wmY = 3189068.5 * math.log((1.0 + math.sin(y2)) / (1.0 - math.sin(y2)))
    (wmX, wmY)
  }
}
