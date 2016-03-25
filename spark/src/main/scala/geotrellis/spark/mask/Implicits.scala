package geotrellis.spark.mask

import geotrellis.spark._
import geotrellis.spark.TileLayerRDD
import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withRDDMaskMethods[K: SpatialComponent: ClassTag](val self: TileLayerRDD[K])
      extends TileLayerRDDMaskMethods[K]
}
