package geotrellis.spark.mapalgebra.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.mapalgebra._
import geotrellis.raster.mapalgebra.local.Pow
import org.apache.spark.Partitioner
import org.apache.spark.rdd.RDD

trait PowTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Pow each value of the raster by a constant value.*/
  def localPow(i: Int) =
    self.mapValues { r => Pow(r, i) }

  /** Pow each value of the raster by a constant value.*/
  def **(i:Int) = localPow(i)

  /** Pow a constant value by each cell value.*/
  def localPowValue(i: Int) =
    self.mapValues { r => Pow(i, r) }

  /** Pow a constant value by each cell value.*/
  def **:(i:Int) = localPowValue(i)

  /** Pow each value of a raster by a double constant value.*/
  def localPow(d: Double) =
    self.mapValues { r => Pow(r, d) }
  /** Pow each value of a raster by a double constant value.*/
  def **(d:Double) = localPow(d)

  /** Pow a double constant value by each cell value.*/
  def localPowValue(d: Double) =
    self.mapValues { r => Pow(d, r) }

  /** Pow a double constant value by each cell value.*/
  def **:(d: Double) = localPowValue(d)

  /** Pow the values of each cell in each raster. */
  def localPow(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localPow(other, None)
  def localPow(other: RDD[(K, Tile)], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(other, partitioner)(Pow.apply)

  /** Pow the values of each cell in each raster. */
  def **(other: RDD[(K, Tile)]): RDD[(K, Tile)] = localPow(other, None)

  /** Pow the values of each cell in each raster. */
  def localPow(others: Traversable[RDD[(K, Tile)]], partitioner: Option[Partitioner]): RDD[(K, Tile)] =
    self.combineValues(others, partitioner)(Pow.apply)

  /** Pow the values of each cell in each raster. */
  def **(others: Traversable[RDD[(K, Tile)]]): RDD[(K, Tile)] = localPow(others, None)
}
