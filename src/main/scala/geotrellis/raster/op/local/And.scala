package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object And {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ & _)
  def apply(r:Op[Raster], c:Op[Int]) = AndConstant1(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = AndConstant2(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = AndRaster(r1, r2)
}

case class AndConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(_ & c))
})

case class AndConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(_ & c))
})

case class AndRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)(_ & _))
})
