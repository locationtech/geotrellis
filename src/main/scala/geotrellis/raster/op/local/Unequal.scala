package geotrellis.raster.op.local

import geotrellis._

/** Determines if values are not equal, and sets results as 1 if not equal, 0 otherwise. */
object Unequal {
  /** Determines if two Int values are not equal */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => if(x != y) 1 else 0)
  /** Determines if cell values of the Raster are not equal. See [[UnequalConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = new UnequalConstant1(r, c)
  /** Determines if cell values of the Raster are not equal. See [[UnequalConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = new UnequalConstant2(c, r)
  /** Determines if corresponding cell values of the two input Rasters are not equal.
   * See [[UnequalRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = new UnequalRaster(r1, r2)
}

/**
 * Determines if cell values of the Raster are not equal.
 * Returns a Raster with TypeBit data, where 1 indicates the cell value
 * did not equal the input value.
 */
case class UnequalConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z != c) 1 else 0
  } {
    z => if (z != c) 1 else 0
  })
})

/**
 * Determines if cell values of the Raster are not equal.
 * Returns a Raster with TypeBit data, where 1 indicates the cell value
 * did not equal the input value.
 */
case class UnequalConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.convert(TypeBit).dualMapIfSet {
    z => if (z != c) 1 else 0
  } {
    z => if (z != c) 1 else 0
  })
})

/**
 * Determines if corresponding cell values of the two input Rasters are not equal.
 * Returns a Raster with TypeBit data, where 1 indicates the corresponding cell values
 * did not equal each other.
 */
case class UnequalRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.convert(TypeBit).dualCombine(r2.convert(TypeBit)) {
    (z1, z2) => if (z1 != z2) 1 else 0
  } {
    (z1, z2) => if (z1 != z2) 1 else 0
  })
})
