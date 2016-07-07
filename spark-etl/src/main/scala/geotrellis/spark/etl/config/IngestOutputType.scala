package geotrellis.spark.etl.config

case class IngestOutputType(
  output: BackendType,
  credentials: Option[String] = None
)
