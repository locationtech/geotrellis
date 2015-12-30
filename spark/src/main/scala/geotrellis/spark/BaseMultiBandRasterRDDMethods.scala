package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._

trait BaseMultiBandRasterRDDMethods[K] extends MultiBandRasterRDDMethods[K] {
  def convert(cellType: CellType): MultiBandRasterRDD[K] =
    rdd.mapValues(_.convert(cellType), rdd.metadata.copy(cellType = cellType))
}
