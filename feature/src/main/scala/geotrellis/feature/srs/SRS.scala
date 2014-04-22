package geotrellis.feature.srs

case class SRS(name: String)

class LatLng extends SRS("WGS84 Datum EPSG:4326")
class WebMercator extends SRS("Spherical Mercator EPSG:900913")

object SRS {
  val originShift = 2 * math.Pi * 6378137 / 2.0
}

