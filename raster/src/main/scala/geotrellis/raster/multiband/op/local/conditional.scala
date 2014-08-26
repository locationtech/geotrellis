package geotrellis.raster.multiband.op.local

import geotrellis.raster._
import geotrellis.raster.multiband._

/**
 * Maps all cells matching `cond` to Int `trueValue`.
 */

object IfCell extends Serializable {
  /**Maps all cells matching `cond` to Int `trueValue`. */
  def apply(m: MultiBandTile, cond: Int => Boolean, trueValue: Int): MultiBandTile =
    m.dualMap { z: Int => if (cond(z)) trueValue else z } { z: Double => if (cond(d2i(z))) i2d(trueValue) else z }

  /**Maps all cells matching `cond` to Double `trueValue`. */
  def apply(m: MultiBandTile, cond: Double => Boolean, trueValue: Double): MultiBandTile =
    m.dualMap { z: Int => if (cond(i2d(z))) d2i(trueValue) else z } { z: Double => if (cond(z)) trueValue else z }

  /**
   * Set all values of output multiband raster to one value or another based on whether a
   * condition is true or false.
   */
  def apply(m: MultiBandTile, cond: Int => Boolean, trueValue: Int, falseValue: Int): MultiBandTile =
    m.dualMap { z: Int => if (cond(z)) trueValue else falseValue } { z: Double => if (cond(d2i(z))) i2d(trueValue) else i2d(falseValue) }

  /**
   * Set all values of output multiband raster to one value or another based on whether a
   * condition is true or false for Double values.
   */
  def apply(m: MultiBandTile, cond: Double => Boolean, trueValue: Double, falseValue: Double): MultiBandTile =
    m.dualMap { z: Int => if (cond(i2d(z))) d2i(trueValue) else d2i(falseValue) } { z: Double => if (cond(z)) trueValue else falseValue }

  /**
   * Given a condition over two multiband rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input multiband rasters.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, cond: (Int, Int) => Boolean, trueValue: Int): MultiBandTile =
    m1.dualCombine(m2) { (z1, z2) => if (cond(z1, z2)) trueValue else z1 } { (z1, z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else z1 }

  /**
   * Given a Double condition over two multiband rasters, set the value of each cell in the output
   * to a specified value if the condition is true given the corresponding values in
   * each of the two input multibnd rasters.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, cond: (Double, Double) => Boolean, trueValue: Double): MultiBandTile =
    m1.dualCombine(m2) { (z1, z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else z1 } { (z1, z2) => if (cond(z1, z2)) trueValue else z1 }

  /**
   * Given a condition over two multiband rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input multiband rasters.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, cond: (Int, Int) => Boolean, trueValue: Int, falseValue: Int): MultiBandTile =
    m1.dualCombine(m2) { (z1, z2) => if (cond(z1, z2)) trueValue else falseValue } { (z1, z2) => if (cond(d2i(z1), d2i(z2))) i2d(trueValue) else i2d(falseValue) }

  /**
   * Given a Double condition over two multiband rasters, set the value of each cell in the output
   * to a specified true or false value after testing the specified condition on
   * the corresponding values in each of the two input multiband rasters.
   */
  def apply(m1: MultiBandTile, m2: MultiBandTile, cond: (Double, Double) => Boolean, trueValue: Double, falseValue: Double): MultiBandTile =
    m1.dualCombine(m2) { (z1, z2) => if (cond(i2d(z1), i2d(z2))) d2i(trueValue) else d2i(falseValue) } { (z1, z2) => if (cond(z1, z2)) trueValue else falseValue }
}

trait ConditionalMethods extends MultiBandTileMethods {
  def localIf(cond: Int => Boolean, trueValue: Int): MultiBandTile =
    IfCell(mTile, cond, trueValue)
  def localIf(cond: Double => Boolean, trueValue: Double): MultiBandTile =
    IfCell(mTile, cond, trueValue)
  def localIf(cond: Int => Boolean, trueValue: Int, falseValue: Int): MultiBandTile =
    IfCell(mTile, cond, trueValue, falseValue)
  def localIf(cond: Double => Boolean, trueValue: Double, falseValue: Double): MultiBandTile =
    IfCell(mTile, cond, trueValue, falseValue)
  def localIf(m: MultiBandTile, cond: (Int, Int) => Boolean, trueValue: Int): MultiBandTile =
    IfCell(mTile, m, cond, trueValue)
  def localIf(m: MultiBandTile, cond: (Double, Double) => Boolean, trueValue: Double): MultiBandTile =
    IfCell(mTile, m, cond, trueValue)
  def localIf(m: MultiBandTile, cond: (Int, Int) => Boolean, trueValue: Int, falseValue: Int): MultiBandTile =
    IfCell(mTile, m, cond, trueValue, falseValue)
  def localIf(m: MultiBandTile, cond: (Double, Double) => Boolean, trueValue: Double, falseValue: Double): MultiBandTile =
    IfCell(mTile, m, cond, trueValue, falseValue)
}
