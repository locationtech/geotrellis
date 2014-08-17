package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */

object Unequal extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 == z2) false else true

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { if (isNoData(z2)) false else true }
    else {
      if (isNoData(z2)) { true }
      else {
        if (z1 == z2) false
        else true
      }
    }
}

trait UnequalMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * integer, else 0.
   */
  def localUnequal(i: Int): MultiBandTile = Unequal(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * integer, else 0.
   */
  def !==(i: Int): MultiBandTile = localUnequal(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * integer, else 0.
   */
  def !==:(i: Int): MultiBandTile = localUnequal(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * intenger, else 0.
   */
  def localUnequal(d: Double): MultiBandTile = Unequal(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * double, else 0.
   */
  def !==(d: Double): MultiBandTile = localUnequal(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * double, else 0.
   */
  def !==:(d: Double): MultiBandTile = localUnequal(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband rasters are not equal, else 0.
   */
  def localUnequal(m: MultiBandTile): MultiBandTile = Unequal(mTile, m)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband rasters are not equal, else 0.
   */
  def !==(m: MultiBandTile): MultiBandTile = localUnequal(m)
}
