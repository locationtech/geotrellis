package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Max
import org.apache.spark.rdd.RDD

trait MaxTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Max a constant Int value to each cell. */
  def localMax(i: Int) =
    self.mapValues { r => Max(r, i) }

  /** Max a constant Double value to each cell. */
  def localMax(d: Double) =
    self.mapValues { r => Max(r, d) }

  /** Max the values of each cell in each raster.  */
  def localMax(other: RDD[(K, Tile)]) =
    self.combineValues(other)(Max.apply)

  /** Max the values of each cell in each raster.  */
  def localMax(others: Seq[RDD[(K, Tile)]]) =
    self.combineValues(others)(Max.apply)
}
