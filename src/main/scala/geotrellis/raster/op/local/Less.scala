package geotrellis.raster.op.local

import geotrellis._

/**
 * Determines if one value is less than another. Sets to 1 if true, else 0.
 */
object Less {
  /** Returns 1 if the first input integer is less than the second, else 0 */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => if(x < y) 1 else 0)
  /** Returns a raster indicating which cell values are less than the input value. See [[LessConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = new LessConstant1(r, c)
  /** Returns a raster indication which cell values are less than the input value. See [[LessConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = new LessConstant2(c, r)
  /** Returns a raster indicating which cell values of the first input raster are
   *  less than the corresponding cells of the second input raster. See [[LessRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = new LessRaster(r1, r2)
}

/**
 * Returns a Raster with TypeBit data that has cell values of 1 if the
 * corresponding cell value is less than the input integer.
 */
case class LessConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z < c) 1 else 0
  } {
    z => if (z < c) 1 else 0
  })
})

/**
 * Returns a Raster with TypeBit data that has cell values of 1 if the
 * corresponding cell value is less than the input integer.
 */
case class LessConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z < c) 1 else 0
  } {
    z => if (z < c) 1 else 0
  })
})

/**
 * Returns a raster indicating which cell values of the first input raster are
 * less than the corresponding cells of the second input raster.
 */
case class LessRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.convert(TypeBit).dualCombine(r2.convert(TypeBit)) {
    (z1, z2) => if (z1 < z2) 1 else 0
  } {
    (z1, z2) => if (z1 < z2) 1 else 0
  })
})
