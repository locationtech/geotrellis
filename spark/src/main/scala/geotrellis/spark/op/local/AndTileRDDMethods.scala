package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local.And
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
  def localAnd(other: RDD[(K, Tile)]) =
    self.combineValues(other){ And.apply }

  /** And the values of each cell in each raster. */
  def &(rs: RasterRDD[K]) = localAnd(rs)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others){ And.apply }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[RDD[(K, Tile)]]) =
    localAnd(others)
}
