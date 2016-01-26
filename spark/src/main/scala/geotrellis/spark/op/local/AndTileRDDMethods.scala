package geotrellis.spark.op.local

import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster._
import geotrellis.raster.op.local.And
import org.apache.spark.Partitioner

trait AndTileRDDMethods[K] extends TileRDDMethods[K] {
  /** And a constant Int value to each cell. */
  def localAnd(i: Int) =
    self.mapValues { r => And(r, i) }

  /** And a constant Int value to each cell. */
  def &(i: Int) = localAnd(i)

  /** And a constant Int value to each cell. */
  def &:(i: Int) = localAnd(i)

  /** And the values of each cell in each raster.  */
  def localAnd(other: Self): Self = localAnd(other, None)
  def localAnd(other: Self, partitioner: Option[Partitioner]): Self =
    self.combineValues(other, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(rs: RasterRDD[K]): Self = &(rs, None)
  def &(rs: RasterRDD[K], partitioner: Option[Partitioner]): Self = localAnd(rs, partitioner)

  /** And the values of each cell in each raster.  */
  def localAnd(others: Traversable[Self]): Self = localAnd(others, None)
  def localAnd(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner){ And.apply }

  /** And the values of each cell in each raster. */
  def &(others: Traversable[Self]): Self = &(others, None)
  def &(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    localAnd(others, partitioner)
}
