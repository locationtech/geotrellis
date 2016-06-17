package geotrellis.spark.etl.config

case class IngestType(
  format: String,
  input: BackendInputType,
  output: BackendType,
  inputCredentials: Option[String] = None,
  outputCredentials: Option[String] = None
)
