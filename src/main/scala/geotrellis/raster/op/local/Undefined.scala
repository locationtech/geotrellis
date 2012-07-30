package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object Undefined {
  def apply(z:Op[Int]) = new UndefinedConstant(z)
  def apply(r:Op[Raster]) = new UndefinedRaster(r)
}

case class UndefinedConstant(c:Op[Int]) extends Op1(c)({
  c => Result(if (c == NODATA) 1 else 0)
})

case class UndefinedRaster(r:Op[Raster]) extends Op1(r)({
  r => Result(r.convert(TypeBit).map(z => if (z == NODATA) 1 else 0))
})
