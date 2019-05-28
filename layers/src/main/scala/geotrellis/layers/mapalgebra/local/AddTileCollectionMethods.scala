package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Add
import geotrellis.raster.Tile
import geotrellis.layers._
import geotrellis.util.MethodExtensions


trait AddTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Add a constant Int value to each cell. */
  def localAdd(i: Int) =
    self.mapValues { r => Add(r, i) }

  /** Add a constant Int value to each cell. */
  def +(i: Int) = localAdd(i)

  /** Add a constant Int value to each cell. */
  def +:(i: Int) = localAdd(i)

  /** Add a constant Double value to each cell. */
  def localAdd(d: Double) =
    self.mapValues { r => Add(r, d) }

  /** Add a constant Double value to each cell. */
  def +(d: Double) = localAdd(d)

  /** Add a constant Double value to each cell. */
  def +:(d: Double) = localAdd(d)

  /** Add the values of each cell in each raster.  */
  def localAdd(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other) { Add.apply }

  /** Add the values of each cell in each raster. */
  def +(other: Seq[(K, Tile)]): Seq[(K, Tile)] = localAdd(other)

  def localAdd(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] =
    self.combineValues(others) { Add.apply }

  def +(others: Traversable[Seq[(K, Tile)]]): Seq[(K, Tile)] = localAdd(others)
}
