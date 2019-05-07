package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile

import scala.reflect.ClassTag


object Implicits extends Implicits

trait Implicits {
  implicit class withLocalTileCollectionMethods[K](val self: Seq[(K, Tile)]) extends LocalTileCollectionMethods[K]
  implicit class withLocalTileCollectionSeqMethods[K](val self: Traversable[Seq[(K, Tile)]]) extends LocalTileCollectionSeqMethods[K]
}
