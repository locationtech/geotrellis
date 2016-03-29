package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

abstract class CellGridLayoutRDDMethods[K: SpatialComponent: ClassTag, V <: CellGrid, M: GetComponent[?, LayoutDefinition]]
    extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def asRasters(): RDD[(K, Raster[V])] = {
    val mapTransform = self.metadata.getComponent[LayoutDefinition].mapTransform
    self.mapPartitions({ part =>
      part.map { case (key, tile) =>
        (key, Raster(tile, mapTransform(key)))
      }
    }, true)
  }
}
