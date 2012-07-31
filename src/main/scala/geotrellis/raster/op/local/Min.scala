package geotrellis.raster.op.local

import scala.math.min

import geotrellis._
import geotrellis.process._

object Min {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => min(z1, z2))
  def apply(r:Op[Raster], c:Op[Int]) = MinConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = MinConstant2(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = MinRaster(r1, r2)
}

case class MinConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(z => min(z, c)))
})

case class MinConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(z => min(z, c)))
})

case class MinRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)((z1, z2) => min(z1, z2)))
})
