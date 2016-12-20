package geotrellis.spark.streaming.filter

import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.streaming.dstream.DStream

object Implicits extends Implicits

trait Implicits {
  implicit class withTileLayerRDDFilterMethods[
    K: Boundable,
    V,
    M: Component[?, Bounds[K]]
  ](val self: DStream[(K, V)] with Metadata[M])
    extends TileLayerDStreamFilterMethods[K, V, M]

  implicit class withSpaceTimeToSpatialMethods[
    K: SpatialComponent: TemporalComponent,
    V,
    M: Component[?, Bounds[K]]
  ](val self: DStream[(K, V)] with Metadata[M])
    extends SpaceTimeToSpatialMethods[K, V, M]
}

