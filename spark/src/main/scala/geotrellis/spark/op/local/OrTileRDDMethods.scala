package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Or
import org.apache.spark.Partitioner

trait OrTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Or a constant Int value to each cell. */
  def localOr(i: Int) =
    self.mapValues { case r => Or(r, i) }

  /** Or a constant Int value to each cell. */
  def |(i: Int) = localOr(i)

  /** Or a constant Int value to each cell. */
  def |:(i: Int) = localOr(i)

  /** Or the values of each cell in each raster.  */
  def localOr(other: Self): Self = localOr(other, None)
  def localOr(other: Self, partitioner: Option[Partitioner]): Self =
    self.combineValues(other, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(r: Self): Self = localOr(r, None)

  /** Or the values of each cell in each raster.  */
  def localOr(others: Traversable[Self]): Self = localOr(others, None)
  def localOr(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(others: Traversable[Self]): Self = localOr(others, None)
}
