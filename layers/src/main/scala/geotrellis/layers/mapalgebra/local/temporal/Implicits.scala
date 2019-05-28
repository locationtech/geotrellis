package geotrellis.layers.mapalgebra.local.temporal

import geotrellis.tiling.{SpatialComponent, TemporalComponent}
import geotrellis.raster.Tile
import geotrellis.util.MethodExtensions

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withLocalTemporalTileCollectionMethods[K: SpatialComponent: TemporalComponent](self: Seq[(K, Tile)])
    extends LocalTemporalTileCollectionMethods[K](self)
}
