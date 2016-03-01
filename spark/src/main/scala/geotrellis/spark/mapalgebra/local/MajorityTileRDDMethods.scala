package geotrellis.spark.mapalgebra.local

import org.apache.spark.Partitioner

import scala.collection.immutable.Map

import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.Majority

import geotrellis.spark._
import geotrellis.spark.mapalgebra._

import org.apache.spark.rdd.RDD

trait MajorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMajority(others, None)
  def localMajority(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Majority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * most numerous.
    */
  def localMajority(rs: RDD[(K, Tile)]*): RDD[(K, Tile)] =
    localMajority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMajority(n, others, None)
  def localMajority(n: Int, others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner) { tiles => Majority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the
    * nth most numerous.
    */
  def localMajority(n: Int, rs: RDD[(K, Tile)]*): RDD[(K, Tile)] =
    localMajority(n, rs)
}
