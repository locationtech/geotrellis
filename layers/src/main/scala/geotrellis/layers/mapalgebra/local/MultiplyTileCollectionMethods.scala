package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile
import geotrellis.raster.mapalgebra.local.Multiply
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait MultiplyTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Multiply a constant value from each cell.*/
  def localMultiply(i: Int) =
    self.mapValues { r => Multiply(r, i) }

  /** Multiply a constant value from each cell.*/
  def *(i: Int) = localMultiply(i)

  /** Multiply a constant value from each cell.*/
  def *:(i: Int) = localMultiply(i)

  /** Multiply a double constant value from each cell.*/
  def localMultiply(d: Double) =
    self.mapValues { r => Multiply(r, d) }

  /** Multiply a double constant value from each cell.*/
  def *(d: Double) = localMultiply(d)

  /** Multiply a double constant value from each cell.*/
  def *:(d: Double) = localMultiply(d)

  /** Multiply the values of each cell in each raster. */
  def localMultiply(other: Seq[(K, Tile)]): Seq[(K, Tile)] = {
    self.combineValues(other)(Multiply.apply)
  }

  /** Multiply the values of each cell in each raster. */
  def *(other: Seq[(K, Tile)]): Seq[(K, Tile)] = localMultiply(other)

  /** Multiply the values of each cell in each raster. */
  def localMultiply(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Multiply.apply)

  /** Multiply the values of each cell in each raster. */
  def *(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localMultiply(others)
}
