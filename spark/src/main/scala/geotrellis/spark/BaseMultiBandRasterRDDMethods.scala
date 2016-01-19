package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._

trait BaseMultibandRasterRDDMethods[K] extends MultibandRasterRDDMethods[K] with Serializable {
  def convert(cellType: CellType): MultibandRasterRDD[K] =
    rdd.mapValues(_.convert(cellType), rdd.metadata.copy(cellType = cellType))
}
