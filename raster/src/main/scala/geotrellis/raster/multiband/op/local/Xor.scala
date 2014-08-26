package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Xor's cell values of multiband rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed rasters, the values
 *              will be rounded to Ints.
 */

object Xor extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 ^ z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) ^ d2i(z2))
}

trait XorMethods extends MultiBandTileMethods {
  /** Xor a constant Int value to each cell of each band in a multiband raster. */
  def localXor(i: Int): MultiBandTile = Xor(mTile, i)
  /** Xor a constant Int value to each cell of each band in a multiband raster. */
  def ^(i: Int): MultiBandTile = localXor(i)
  /** Xor a constant Int value to each cell of each band in a multiband raster. */
  def ^:(i: Int): MultiBandTile = localXor(i)
  /** Xor the values of each cell in each bands in multiband raster.  */
  def localXor(m: MultiBandTile): MultiBandTile = Xor(mTile, m)
  /** Xor the values of each cell in each bands in multiband raster. */
  def ^(m: MultiBandTile): MultiBandTile = localXor(m)
  /** Xor the values of each cell in each band of a multiband raster. */
  def localXor(): Tile = Xor(mTile)
  /** Xor the values of each cell in each band in given range of multiband raster. */
  def localXor(f: Int, l: Int): Tile = Xor(mTile, f, l)
}