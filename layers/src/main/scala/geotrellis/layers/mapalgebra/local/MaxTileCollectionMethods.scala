package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Max
import geotrellis.raster.{DI, Tile}
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait MaxTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) =
    self.mapValues { r => Max(r, i) }

  /** Max a constant Double value to each cell. */
  def localMax(d: Double) =
    self.mapValues { r => Max(r, d) }

  /** Max the values of each cell in each raster.  */
  def localMax(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Max.apply)

  /** Max the values of each cell in each raster.  */
  def localMax(others: Seq[Seq[(K, Tile)]])(implicit d: DI): Seq[(K, Tile)] =
    self.combineValues(others)(Max.apply)
}
