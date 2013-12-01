package geotrellis

import com.typesafe.config.{ConfigFactory,Config}

case class GeoTrellisConfig(catalogPath:Option[String])

object GeoTrellisConfig {
  def apply():GeoTrellisConfig = apply(ConfigFactory.load())

  def apply(config:Config):GeoTrellisConfig = {
    val catalogPath = if(config.hasPath("geotrellis.catalog")) {
      Some(config.getString("geotrellis.catalog"))
    } else { None }
    GeoTrellisConfig(catalogPath)
  }

  def apply(catalogPath:String):GeoTrellisConfig =
    GeoTrellisConfig(Some(catalogPath))
}
