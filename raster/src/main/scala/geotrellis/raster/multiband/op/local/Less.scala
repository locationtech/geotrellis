package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Determines if values are less than other values. Sets to 1 if true, else 0.
 */

object Less extends LocalMultiBandComparatorOp {
  def compare(z1: Int, z2: Int): Boolean =
    if (z1 < z2) true else false

  def compare(z1: Double, z2: Double): Boolean =
    if (isNoData(z1)) { false }
    else {
      if (isNoData(z2)) { false }
      else {
        if (z1 < z2) true
        else false
      }
    }
}

trait LessMethods extends MultiBandTileMethods {
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * integer, else 0.
   */
  def localLess(i: Int): MultiBandTile = Less(mTile, i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * integer, else 0.
   */
  def <(i: Int): MultiBandTile = localLess(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * integer, else 0.
   */
  def localLessRightAssociative(i: Int): MultiBandTile = Less(i, mTile)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * integer, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(i: Int): MultiBandTile = localLessRightAssociative(i)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * double, else 0.
   */
  def localLess(d: Double): MultiBandTile = Less(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * double, else 0.
   */
  def localLessRightAssociative(d: Double): MultiBandTile = Less(mTile, d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * double, else 0.
   */
  def <(d: Double): MultiBandTile = localLess(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell value of band in input multiband raster is less than the input
   * double, else 0.
   * 
   * @note Syntax has double '<' due to '<:' operator being reserved in Scala.
   */
  def <<:(d: Double): MultiBandTile = localLessRightAssociative(d)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are less than the next multiband raster, else 0.
   */
  def localLess(m: MultiBandTile): MultiBandTile = Less(mTile, m)
  /**
   * Returns a MultiBandTile with data of TypeBit, where cell values equal 1 if
   * the corresponding cell valued of the multiband raster are less than the next multiband raster, else 0.
   */
  def <(m: MultiBandTile): MultiBandTile = localLess(m)
}
