package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are less than or equal to other values. Sets to 1 if true, else 0.
 */

object LessOrEqual extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 <= z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { false }
    else {
      if (isNoData(z2)) { false }
      else {
        if (z1 <= z2) true
        else false
      }
    }
}

trait LessOrEqualMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value band in input multiband input raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqual(i: Int): MultiBandTile = LessOrEqual(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * integer, else 0.
   */
  def localLessOrEqualRightAssociative(i: Int): MultiBandTile = LessOrEqual(i, mTile)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
    * integer, else 0.
   */
  def <=(i: Int): MultiBandTile = localLessOrEqual(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * integer, else 0.
   */
  def <=:(i: Int): MultiBandTile = localLessOrEqualRightAssociative(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqual(d: Double): MultiBandTile = LessOrEqual(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * double, else 0.
   */
  def localLessOrEqualRightAssociative(d: Double): MultiBandTile = LessOrEqual(d, mTile)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * double, else 0.
   */
  def <=(d: Double): MultiBandTile = localLessOrEqual(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than or equal to the input
   * double, else 0.
   */
  def <=:(d: Double): MultiBandTile = localLessOrEqualRightAssociative(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are less than or equal to the next multiband raster, else 0.
   */
  def localLessOrEqual(m: MultiBandTile): MultiBandTile = LessOrEqual(mTile, m)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are less than or equal to the next multiband raster, else 0.
   */
  def <=(m: MultiBandTile): MultiBandTile = localLessOrEqual(m)
}
