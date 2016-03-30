package geotrellis.spark.tiling

import geotrellis.spark._
import geotrellis.vector._
import geotrellis.raster._
import geotrellis.raster.merge._
import geotrellis.raster.prototype._
import geotrellis.util._

import org.apache.spark.rdd._

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withTilerMethods[K, V <: CellGrid: ClassTag: ? => TileMergeMethods[V]: ? => TilePrototypeMethods[V]](self: RDD[(K, V)])
      extends TilerMethods[K, V](self)

  implicit class withTupleTilerKeyMethods[K: SpatialComponent](val self: (K, Extent)) extends TilerKeyMethods[(K, Extent), K] {
    def extent = self._2
    def translate(spatialKey: SpatialKey): K = self._1.setComponent(spatialKey)
  }
}
