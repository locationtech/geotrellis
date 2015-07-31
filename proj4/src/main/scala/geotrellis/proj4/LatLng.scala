package geotrellis.proj4

object LatLng extends CRS {
  lazy val crs = factory.createFromName("EPSG:4326")
  val epsgCode: Option[Int] = CRS.getEPSGCode(toProj4String + " <>")
}
