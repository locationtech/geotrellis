package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.spark._
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withTileRDDMergeMethods[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](self: RDD[(K, V)])
    extends TileRDDMergeMethods[K, V](self)

  implicit class withRDDLayoutMergeMethods[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V],
    M: (? => LayoutDefinition)
  ](self: RDD[(K, V)] with Metadata[M]) extends RDDLayoutMergeMethods[K, V, M](self)

  implicit class withMergableMethods[T: Mergable](val self: T) extends MethodExtensions[T] {
    def merge(other: T): T =
      implicitly[Mergable[T]].merge(self, other)
  }
}
