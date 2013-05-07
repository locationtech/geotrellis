package geotrellis.raster.op.local

import scala.math.min

import geotrellis._

/**
 * Gets minimum values.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns the value.
 */
object Min {
  /**
   * Takes the min value of two Int values.
   *
   * @note
   * Whereas the operations dealing with rasters use the rule that
   * the Min of a NODATA and another value v is the latter value v,
   * this operation does not differentiate NODATA from other integers
   * and will return what scala.math.min(NODATA, v) returns (which
   * since NODATA = Int.MinValue will be NODATA).
   */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => min(z1, z2))

  /** Takes the min value of two Double values.
   *
   * @note
   * Whereas the operations dealing with rasters use the rule that
   * the Min of a Double.NaN (the NoData value for Double values)
   * and another value v is the latter value v,
   * this operation does not differentiate Double.NaN from other Double values
   * and will return what scala.math.min(Double.NaN, v) returns (which
   * is Double.NaN).
   */
  def apply(x:Op[Double], y:Op[Double])(implicit d:DummyImplicit) = logic.Do2(x, y)((z1, z2) => min(z1, z2))

  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinConstant]]*/
  def apply(r:Op[Raster], c:Op[Int]) = MinConstant(r, c)

  /** Takes a Raster and an Int, and gives a raster with each cell being
   * the min value of the original raster and the integer. See [MinConstant]]*/
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = MinConstant(r, c)

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the min value of the original raster and the Double. See [MinDoubleConstant]]*/
  def apply(r:Op[Raster], c:Op[Double]) = MinDoubleConstant(r, c)

  /** Takes a Raster and an Double, and gives a raster with each cell being
   * the min value of the original raster and the Double. See [MinDoubleConstant]]*/
  def apply(c:Op[Double], r:Op[Raster])(implicit d:DummyImplicit) = MinDoubleConstant(r, c)

  /** Takes two Rasters and gives a raster with the min values of the two at each cell.
   * See [[MinRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = MinRaster(r1, r2)
}

/**
 * Takes a Raster and an Int, and gives a raster with each cell being
 * the min value of the original raster and the integer.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns the value.
 */
case class MinConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => 
    if (c == NODATA) 
      Result(r)
    else 
      AndThen(logic.RasterDualMap(r)
        (z => if(z == NODATA) c else min(z, c) )
        (z => if(java.lang.Double.isNaN(z)) c else min(z,c)))
})

/**
 * Takes a Raster and an Double, and gives a raster with each cell being
 * the min value of the original raster and the integer.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns the value.
 */
case class MinDoubleConstant(r:Op[Raster], c:Op[Double]) extends Op2(r, c) ({
  (r, c) => 
    if (java.lang.Double.isNaN(c))
      Result(r)
    else 
      AndThen(logic.RasterDualMap(r)
        (z => if(z == NODATA) c.toInt else min(z,c).toInt)
        (z => if(java.lang.Double.isNaN(z)) c else min(z,c))
      )
})

/**
 * Takes two Rasters and gives a raster with the min values of the two at each cell.
 *
 * @note          Min handles NoData values such that taking the Min
 *                between a value and NoData returns the value.
 */
case class MinRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => AndThen(logic.RasterDualCombine(r1,r2)
    ((z1, z2) =>
        if(z1 == NODATA) { z2 }
        else if(z2 == NODATA) { z1 }
        else { min(z1,z2) })
    ((z1,z2) => 
        if(java.lang.Double.isNaN(z1)) { z2 }
        else if(java.lang.Double.isNaN(z2)) { z1 }
        else { min(z1,z2) }))
})
