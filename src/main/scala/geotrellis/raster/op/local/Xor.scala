package geotrellis.raster.op.local

import geotrellis._

/**
 * Xor's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 */
object Xor {
  /**
   * Xor's two integer values together.
   *
   * @note      Whereas the other versions of this operation treat
   *            NODATA specially, the Xor'ing of two integers does
   *            not cause a NODATA value to fall through; NODATA
   *            in this case is treated like Int.MinValue.
   */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ ^ _)

  /** Xor's an integer value with each cell of the raster. See [[XorConstant]] */
  def apply(r:Op[Raster], c:Op[Int]) = XorConstant(r, c)
  /** Xor's an integer value with each cell of the raster. See [[XorConstant]] */
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = XorConstant(r, c)
  /** Xor's the cell values of the two rasters. See [[XorRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = XorRaster(r1, r2)
}

/**
 * Xor's an integer value with each cell of the raster
 */
case class XorConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(_ ^ c))
})

/**
 * Xor's the cell values of the two rasters.
 *
 * @note               If used with Double typed rasters, the values
 *                     will be rounded to Ints before Xor'ing.
 */
case class XorRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)(_ ^ _))
})
