package geotrellis.spark

import geotrellis.raster._
import geotrellis.util.MethodExtensions
import org.apache.spark.rdd._
import scala.reflect.ClassTag


abstract class TileLayerRDDMethods[K: SpatialComponent: ClassTag] extends MethodExtensions[TileLayerRDD[K]] {
  def convert(cellType: CellType) =
    ContextRDD(
      self.mapValues(_.convert(cellType)),
      self.metadata.copy(cellType = cellType))
}
