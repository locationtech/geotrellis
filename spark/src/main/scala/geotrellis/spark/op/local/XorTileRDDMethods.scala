package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Xor

trait XorTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) =
    self.mapValues { r => Xor(r, i) }

  /** Xor a constant Int value to each cell. */
  def ^(i: Int) = localXor(i)

  /** Xor a constant Int value to each cell. */
  def ^:(i: Int) = localXor(i)

  /** Xor the values of each cell in each raster.  */
  def localXor(other: Self) =
    self.combineValues(other)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD[K]) = localXor(r)

  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[Self]) =
    self.combineValues(others)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[Self]) = localXor(others)
}
