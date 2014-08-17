package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Subtracts values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */

object Subtract extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 - z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 - z2
}

trait SubtractMethods extends MultiBandTileMethods {
  /** Subtract a constant value from each cell of each band in a multiband raster.*/
  def localSubtract(i: Int): MultiBandTile = Subtract(mTile, i)
  /** Subtract a constant value from each cell of each band in a multiband raster.*/
  def -(i: Int): MultiBandTile = localSubtract(i)
  /** Subtract each value of a cell of each band in a multiband raster from a constant value. */
  def localSubtractFrom(i: Int): MultiBandTile = Subtract(i, mTile)
  /** Subtract each value of a cell of each band in a multiband raster from a constant value. */
  def -:(i: Int): MultiBandTile = localSubtract(i)
  /** Subtract a double constant value from each cell of each band in a multiband raster.*/
  def localSubtract(d: Double): MultiBandTile = Subtract(mTile, d)
  /** Subtract a double constant value from each cell of each band in a multiband raster. */
  def -(d: Double): MultiBandTile = localSubtract(d)
  /** Subtract each value of a cell of each band in a multiband raster from a double constant value. */
  def localSubtractFrom(d: Double): MultiBandTile = Subtract(d, mTile)
  /** Subtract each value of a cell of each band in a multiband raster from a double constant value. */
  def -:(d: Double): MultiBandTile = localSubtractFrom(d)
  /** Subtract left band from right bands value of a multiband raster. */
  def localSubtract(): Tile = Subtract(mTile)
  /** Subtract left band from right bands value of given range of multiband raster. */
  def localSubtract(f: Int, l: Int): Tile = Subtract(mTile, f, l)
  /** Subtract the values of each cell in each multiband raster. */
  def localSubtract(m: MultiBandTile): MultiBandTile = Subtract(mTile, m)
  /** Subtract the values of each cell in each multiband raster. */
  def -(m: MultiBandTile): MultiBandTile = localSubtract(m)
}
