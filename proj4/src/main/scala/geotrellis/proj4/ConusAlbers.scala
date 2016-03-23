package geotrellis.proj4

object ConusAlbers extends CRS {
  lazy val proj4jCrs = factory.createFromName("EPSG:5070")

  def epsgCode: Option[Int] = Some(5070)
}
