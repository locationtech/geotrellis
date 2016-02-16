package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Or
import org.apache.spark.Partitioner
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
  def localOr(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localOr(other, None)
  def localOr(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(r: RDD[(K, Tile)]): RDD[(K, Tile)] = localOr(r, None)

  /** Or the values of each cell in each raster.  */
  def localOr(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localOr(others, None)
  def localOr(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Or.apply)

  /** Or the values of each cell in each raster. */
  def |(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localOr(others, None)
}
