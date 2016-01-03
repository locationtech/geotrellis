package geotrellis.spark.buffer

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.spark._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withBufferTilesMethodsWrapper[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](self: RDD[(K, V)]) extends BufferTilesMethods[K, V](self)
}
