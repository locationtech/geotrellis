package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */

object Equal extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 == z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { if (isNoData(z2)) true else false }
    else {
      if (isNoData(z2)) { false }
      else {
        if (z1 == z2) true
        else false
      }
    }
}

trait EqualMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * integer, else 0.
   */
  def localEqual(i: Int): MultiBandTile = Equal(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the input
   * double, else 0.
   */
  def localEqual(d: Double): MultiBandTile = Equal(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is equal to the 
   * corresponding cell value of band in other input multiband raster, else 0.
   */
  def localEqual(m: MultiBandTile): MultiBandTile = Equal(mTile, m)
}