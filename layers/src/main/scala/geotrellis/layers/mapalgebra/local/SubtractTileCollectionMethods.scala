package geotrellis.layers.mapalgebra.local

import geotrellis.raster.Tile
import geotrellis.raster.mapalgebra.local.Subtract
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait SubtractTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Subtract a constant value from each cell.*/
  def localSubtract(i: Int) =
    self.mapValues { r => Subtract(r, i) }

  /** Subtract a constant value from each cell.*/
  def -(i: Int) = localSubtract(i)

  /** Subtract each value of a cell from a constant value. */
  def localSubtractFrom(i: Int) =
    self.mapValues { r => Subtract(i, r) }

  /** Subtract each value of a cell from a constant value. */
  def -:(i: Int) = localSubtractFrom(i)

  /** Subtract a double constant value from each cell.*/
  def localSubtract(d: Double) =
    self.mapValues { r => Subtract(r, d) }

  /** Subtract a double constant value from each cell.*/
  def -(d: Double) = localSubtract(d)

  /** Subtract each value of a cell from a double constant value. */
  def localSubtractFrom(d: Double) =
    self.mapValues { r => Subtract(d, r) }

  /** Subtract each value of a cell from a double constant value. */
  def -:(d: Double) = localSubtractFrom(d)

  /** Subtract the values of each cell in each raster. */
  def localSubtract(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Subtract.apply)

  /** Subtract the values of each cell in each raster. */
  def -(other: Seq[(K, Tile)]): Seq[(K, Tile)] = localSubtract(other)

  /** Subtract the values of each cell in each raster. */
  def localSubtract(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others)(Subtract.apply)

  /** Subtract the values of each cell in each raster. */
  def -(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localSubtract(others)
}
