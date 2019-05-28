package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile
import geotrellis.raster.mapalgebra.local.Xor
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait XorTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) =
    self.mapValues { r => Xor(r, i) }

  /** Xor a constant Int value to each cell. */
  def ^(i: Int) = localXor(i)

  /** Xor a constant Int value to each cell. */
  def ^:(i: Int) = localXor(i)

  /** Xor the values of each cell in each raster.  */
  def localXor(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: TileLayerCollection[K]): Seq[(K, Tile)] = localXor(r)

  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localXor(others)
}
