package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are greater than or equal to other values. Sets to 1 if true, else 0.
 */

object GreaterOrEqual extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 >= z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { false }
    else {
      if (isNoData(z2)) { false }
      else {
        if (z1 >= z2) true
        else false
      }
    }
}

trait GreaterOrEqualMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * integer, else 0.
   */
  def localGreaterOrEqual(i: Int): MultiBandTile = GreaterOrEqual(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * integer, else 0.
   */
  def localGreaterOrEqualRightAssociative(i: Int): MultiBandTile = GreaterOrEqual(i, mTile)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
    * integer, else 0.
   */
  def >=(i: Int): MultiBandTile = localGreaterOrEqual(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * integer, else 0.
   */
  def >=:(i: Int): MultiBandTile = localGreaterOrEqualRightAssociative(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * double, else 0.
   */
  def localGreaterOrEqual(d: Double): MultiBandTile = GreaterOrEqual(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * double, else 0.
   */
  def localGreaterOrEqualRightAssociative(d: Double): MultiBandTile = GreaterOrEqual(d, mTile)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * double, else 0.
   */
  def >=(d: Double): MultiBandTile = localGreaterOrEqual(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than or equal to the input
   * double, else 0.
   */
  def >=:(d: Double): MultiBandTile = localGreaterOrEqualRightAssociative(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are greater than or equal to the next multiband raster, else 0.
   */
  def localGreaterOrEqual(m: MultiBandTile): MultiBandTile = GreaterOrEqual(mTile, m)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are greater than or equal to the next multiband raster, else 0.
   */
  def >=(m: MultiBandTile): MultiBandTile = localGreaterOrEqual(m)
}
