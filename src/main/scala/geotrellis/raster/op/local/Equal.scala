package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Determines if values are equal. Sets to 1 if true, else 0.
 */
object Equal {
  /** Returns 1 if two Int are equal, otherwise 0 */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => if(x == y) 1 else 0)
  /** Determines if cell values of a Raster are equal to an integer. See [[EqualConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = new EqualConstant1(r, c)
  /** Determines if cell values of a Raster are equal to an integer. See [[EqualConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = new EqualConstant2(c, r)
  /** Returns a raster indication what values of the two rasters are equal. See [[EqualRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = new EqualRaster(r1, r2)
}

/**
 * Returns a Raster with data of TypeBit, where cell values equal 1 if
 * the corresponding cell value of the input raster is equal to the input
 * intenger, else 0.
 */
case class EqualConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z == c) 1 else 0
  } {
    z => if (z == c) 1 else 0
  })
})

/**
 * Returns a Raster with data of TypeBit, where cell values equal 1 if
 * the corresponding cell value of the input raster is equal to the input
 * intenger, else 0.
 */
case class EqualConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z == c) 1 else 0
  } {
    z => if (z == c) 1 else 0
  })
})

/**
 * Returns a Raster with data of TypeBit, where cell values equal 1 if
 * the corresponding cell values of the input rasters are equal, else 0.
 */
case class EqualRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.convert(TypeBit).dualCombine(r2.convert(TypeBit)) {
    (z1, z2) => if (z1 == z2) 1 else 0
  } {
    (z1, z2) => if (z1 == z2) 1 else 0
  })
})
