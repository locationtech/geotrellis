package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Or's cell values of rasters or Int values.
 *
 * @note        NoData values will cause the results of this operation
 *              to be NODATA.
 */
object Or {
  /** Or's two integer values together. */
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ | _)
  /** Or's an integer value with each cell of the raster. See [[OrConstant]] */
  def apply(r:Op[Raster], c:Op[Int]) = OrConstant(r, c)
  /** Or's an integer value with each cell of the raster. See [[OrConstant]] */
  def apply(c:Op[Int], r:Op[Raster])(implicit d:DummyImplicit) = OrConstant(r, c)
  /** Or's the cell values of the two rasters. See [[OrRaster]] */
  def apply(r1:Op[Raster], r2:Op[Raster]) = OrRaster(r1, r2)
}

/**
 * Or's an integer value with each cell of the raster
 */
case class OrConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => AndThen(logic.RasterMapIfSet(r)(_ | c))
})

/**
 * Or's the cell values of the two rasters.
 *
 * @note               If used with Double typed rasters, the values
 *                     will be rounded to Ints before or'ing.
 */
case class OrRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => AndThen(logic.RasterCombine(r1,r2)(_ | _))
})
