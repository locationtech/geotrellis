package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Pow
import org.apache.spark.Partitioner

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
  def localPow(other: Self): Self = localPow(other, None)
  def localPow(other: Self, partitioner: Option[Partitioner]): Self =
    self.combineValues(other, partitioner)(Pow.apply)

  /** Pow the values of each cell in each raster. */
  def **(other: Self): Self = **(other, None)
  def **(other: Self, partitioner: Option[Partitioner]): Self = localPow(other, partitioner)

  /** Pow the values of each cell in each raster. */
  def localPow(others: Traversable[Self], partitioner: Option[Partitioner]): Self =
    self.combineValues(others, partitioner)(Pow.apply)

  /** Pow the values of each cell in each raster. */
  def **(others: Traversable[Self]): Self = **(others, None)
  def **(others: Traversable[Self], partitioner: Option[Partitioner]): Self = localPow(others, partitioner)
}
