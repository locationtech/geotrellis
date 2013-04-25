package geotrellis.raster.op.local

import geotrellis._

/**
 * Operation for dividing values.
 */
object Divide {
  /** Divide two integer values. */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => x / y)

  /** Divide two double values. */
  def apply(x:Op[Double], y:Op[Double])(implicit d: DummyImplicit) = logic.Do2(x, y)((x, y) => x / y)

  /** Divide each cell by a constant Int value. See [[DivideConstant]] */
  def apply(r:Op[Raster], c:Op[Int]) = DivideConstant(r, c)

  /** Divide each cell by a constant Double value. See [[DivideDoubleConstant]] */
  def apply(r:Op[Raster], c:Op[Double]) = DivideDoubleConstant(r, c)

  /** Divide each cell by a constant Int value. See [[DivideConstantBy]] */
  def apply(c:Op[Int], r:Op[Raster]) = DivideConstantBy(c, r)

  /** Divide each cell by a constant Double value. See [[DivideDoubleConstantBy]] */
  def apply(c:Op[Double], r:Op[Raster]) = DivideDoubleConstantBy(c, r)

  /** Divide each value of one raster with the values from another raster. */
  def apply(r1:Op[Raster], r2:Op[Raster]) = DivideRaster(r1, r2)
}

/**
 * Divide each cell by a constant value.
 */
case class DivideConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c)({
  (r, c) => Result(r.dualMapIfSet(_ / c)(_ / c))
})

/**
 * Divide each cell by a constant Double value.
 */
case class DivideDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c)({
  (r, c) => AndThen(logic.RasterDualMapIfSet(r)({i:Int => (i / c).toInt})(_ / c))
})

/**
 * Divide each cell by a constant value.
 */
case class DivideConstantBy(c:Op[Int], r:Op[Raster]) extends Op2(c, r)({
  (c, r) => AndThen(logic.RasterDualMapIfSet(r)(c / _)(c / _))
})

/**
 * Divide each cell by a constant Double value.
 */
case class DivideDoubleConstantBy(c:Op[Double], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => AndThen(logic.RasterDualMapIfSet(r)({i:Int => (c / i).toInt})(c / _))
})

/**
 * Divide each value of one raster with the values from another raster.
 */
case class DivideRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1,r2)({
  (r1,r2) => AndThen(logic.RasterDualCombine(r1,r2)
  ((z1:Int, z2:Int) => if (z2 == NODATA || z2 == 0 || z1 == NODATA) {
    NODATA
  } else {
    z1 / z2
  })
  ((z1:Double, z2:Double) => z1 / z2)
  )
})
