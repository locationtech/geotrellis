package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Divides values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */

object Divide extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else if (z2 == 0) NODATA
    else z1 / z2

  def combine(z1: Double, z2: Double) =
    if (z2 == 0) Double.NaN
    else z1 / z2
}

trait DivideMethods extends MultiBandTileMethods {
  /** Divide each value of the bands of multiband raster by a constant value.*/
  def localDivide(i: Int): MultiBandTile = Divide(mTile, i)
  /** Divide each value of the bands of multiband raster by a constant value.*/
  def /(i: Int): MultiBandTile = localDivide(i)
  /** Divide a constant value by each cell value of each bands in multiband raster.*/
  def localDivideValue(i: Int): MultiBandTile = Divide(i, mTile)
  /** Divide a constant value by each cell value of each bands in multiband raster.*/
  def /:(i: Int): MultiBandTile = localDivideValue(i)
  /** Divide each value of bands in a multiband raster by a double constant value.*/
  def localDivide(d: Double): MultiBandTile = Divide(mTile, d)
  /** Divide each value of bands in a multiband raster by a double constant value.*/
  def /(d: Double): MultiBandTile = localDivide(d)
  /** Divide a double constant value by each cell value of each bands in multiband raster.*/
  def localDivideValue(d: Double): MultiBandTile = Divide(d, mTile)
  /** Divide a double constant value by each cell value of each bands in multiband raster.*/
  def /:(d: Double): MultiBandTile = localDivideValue(d)
  /** Devide left band from right bands of multiband raster.*/
  def localDivide(): Tile = Divide(mTile)
  /** Devide left band from right bands in a given range of multiband raster.*/
  def localDivide(f: Int, l: Int): Tile = Divide(mTile,f,l)
  /** Divide the values of each cell in each mutliband raster. */
  def localDivide(m: MultiBandTile): MultiBandTile = Divide(mTile, m)
  /** Divide the values of each cell in each multiband raster. */
  def /(m: MultiBandTile): MultiBandTile = localDivide(m)
}
