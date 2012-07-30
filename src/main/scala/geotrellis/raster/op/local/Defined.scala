package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object Defined {
  def apply(z:Op[Int]) = new DefinedConstant(z)
  def apply(r:Op[Raster]) = new DefinedRaster(r)
}

case class DefinedConstant(c:Op[Int]) extends Op1(c)({
  c => Result(if (c == NODATA) 0 else 1)
})

case class DefinedRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.convert(TypeBit).map(z => if (z == NODATA) 0 else 1))
})
