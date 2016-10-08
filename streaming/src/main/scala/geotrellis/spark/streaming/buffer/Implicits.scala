package geotrellis.spark.streaming.buffer

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.crop._
import geotrellis.spark._

import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withBufferTilesDStreamMethodsWrapper[
    K: SpatialComponent: ClassTag,
    V <: CellGrid: Stitcher: ClassTag: (? => CropMethods[V])
  ](self: DStream[(K, V)]) extends BufferTilesMethods[K, V](self)
}