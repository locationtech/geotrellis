package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Pows values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */

object Pow extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else math.pow(z1, z2).toInt

  def combine(z1: Double, z2: Double) =
    math.pow(z1, z2)
}

trait PowMethods extends MultiBandTileMethods {
  /** Pow each value of the multiband raster by a constant value.*/
  def localPow(i: Int): MultiBandTile = Pow(mTile, i)
  /** Pow each value of the multiband raster by a constant value.*/
  def **(i: Int): MultiBandTile = localPow(i)
  /** Pow a constant value by each cell value of multiband raster.*/
  def localPowValue(i: Int): MultiBandTile = Pow(i, mTile)
  /** Pow a constant value by each cell value of multiband raster.*/
  def **:(i: Int): MultiBandTile = localPowValue(i)
  /** Pow each value of a multiband raster by a double constant value.*/
  def localPow(d: Double): MultiBandTile = Pow(mTile, d)
  /** Pow each value of a multiband raster by a double constant value.*/
  def **(d: Double): MultiBandTile = localPow(d)
  /** Pow a double constant value by each cell value of multiband raster.*/
  def localPowValue(d: Double): MultiBandTile = Pow(d, mTile)
  /** Pow a double constant value by each cell value of multiband raster.*/
  def **:(d: Double): MultiBandTile = localPowValue(d)
  /** Pow the values of each cell in each multiband raster. */
  def localPow(m: MultiBandTile): MultiBandTile = Pow(mTile, m)
  /** Pow the values of each cell in each multiband raster. */
  def **(m: MultiBandTile): MultiBandTile = localPow(m)
  /** Pow the values of each cell in each band of multiband raster. */
  def localPow(): Tile = Pow(mTile)
  /** Pow the values of each cell in each band in a given raster. */
  def localPow(f: Int, l: Int): Tile = Pow(mTile, f, l)
}
