package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Xor
import org.apache.spark.rdd.RDD

trait XorTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Xor a constant Int value to each cell. */
  def localXor(i: Int) =
    self.mapValues { r => Xor(r, i) }

  /** Xor a constant Int value to each cell. */
  def ^(i: Int) = localXor(i)

  /** Xor a constant Int value to each cell. */
  def ^:(i: Int) = localXor(i)

  /** Xor the values of each cell in each raster.  */
  def localXor(other: RDD[(K, Tile)]) =
    self.combineValues(other)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: RasterRDD[K]) = localXor(r)

  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[RDD[(K, Tile)]]) = localXor(others)
}
