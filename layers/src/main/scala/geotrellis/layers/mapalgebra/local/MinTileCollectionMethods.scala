package geotrellis.layers.mapalgebra.local

import geotrellis.raster.mapalgebra.local.Min
import geotrellis.raster.{DI, Tile}
import geotrellis.layers._
import geotrellis.util.MethodExtensions

trait MinTileCollectionMethods[K] extends MethodExtensions[Seq[(K, Tile)]] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) =
    self.mapValues { r => Min(r, i) }

  /** Min a constant Double value to each cell. */
  def localMin(d: Double) =
    self.mapValues { r => Min(r, d) }

  /** Min the values of each cell in each raster.  */
  def localMin(other: Seq[(K, Tile)]): Seq[(K, Tile)] =
    self.combineValues(other)(Min.apply)

  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[Seq[(K, Tile)]])(implicit d: DI): Seq[(K, Tile)] =
    self.combineValues(others)(Min.apply)
}
