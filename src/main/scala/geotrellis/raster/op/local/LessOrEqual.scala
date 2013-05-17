package geotrellis.raster.op.local

import geotrellis._

/**
 * Determines if one value is less than or equal to another. Sets to 1 if true, else 0.
 */
object LessOrEqual {
  /** Returns 1 if the first input integer is less than or equal to the second, else 0 */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => if(x <= y) 1 else 0)
  /** Returns a raster indicating which cell values are less than or equal to the input value.
   * See [[LessOrEqualConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = new LessOrEqualConstant1(r, c)
  /** Returns a raster indication which cell values are less than or equal to the input value.
   * See [[LessOrEqualConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = new LessOrEqualConstant2(c, r)
  /** Returns a raster indicating which cell values of the first input raster are
   *  less than or equal to the corresponding cells of the second input raster.
   *  See [[LessOrEqualRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = new LessOrEqualRaster(r1, r2)
}

/**
 * Returns a raster indicating which cell values are less than or equal to the input value.
 */
case class LessOrEqualConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => AndThen(logic.RasterDualMapIfSet(r)
    (z => if (z <= c) 1 else 0)
    (z => if (z <= c) 1 else 0)
  )
})

/**
 * Returns a raster indication which cell values are less than or equal to the input value.
 */
case class LessOrEqualConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => AndThen(logic.RasterDualMapIfSet(r)
    (z => if (z <= c) 1 else 0)
    (z => if (z <= c) 1 else 0))
})

/**
 * Returns a raster indicating which cell values of the first input raster are
 * less than or equal to the corresponding cells of the second input raster.
 */
case class LessOrEqualRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => AndThen(logic.RasterDualCombine(r1,r2)
    ((z1, z2) => if (z1 <= z2) 1 else 0)
    ((z1, z2) => if (z1 <= z2) 1 else 0))
})
