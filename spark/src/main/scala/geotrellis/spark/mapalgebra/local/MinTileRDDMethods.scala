package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Min
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait MinTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Min a constant Int value to each cell. */
  def localMin(i: Int) =
    self.mapValues { r => Min(r, i) }

  /** Min a constant Double value to each cell. */
  def localMin(d: Double) =
    self.mapValues { r => Min(r, d) }

  /** Min the values of each cell in each raster.  */
  def localMin(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localMin(other, None)
  def localMin(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Min.apply)

  /** Min the values of each cell in each raster.  */
  def localMin(others: Seq[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMin(others, None)
  def localMin(others: Seq[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Min.apply)
}
