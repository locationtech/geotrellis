package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Or's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */

object Or extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 | z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) | d2i(z2))
}

trait OrMethods extends MultiBandTileMethods {
  /** Or a constant Int value to each cell value of each band in a multiband raster. */
  def localOr(i: Int): MultiBandTile = Or(mTile, i)
  /** Or a constant Int value to each cellvalue of each band in a multiband raster. */
  def |(i: Int): MultiBandTile = localOr(i)
  /** Or a constant Int value to each cellvalue of each band in a multiband raster. */
  def |:(i: Int): MultiBandTile = localOr(i)
  /** Or the values of corresponding cell in each multiband raster.  */
  def localOr(m: MultiBandTile): MultiBandTile = Or(mTile, m)
  /** Or the values of corresponding cell in each multiband raster. */
  def |(m: MultiBandTile): MultiBandTile = localOr(m)
  /** Or the values of each cell in each band of multiband raster.  */
  def localOr(): Tile = Or(mTile)
  /** Or the values of each cell in each band in given range of multiband raster. */
  def |(f: Int, l: Int): Tile = Or(mTile, f, l)
}
