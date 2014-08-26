package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Gets minimum values.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns NoData.
 */

object Min extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else math.min(z1, z2)

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else math.min(z1, z2)
}

trait MinMethods extends MultiBandTileMethods {
  /** Min a constant Int value to each cell of band in a multiband raster. */
  def localMin(i: Int): MultiBandTile = Min(mTile, i)
  /** Min a constant Double value to each cell of band in a multiband raster. */
  def localMin(d: Double): MultiBandTile = Min(mTile, d)
  /** Min the values of each cell of each band in each multiband raster.  */
  def localMin(m: MultiBandTile): MultiBandTile = Min(mTile, m)
  /** Min the values of each cell in each band in a multiband raster.  */
  def localMin(): Tile = Min(mTile)
  /** Min the values of each cell in each band in a given range of multiband raster.  */
  def localMin(f: Int, l: Int): Tile = Min(mTile, f, l)
}
