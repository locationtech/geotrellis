package geotrellis.layers.mapalgebra.focal.hillshade

import geotrellis.tiling.SpatialComponent
import geotrellis.layers.TileLayerCollection

import reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withElevationTileLayerCollectionMethods[K](val self: TileLayerCollection[K])
    (implicit val _sc: SpatialComponent[K]) extends HillshadeTileLayerCollectionMethods[K] with Serializable
}
