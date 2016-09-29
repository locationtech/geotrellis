package geotrellis.spark.filter

import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd._

object Implicits extends Implicits

trait Implicits {
  implicit class withTileLayerRDDFilterMethods[
    K: Boundable,
    V,
    M: Component[?, Bounds[K]]
  ](val self: RDD[(K, V)] with Metadata[M])
      extends TileLayerRDDFilterMethods[K, V, M]

  implicit class withSpaceTimeToSpatialMethods[
    K: SpatialComponent: TemporalComponent: λ[α => M[α] => Functor[M, α]]: λ[α => Component[M[α], Bounds[α]]],
    V,
    M[_]
  ](val self: RDD[(K, V)] with Metadata[M[K]]) extends SpaceTimeToSpatialMethods[K, V, M]
}
