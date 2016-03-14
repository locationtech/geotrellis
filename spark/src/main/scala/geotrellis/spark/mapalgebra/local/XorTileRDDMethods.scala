package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Xor
import org.apache.spark.Partitioner
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
  def localXor(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localXor(other, None)
  def localXor(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(r: TileLayerRDD[K]): RDD[(K, Tile)] = localXor(r, None)
  
  /** Xor the values of each cell in each raster. */
  def localXor(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localXor(others, None)
  def localXor(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Xor.apply)

  /** Xor the values of each cell in each raster. */
  def ^(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localXor(others, None)
}
