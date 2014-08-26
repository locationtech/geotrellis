package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are greater than other values. Sets to 1 if true, else 0.
 */

object Greater extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 > z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { false }
    else {
      if (isNoData(z2)) { false }
      else {
        if (z1 > z2) true
        else false
      }
    }
}

trait GreaterMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * integer, else 0.
   */
  def localGreater(i: Int): MultiBandTile = Greater(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * integer, else 0.
   */
  def localGreaterRightAssociative(i: Int): MultiBandTile = Greater(i, mTile)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * integer, else 0.
   */
  def >(i: Int): MultiBandTile = localGreater(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * integer, else 0.
   *
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(i: Int): MultiBandTile = localGreaterRightAssociative(i)
  /**
   * Returns a Tile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of the input raster is greater than the input
   * double, else 0.
   */
  def localGreater(d: Double): MultiBandTile = Greater(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * double, else 0.
   */
  def localGreaterRightAssociative(d: Double): MultiBandTile = Greater(d, mTile)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * double, else 0.
   */
  def >(d: Double): MultiBandTile = localGreater(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is greater than the input
   * double, else 0.
   *
   * @note Syntax has double '>' due to '>:' operator being reserved in Scala.
   */
  def >>:(d: Double): MultiBandTile = localGreaterRightAssociative(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of band in input multiband raster are greater than the next multiband raster, else 0.
   */
  def localGreater(m: MultiBandTile): MultiBandTile = Greater(mTile, m)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of band in input multiband raster are greater than the next raster, else 0.
   */
  def >(m: MultiBandTile): MultiBandTile = localGreater(m)
}
