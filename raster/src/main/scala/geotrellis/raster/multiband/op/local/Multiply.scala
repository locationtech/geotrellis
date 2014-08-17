package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Multiplies values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */

object Multiply extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 * z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 * z2
}

trait MultiplyMethods extends MultiBandTileMethods {
  /** Multiply a constant value from each cell of each band in a multiband raster.*/
  def localMultiply(i: Int): MultiBandTile = Multiply(mTile, i)
  /** Multiply a constant value from each cell of each band in a multiband raster.*/
  def *(i: Int): MultiBandTile = localMultiply(i)
  /** Multiply a constant value from each cell of each band in a multiband raster.*/
  def *:(i: Int): MultiBandTile = localMultiply(i)
  /** Multiply a double constant value from each cell of each band in a multiband raster.*/
  def localMultiply(d: Double): MultiBandTile = Multiply(mTile, d)
  /** Multiply a double constant value from each cell of each band in a multiband raster.*/
  def *(d: Double): MultiBandTile = localMultiply(d)
  /** Multiply a double constant value from each cell of each band in a multiband raster.*/
  def *:(d: Double): MultiBandTile = localMultiply(d)
  /** Multiply left band from right bands in a mltiband raster. */
  def localMultiply(): Tile = Multiply(mTile)
  /** Multiply left band from right bands in a mltiband raster. */
  def localMultiply(f: Int, l: Int): Tile = Multiply(mTile, f, l)
  /** Multiply the values of each cell in each multiband raster. */
  def localMultiply(m: MultiBandTile): MultiBandTile = Multiply(mTile, m)
  /** Multiply the values of each cell in each multiband raster. */
  def *(m: MultiBandTile): MultiBandTile = localMultiply(m)

}
