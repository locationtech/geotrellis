package geotrellis.spark.etl.config.dataset

import geotrellis.spark.etl.config.backend._

case class IngestType(
  inputCredentials: Option[String],
  outputCredentials: Option[String],
  input: BackendInputType,
  output: BackendType,
  format: String
)
