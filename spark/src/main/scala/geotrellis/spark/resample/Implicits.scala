package geotrellis.spark.resample

import geotrellis.spark._

object Implicits extends Implicits

trait Implicits {
  implicit class withZoomResampleMethods[K: SpatialComponent](self: TileLayerRDD[K]) extends ZoomResampleMethods[K](self)
}
