package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Or's cell values of rasters or Int values.
 */
object Or {
  /** Or's two integer values together. */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ | _)
  /** Or's an integer value with each cell of the raster. See [[OrConstant1]] */
  def apply(r:Op[Raster], c:Op[Int]) = OrConstant1(r, c)
  /** Or's an integer value with each cell of the raster. See [[OrConstant2]] */
  def apply(c:Op[Int], r:Op[Raster]) = OrConstant2(c, r)
  /** Or's the cell values of the two rasters. See [[OrRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = OrRaster(r1, r2)
}

/**
 * Or's an integer value with each cell of the raster
 */
case class OrConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(_ | c))
})

/**
 * Or's an integer value with each cell of the raster
 */
case class OrConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(_ | c))
})

/**
 * Or's the cell values of the two rasters.
 *
 * @note               OrRaster does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class OrRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)(_ | _))
})
