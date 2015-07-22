package geotrellis.proj4

object WebMercator extends CRS { 
  lazy val crs = factory.createFromName("EPSG:3857")
  override val epsgCode: Option[String] =  CRS.getEPSGCode(toProj4String+" <>")
}
