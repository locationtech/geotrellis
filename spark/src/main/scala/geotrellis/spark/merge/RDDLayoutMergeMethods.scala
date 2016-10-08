package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

class RDDLayoutMergeMethods[
  K: SpatialComponent: ClassTag,
  V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
  M: (? => LayoutDefinition)
](val self: RDD[(K, V)] with Metadata[M]) extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {

 def merge(other: RDD[(K, V)] with Metadata[M]) =
   RDDLayoutMerge.merge(self, other)
}
