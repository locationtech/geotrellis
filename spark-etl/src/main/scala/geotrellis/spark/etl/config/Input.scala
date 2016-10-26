package geotrellis.spark.etl.config

import geotrellis.proj4.CRS
import geotrellis.vector.Extent

import org.apache.spark.storage.StorageLevel

case class Input(
  name: String,
  format: String,
  backend: Backend,
  cache: Option[StorageLevel] = None,
  noData: Option[Double] = None,
  clip: Option[Extent] = None,
  crs: Option[String] = None
) extends Serializable {
  def getCrs = crs.map(CRS.fromName)
}
