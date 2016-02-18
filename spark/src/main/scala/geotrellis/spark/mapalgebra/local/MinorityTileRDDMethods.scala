package geotrellis.spark.mapalgebra.local

import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster._
import geotrellis.raster.mapalgebra.local.Minority
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait MinorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMinority(others, None)
  def localMinority(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Minority.apply)

  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(rs: RDD[(K, Tile)]*)(implicit d: DI): RDD[(K, Tile)] =
    localMinority(rs)

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localMinority(n, others, None)
  def localMinority(n: Int, others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: RDD[(K, Tile)]*)(implicit d: DI): RDD[(K, Tile)] =
    localMinority(n, rs)
}
