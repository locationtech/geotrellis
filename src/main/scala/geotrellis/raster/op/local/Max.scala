package geotrellis.raster.op.local

import scala.math.max

import geotrellis._

/**
 * Gets maximum values.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns the value.
 */
object Max {
  /**
   * Gets the maximum value between two integers.
   *
   * @note
   * Whereas the operations dealing with rasters use the rule that
   * the Max of a NODATA and another value v is the latter value v,
   * this operation does not differentiate NODATA from other integers
   * and will return what scala.math.max(NODATA, v) returns (which
   * since NODATA = Int.MinValue will be the value v).
   */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => max(z1, z2))

  /**
   * Gets the maximum value between two doubles.
   *
   * @note
   * Whereas the operations dealing with rasters use the rule that
   * the Max of a Double.NaN (the NoData value for Double values)
   * and another value v is the latter value v,
   * this operation does not differentiate Double.NaN from other Double values
   * and will return what scala.math.max(Double.NaN, v) returns (which
   * is Double.NaN).
   */
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
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns the value.
 */
case class MaxConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => 
    if(c == NODATA) { Result(r) }
    else {
      Result(r.dualMap({
        z => 
          max(z, c) // Since NODATA is Int.MinValue, if z is NODATA then result will be c
      })({
        z =>
          if(java.lang.Double.isNaN(z)) { c } else { max(z,c) }
      }))
    }
})

/**
 * Gets the maximum value between cell values of a rasters and a Double constant.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns the value.
 */
case class MaxDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c) ({
  (r, c) => 
    if(java.lang.Double.isNaN(c)) { Result(r) }
    else {
      Result(r.dualMap({
        z => 
          if(z == NODATA) { c.toInt } else { max(z,c).toInt }
      })({
        z =>
          if(java.lang.Double.isNaN(z)) { c } else { max(z,c) }
      }))
    }
})

/**
 * Gets the maximum value between cell values of two rasters.
 *
 * @note          Max handles NoData values such that taking the Max
 *                between a value and NoData returns the value.
 */
case class MaxRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => 
    Result(r1.dualCombine(r2)({
      (z1, z2) =>
        max(z1, z2) // Since NODATA is Int.MinValue, NODATA rule will work out
    })({
      (z1,z2) => 
        if(java.lang.Double.isNaN(z1)) { z2 }
        else if(java.lang.Double.isNaN(z2)) { z1 }
        else { max(z1,z2) }
    })
  )
})
