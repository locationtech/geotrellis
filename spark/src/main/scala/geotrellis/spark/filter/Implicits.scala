package geotrellis.spark.filter

import geotrellis.spark._

import org.apache.spark.rdd._

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withTileLayerRDDFilterMethods[K: Boundable : ClassTag, V: ClassTag, M](val self: RDD[(K, V)] with Metadata[M])
      extends TileLayerRDDFilterMethods[K, V, M]

  implicit class withSpaceTimeToSpatialMethods[K : SpatialComponent : TemporalComponent : ClassTag, V : ClassTag, M](val self: RDD[(K, V)] with Metadata[M])
      extends SpaceTimeToSpatialMethods[K, V, M]
}
