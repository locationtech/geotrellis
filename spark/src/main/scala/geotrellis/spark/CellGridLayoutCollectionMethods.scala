package geotrellis.spark

import geotrellis.raster._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import scala.reflect.ClassTag

abstract class CellGridLayoutCollectionMethods[K: SpatialComponent: ClassTag, V <: CellGrid, M: GetComponent[?, LayoutDefinition]]
    extends MethodExtensions[Seq[(K, V)] with Metadata[M]] {
  def asRasters(): Seq[(K, Raster[V])] = {
    val mapTransform = self.metadata.getComponent[LayoutDefinition].mapTransform
    self.map { case (key, tile) =>
        (key, Raster(tile, mapTransform(key)))
    }
  }
}
