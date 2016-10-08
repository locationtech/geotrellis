package geotrellis.spark.streaming.mask

import geotrellis.raster.mask.TileMaskMethods
import geotrellis.spark._
import geotrellis.spark.tiling._
import geotrellis.util._

import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

object Implicits extends Implicits

trait Implicits {
  implicit class withTileRDDMaskMethods[
    K: SpatialComponent: ClassTag,
    V: (? => TileMaskMethods[V]),
    M: GetComponent[?, LayoutDefinition]
  ](val self: DStream[(K, V)] with Metadata[M]) extends TileDStreamMaskMethods[K, V, M]
}
