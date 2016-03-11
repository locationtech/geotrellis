package geotrellis.spark.mapalgebra.focal

import geotrellis.spark._

import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withFocalTileLayerRDDMethods[K](val self: TileLayerRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends FocalTileLayerRDDMethods[K]
}
