package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Operation to And values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 * @note        If used with Double typed multiband rasters, the values
 *              will be rounded to Ints.
 */

object And extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 & z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else i2d(d2i(z1) & d2i(z2))
}

trait AndMethods extends MultiBandTileMethods {
  /** And a constant Int value to each cell of each band in a multiband raster. */
  def localAnd(i: Int): MultiBandTile = And(mTile, i)
  /** And a constant Int value to each cell of each band in a multiband raster. */
  def &(i: Int): MultiBandTile = localAnd(i)
  /** And a constant Int value to each cell of each band in a multiband raster. */
  def &:(i: Int): MultiBandTile = localAnd(i)
  /** And the values of each cell in each band in each multiband rasters.  */
  def localAnd(m: MultiBandTile): MultiBandTile = And(mTile, m)
  /** And the values of each cell in each band in each multiband rasters. */
  def &(m: MultiBandTile): MultiBandTile = localAnd(m)
  /** And the values of each cell in each band in a multiband raster.  */
  def localAnd(): Tile = And(mTile)
  /** And the values of each cell in each band in a given range of multiband raster.  */
  def localAnd(f: Int, l: Int): Tile = And(mTile, f, l)

}