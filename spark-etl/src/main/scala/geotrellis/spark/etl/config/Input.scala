package geotrellis.spark.etl.config

import org.apache.spark.storage.StorageLevel

case class Input(
  name: String,
  format: String,
  backend: Backend,
  cache: Option[StorageLevel] = None,
  noData: Option[Double] = None
) extends Serializable {
  def params = getParams(backend.`type`, backend.path)
}
