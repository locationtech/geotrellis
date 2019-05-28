package geotrellis.layers.mapalgebra.focal

import geotrellis.tiling.SpatialComponent
import geotrellis.layers.TileLayerCollection


object Implicits extends Implicits

trait Implicits {
  implicit class withFocalTileLayerCollectionMethods[K](val self: TileLayerCollection[K])
    (implicit val _sc: SpatialComponent[K]) extends FocalTileLayerCollectionMethods[K]
}
