package geotrellis.spark

import geotrellis.raster._
import org.apache.spark.rdd._
import scala.reflect.ClassTag

abstract class BaseRasterRDDMethods[K: ClassTag] extends MethodExtensions[RasterRDD[K]] {
  def convert(cellType: CellType) =
    ContextRDD(
      self.mapValues(_.convert(cellType)),
      self.metadata.copy(cellType = cellType))

  def asRasters()(implicit sc: SpatialComponent[K]): RDD[(K, Raster[Tile])] =
    self.mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, self.metadata.mapTransform(key)))
      }
    }, true)
}
