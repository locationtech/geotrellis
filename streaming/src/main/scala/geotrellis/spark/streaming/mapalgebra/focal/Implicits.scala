package geotrellis.spark.streaming.mapalgebra.focal

import geotrellis.spark._
import geotrellis.spark.streaming._

import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withFocalTileDStreamMethods[K](val self: TileLayerDStream[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K]) extends FocalTileLayerDStreamMethods[K]
}
