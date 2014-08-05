package geotrellis.proj4

object WebMercator extends CRS { 
  lazy val crs = factory.createFromName("EPSG:3857") 

  val toLLConstant = (57.295779513082323 / 6378137.0)

  override def alternateTransform(other: CRS): Option[(Double, Double) => (Double, Double)] =
    other match {
      case LatLng => Some(toLatLng)
      case _ => None
    }

  def toLatLng(x: Double, y: Double): (Double, Double) = {
    val x2 = x * toLLConstant
    val x3 = math.floor(((x2 + 180.0) / 360.0)) * 360.0
    val llX = x2 - x3

    val y2 = 1.5707963267948966 - (2.0 * math.atan(math.exp((-1.0 * y) / 6378137.0)))
    val llY = y2 * 57.295779513082323

    (llX, llY)
  }
}
