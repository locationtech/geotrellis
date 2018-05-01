package geotrellis.spark.io.hadoop.conf

case class AttributeCachingConfig(
  expirationMinutes: Int = 60,
  maxSize: Int = 1000,
  enabled: Boolean = true
)

case class AttributeConfig(caching: AttributeCachingConfig = AttributeCachingConfig())

object AttributeConfig extends CamelCaseConfig {
  lazy val conf: AttributeConfig = pureconfig.loadConfigOrThrow[AttributeConfig]("geotrellis.attribute")
  implicit def attributeConfigToClass(obj: AttributeConfig.type): AttributeConfig = conf
}
