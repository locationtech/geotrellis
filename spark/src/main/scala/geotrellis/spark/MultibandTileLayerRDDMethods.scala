package geotrellis.spark

import geotrellis.raster._
import geotrellis.util.MethodExtensions

import org.apache.spark.rdd._

import scala.reflect.ClassTag


abstract class MultibandTileLayerRDDMethods[K: SpatialComponent: ClassTag] extends MethodExtensions[MultibandTileLayerRDD[K]] {
  def convert(cellType: CellType): MultibandTileLayerRDD[K] =
    ContextRDD(
      self.mapValues(_.convert(cellType)),
      self.metadata.copy(cellType = cellType))
}
