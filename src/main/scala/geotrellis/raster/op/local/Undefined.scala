package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Maps an integer or Raster to 1 or 0.
 * In the integer case, maps to 1 if not NODATA, else 0.
 * In the Raster case, maps cell values to 1 if they are not NODATA, else 0.
 */
object Undefined {
  /** Maps an integer to 1 if NODATA, else 0. See [[UndefinedConstant]] */
  def apply(z:Op[Int]) = new UndefinedConstant(z)
  /** Maps Raster cell values to 1 if they are NODATA, else 0. See [[UndefinedRaster]] */
  def apply(r:Op[Raster]) = new UndefinedRaster(r)
}

/**
 * Maps an integer to 1 if NODATA, else 0.
 */
case class UndefinedConstant(c:Op[Int]) extends Op1(c)({
  c => Result(if (c == NODATA) 1 else 0)
})

/**
 * Maps Raster cell values to 1 if they are NODATA, else 0.
 */
case class UndefinedRaster(r:Op[Raster]) extends Op1(r)({
  (r) => AndThen(logic.RasterMap(r.convert(TypeBit))(z => if (z == NODATA) 1 else 0))
})
