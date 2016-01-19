package geotrellis.spark.op.local

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.op._
import geotrellis.raster.op.local.Unequal

trait UnequalTileRDDMethods[K] extends TileRDDMethods[K] {
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int) =
    self.mapValues { r => Unequal(r, i) }

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * integer, else 0.
   */
  def !==:(i: Int) = localUnequal(i)

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double) =
    self.mapValues { r => Unequal(r, d) }

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is equal to the input
   * double, else 0.
   */
  def !==:(d: Double) = localUnequal(d)

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the rasters are not equal, else 0.
   */
  def localUnequal(other: Self) =
    self.combineValues(other)(Unequal.apply)

  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the raster are not equal, else 0.
   */
  def !==(other: Self) = localUnequal(other)
}
