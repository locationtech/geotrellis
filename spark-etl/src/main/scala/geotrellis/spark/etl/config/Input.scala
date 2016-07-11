package geotrellis.spark.etl.config

import org.apache.spark.storage.StorageLevel

case class Input(
  name: String,
  format: String,
  backend: Backend,
  cache: Option[StorageLevel] = None,
  noData: Option[Double] = None
) {
  def params = getParams(backend.`type`, backend.path)
}
