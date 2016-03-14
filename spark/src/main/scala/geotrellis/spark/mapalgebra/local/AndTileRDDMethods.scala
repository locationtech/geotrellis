package geotrellis.spark.mapalgebra.local

import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.And
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait AndTileRDDMethods[K] extends TileRDDMethods[K] {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) =
    self.mapValues { r => And(r, i) }

  /** And a constant Int value to each cell. */
  def &(i: Int) = localAnd(i)

  /** And a constant Int value to each cell. */
  def &:(i: Int) = localAnd(i)

  /** And the values of each cell in each raster.  */
  def localAnd(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localAnd(other, None)
  def localAnd(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(rs: TileLayerRDD[K]): RDD[(K, Tile)] = localAnd(rs, None)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAnd(others, None)
  def localAnd(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localAnd(others, None)
}
