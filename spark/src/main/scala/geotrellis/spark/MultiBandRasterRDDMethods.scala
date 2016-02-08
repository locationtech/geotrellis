package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._
import scala.reflect.ClassTag

abstract class MultiBandRasterRDDMethods[K: ClassTag] extends MethodExtensions[MultiBandRasterRDD[K]] {
  def convert(cellType: CellType): MultiBandRasterRDD[K] =
    ContextRDD(
      self.mapValues(_.convert(cellType)),
      self.metadata.copy(cellType = cellType))
}
