package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

/**
 * Maps values to 0 if the are NoData values, otherwise 1.
 */
object Defined {
  def apply(z:Op[Int]) = new DefinedConstant(z)
  def apply(r:Op[Raster]) = new DefinedRaster(r)
}

case class DefinedConstant(c:Op[Int]) extends Op1(c)({
  c => Result(if (c == NODATA) 0 else 1)
})

/**
 * Maps an integer typed Raster to 1 if the cell value is not NODATA, otherwise 0.
 */
case class DefinedRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.convert(TypeBit).map(z => if (z == NODATA) 0 else 1))
})
