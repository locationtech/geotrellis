package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Gets maximum values.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns NoData.
 */

object Max extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else math.max(z1, z2)

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else math.max(z1, z2)
}

trait MaxMethods extends MultiBandTileMethods {
  /** Max a constant Int value to each cell of band in multiband raster. */
  def localMax(i: Int): MultiBandTile = Max(mTile, i)
  /** Max a constant Double value to each cell of band in multiband raster. */
  def localMax(d: Double): MultiBandTile = Max(mTile, d)
  /** Max the values of each cell in each raster.  */
  def localMax(m: MultiBandTile): MultiBandTile = Max(mTile, m)
  /** Max the values of each cell in each band in given multiband raster.  */
  def localMax(): Tile = Max(mTile)
  /** Max the values of each cell in each band in given range of ramultiband raster.  */
  def localMax(f: Int, l: Int): Tile = Max(mTile, f, l)
}
