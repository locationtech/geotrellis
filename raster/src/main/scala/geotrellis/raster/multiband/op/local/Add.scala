
package geotrellis.raster.multiband.op.local

import geotrellis.raster.multiband.MultiBandTileMethods
import geotrellis.raster._
import geotrellis.raster.multiband._

import scala.annotation.tailrec

/**
 * Operation to add values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA or Double.NaN.
 */

object Add extends LocalMultiBandBinaryOp {
  def combine(z1: Int, z2: Int) =
    if (isNoData(z1) || isNoData(z2)) NODATA
    else z1 + z2

  def combine(z1: Double, z2: Double) =
    if (isNoData(z1) || isNoData(z2)) Double.NaN
    else z1 + z2
}

trait AddMethods extends MultiBandTileMethods {

  /** Add a constant Int value to each cell. */
  def localAdd(i: Int): MultiBandTile = Add(mTile, i)
  /** Add a constant Int value to each cell. */
  def +(i: Int): MultiBandTile = localAdd(i)
  /** Add a constant Int value to each cell. */
  def +:(i: Int): MultiBandTile = localAdd(i)
  /** Add a constant Double value to each cell. */
  def localAdd(d: Double): MultiBandTile = Add(mTile, d)
  /** Add a constant Double value to each cell. */
  def +(d: Double): MultiBandTile = localAdd(d)
  /** Add a constant Double value to each cell. */
  def +:(d: Double): MultiBandTile = localAdd(d)
  /** Add value of each cell together in bands. */
  def localAdd(): Tile = Add(mTile)
  /** Add value of each cell together in given bands. */
  def localAdd(f: Int, l: Int): Tile = Add(mTile, f, l)
  /** Add the values of each cell in each raster.  */
  def localAdd(m: MultiBandTile): MultiBandTile = Add(mTile, m)
  /** Add the values of each cell in each raster. */
  def +(m: MultiBandTile): MultiBandTile = localAdd(m)
}
