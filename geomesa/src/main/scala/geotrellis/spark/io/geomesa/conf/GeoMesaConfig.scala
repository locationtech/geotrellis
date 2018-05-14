package geotrellis.spark.io.geomesa.conf

case class GeoMesaConfig(featureTypeCacheSize: Int = 16)

object GeoMesaConfig {
  lazy val conf: GeoMesaConfig = pureconfig.loadConfigOrThrow[GeoMesaConfig]("geotrellis.geomesa")
  implicit def geoMesaConfigToClass(obj: GeoMesaConfig.type): GeoMesaConfig = conf
}