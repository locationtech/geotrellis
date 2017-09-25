package geotrellis.spark.regrid

import geotrellis.raster._
import geotrellis.raster.crop._
import geotrellis.raster.prototype._
import geotrellis.raster.stitch._
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withRegridMethods[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: ClassTag: Stitcher: (? => CropMethods[V]): (? => TilePrototypeMethods[V]),
    M: Component[?, LayoutDefinition]: Component[?, Bounds[K]]
  ](self: RDD[(K, V)] with Metadata[M]) extends RegridMethods[K, V, M](self)
}
