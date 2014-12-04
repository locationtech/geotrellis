package geotrellis.proj4.proj

trait HasCoordinateBounds {

  val minLatitude: Double = math.toRadians(-60)

  val maxLatitiude: Double = math.toRadians(60)

  val minLongitude: Double = math.toRadians(-90)

  val maxLongitude: Double = math.toRadians(90)

}
