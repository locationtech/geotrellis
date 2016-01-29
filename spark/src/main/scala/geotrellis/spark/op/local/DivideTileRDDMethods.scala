package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Divide
import org.apache.spark.rdd.RDD

trait DivideTileRDDMethods[K] extends TileRDDMethods[K] {
  /** Divide each value of the raster by a constant value.*/
  def localDivide(i: Int) =
    self.mapValues { r => Divide(r, i) }

  /** Divide each value of the raster by a constant value.*/
  def /(i: Int) = localDivide(i)

  /** Divide a constant value by each cell value.*/
  def localDivideValue(i: Int) =
    self.mapValues { r => Divide(i, r) }

  /** Divide a constant value by each cell value.*/
  def /:(i: Int) = localDivideValue(i)

  /** Divide each value of a raster by a double constant value.*/
  def localDivide(d: Double) =
    self.mapValues { r => Divide(r, d) }

  /** Divide each value of a raster by a double constant value.*/
  def /(d: Double) = localDivide(d)

  /** Divide a double constant value by each cell value.*/
  def localDivideValue(d: Double) =
    self.mapValues { r => Divide(d, r) }

  /** Divide a double constant value by each cell value.*/
  def /:(d: Double) = localDivideValue(d)

  /** Divide the values of each cell in each raster. */
  def localDivide(other: RDD[(K, Tile)]) =
    self.combineValues(other)(Divide.apply)

  /** Divide the values of each cell in each raster. */
  def /(other: RDD[(K, Tile)]) = localDivide(other)

  def localDivide(others: Traversable[RDD[(K, Tile)]]) =
    self.combineValues(others)(Divide.apply)

  def /(others: Traversable[RDD[(K, Tile)]]) =
    localDivide(others)
}
