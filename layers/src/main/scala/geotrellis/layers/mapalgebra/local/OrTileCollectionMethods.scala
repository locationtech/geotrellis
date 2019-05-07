package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile
import geotrellis.raster.mapalgebra.local.Or
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait OrTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int) =
    self.mapValues { r => Or(r, i) }

  /** Or a constant Int value to each cell. */
  def |(i: Int) = localOr(i)

  /** Or a constant Int value to each cell. */
  def |:(i: Int) = localOr(i)

  /** Or the values of each cell in each raster.  */
  def localOr(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(r: Seq[(K, Tile)]): Seq[(K, Tile)] = localOr(r)

  /** Or the values of each cell in each raster.  */
  def localOr(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localOr(others)
}
