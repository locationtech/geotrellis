package geotrellis.spark.etl.config

import geotrellis.vector.Extent
import org.apache.spark.storage.StorageLevel

case class Input(
  name: String,
  format: String,
  backend: Backend,
  cache: Option[StorageLevel] = None,
  noData: Option[Double] = None,
  clip: Option[Extent] = None
) extends Serializable
