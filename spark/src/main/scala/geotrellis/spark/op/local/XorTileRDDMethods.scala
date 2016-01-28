package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Xor
import org.apache.spark.Partitioner

trait XorTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) =
    self.mapValues { r => Xor(r, i) }

  /** Xor a constant Int value to each cell. */
  def ^(i: Int) = localXor(i)

  /** Xor a constant Int value to each cell. */
  def ^:(i: Int) = localXor(i)

  /** Xor the values of each cell in each raster.  */
  def localXor(other: Self): Self = localXor(other, None)
  def localXor(other: Self, partitioner: Option[Partitioner]): Self =
    self.combineValues(other, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD[K]): Self = localXor(r, None)
  
  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[Self]): Self = localXor(others, None)
  def localXor(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[Self]): Self = localXor(others, None)
}
