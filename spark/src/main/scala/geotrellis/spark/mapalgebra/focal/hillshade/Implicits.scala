package geotrellis.spark.mapalgebra.focal.hillshade

import geotrellis.spark._

import reflect.ClassTag

object Implicits extends Implicits

trait Implicits  {
  implicit class withElevationTileLayerRDDMethods[K](val self: TileLayerRDD[K])
    (implicit val keyClassTag: ClassTag[K], implicit val _sc: SpatialComponent[K])
      extends HillshadeTileLayerRDDMethods[K] with Serializable
}
