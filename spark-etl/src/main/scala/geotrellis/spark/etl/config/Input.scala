package geotrellis.spark.etl.config

import org.apache.spark.storage.StorageLevel

import scala.util.matching.Regex

case class Input(
  name: String,
  ingestType: IngestType,
  path: String,
  ingestOptions: IngestOptions,
  cache: Option[StorageLevel] = None
) {
  def inputParams  = getParams(ingestType.input, path)
}
