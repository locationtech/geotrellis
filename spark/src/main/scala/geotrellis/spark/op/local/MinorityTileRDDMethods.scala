package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local.Minority
import org.apache.spark.rdd.RDD

trait MinorityTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
    * Assigns to each cell the value within the given rasters that is the least
    * numerous.
    */
  def localMinority(others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others)(Minority.apply)

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
  def localMinority(n: Int, others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others) { tiles => Minority(n, tiles) }

  /**
    * Assigns to each cell the value within the given rasters that is the nth
    * least numerous.
    */
  def localMinority(n: Int, rs: RDD[(K, Tile)]*)(implicit d: DI): RDD[(K, Tile)] =
    localMinority(n, rs)
}
