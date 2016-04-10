package geotrellis.spark.mask

import geotrellis.raster.mask.TileMaskMethods
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.rdd.RDD

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withTileRDDMaskMethods[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](val self: RDD[(K, V)] with Metadata[M]) extends TileRDDMaskMethods[K, V, M]
}
