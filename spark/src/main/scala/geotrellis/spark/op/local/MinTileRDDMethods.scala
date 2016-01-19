package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Min

trait MinTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) =
    self.mapValues { r => Min(r, i) }

  /** Min a constant Double value to each cell. */
  def localMin(d: Double) =
    self.mapValues { r => Min(r, d) }

  /** Min the values of each cell in each raster.  */
  def localMin(other: Self) =
    self.combineValues(other)(Min.apply)

  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[Self]) =
    self.combineValues(others)(Min.apply)
}
