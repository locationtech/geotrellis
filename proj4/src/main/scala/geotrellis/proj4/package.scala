package geotrellis

package object proj4 {
  type Transform = (Double, Double) => (Double, Double)

  val WebMercator = CRS.fromEpsgCode(3857)
  val LatLng = CRS.fromEpsgCode(4326)
  val ConusAlbers = CRS.fromEpsgCode(5070)

  lazy val Backend = Proj4Backend
}
