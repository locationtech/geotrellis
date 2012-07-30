package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object Equal {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((x, y) => if(x == y) 1 else 0)
  def apply(r:Op[Raster], c:Op[Int]) = new EqualConstant1(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = new EqualConstant2(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = new EqualRaster(r1, r2)
}

case class EqualConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.convert(TypeBit).mapIfSet(z => if (z == c) 1 else 0))
})

case class EqualConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.convert(TypeBit).mapIfSet(z => if (z == c) 1 else 0))
})

case class EqualRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.convert(TypeBit).combine(r2.convert(TypeBit)) {
    (z1, z2) => if (z1 == z2) 1 else 0
  })
})
