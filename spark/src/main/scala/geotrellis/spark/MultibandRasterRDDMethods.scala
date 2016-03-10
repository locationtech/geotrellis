package geotrellis.spark

import geotrellis.raster._
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd._
import scala.reflect.ClassTag


abstract class MultibandRasterRDDMethods[K: ClassTag] extends MethodExtensions[MultibandRasterRDD[K]] {
  def convert(cellType: CellType): MultibandRasterRDD[K] =
    ContextRDD(
      self.mapValues(_.convert(cellType)),
      self.metadata.copy(cellType = cellType))
}
