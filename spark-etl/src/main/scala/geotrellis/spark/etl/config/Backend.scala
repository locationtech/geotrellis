package geotrellis.spark.etl.config

case class Backend(`type`: BackendType, path: String, profile: Option[String] = None)
