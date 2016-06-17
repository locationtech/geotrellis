package geotrellis.spark.etl.config

case class IngestKeyIndexMethod(
  `type`: String,
  timeTag: Option[String] = None,
  timeFormat: Option[String] = None,
  temporalResolution: Option[Int] = None
)
