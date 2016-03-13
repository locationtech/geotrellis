package geotrellis.proj4

object LatLng extends CRS {
  lazy val proj4jCrs = factory.createFromName("EPSG:4326")

  def epsgCode: Option[Int] = CRS.getEPSGCode(toProj4String + " <>")
}
