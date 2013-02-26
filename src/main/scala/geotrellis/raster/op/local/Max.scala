package geotrellis.raster.op.local

import scala.math.max

import geotrellis._

/**
 * Gets maximum values.
 */
object Max {
  /** Gets the maximum value between two integers */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => max(z1, z2))

  /** Gets the maximum value between two doubles */
  def apply(x:Op[Double], y:Op[Double])(implicit d:DummyImplicit) = logic.Do2(x, y)((z1, z2) => max(z1, z2))

  /** Gets the maximum value between cell values of a rasters and an Int constant. See [[MaxConstant]] */
  def apply(r:Op[Raster], c:Op[Int]) = MaxConstant(r, c)

  /** Gets the maximum value between cell values of a rasters and an Int constant. See [[MaxConstant]] */
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = MaxConstant(r, c)

  /** Gets the maximum value between cell values of a rasters and a Double constant. See [[MaxDoubleConstant]] */
  def apply(r:Op[Raster], c:Op[Double]) = MaxDoubleConstant(r, c)

  /** Gets the maximum value between cell values of a rasters and a Double constant. See [[MaxDoubleConstant]] */
  def apply(c:Op[Double], r:Op[Raster])(implicit d:DummyImplicit) = MaxDoubleConstant(r, c)

  /** Gets the maximum value between cell values of two rasters. See [[MaxRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = MaxRaster(r1, r2)
}

/**
 * Gets the maximum value between cell values of a rasters and an Int constant.
 */
case class MaxConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.dualMapIfSet(max(_, c))(max(_,c)))
})

/**
 * Gets the maximum value between cell values of a rasters and a Double constant.
 */
case class MaxDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c) ({
  (r, c) => Result(r.dualMapIfSet({z:Int => max(z, c).toInt})(max(_,c)))
})

/**
 * Gets the maximum value between cell values of two rasters.
 */
case class MaxRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.dualCombine(r2)
                                   ((z1, z2) => max(z1, z2))
                                   ((z1,z2) => max(z1,z2)))
})
