package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Or
import org.apache.spark.rdd.RDD

trait OrTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int) =
    self.mapValues { case r => Or(r, i) }

  /** Or a constant Int value to each cell. */
  def |(i: Int) = localOr(i)

  /** Or a constant Int value to each cell. */
  def |:(i: Int) = localOr(i)

  /** Or the values of each cell in each raster.  */
  def localOr(other: RDD[(K, Tile)]) =
    self.combineValues(other)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(r: RDD[(K, Tile)]) = localOr(r)

  /** Or the values of each cell in each raster.  */
  def localOr(others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(others: Traversable[RDD[(K, Tile)]]) = localOr(others)
}
