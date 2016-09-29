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
    K: SpatialComponent: TemporalComponent,
    V,
    M[_]
  ](val self: RDD[(K, V)] with Metadata[M[K]])(
    implicit comp: Component[M[K], Bounds[K]],
         mFunctor: M[K] => Functor[M, K]
  ) extends SpaceTimeToSpatialMethods[K, V, M]
}
