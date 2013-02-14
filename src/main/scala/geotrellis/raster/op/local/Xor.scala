package geotrellis.raster.op.local

import geotrellis._

/**
 * Xor's cell values of rasters or Int values.
 */
object Xor {
  /** Xor's two integer values together. */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ ^ _)
  /** Xor's an integer value with each cell of the raster. See [[XorConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = XorConstant1(r, c)
  /** Xor's an integer value with each cell of the raster. See [[XorConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = XorConstant2(c, r)
  /** Xor's the cell values of the two rasters. See [[XorRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = XorRaster(r1, r2)
}

/**
 * Xor's an integer value with each cell of the raster
 */
case class XorConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(_ ^ c))
})

/**
 * Xor's an integer value with each cell of the raster
 */
case class XorConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(_ ^ c))
})

/**
 * Xor's the cell values of the two rasters.
 *
 * @note               XorRaster does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class XorRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)(_ ^ _))
})
