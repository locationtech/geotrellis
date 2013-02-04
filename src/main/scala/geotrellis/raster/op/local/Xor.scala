package geotrellis.raster.op.local

import geotrellis._
import geotrellis.process._

object Xor {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)(_ ^ _)
  def apply(r:Op[Raster], c:Op[Int]) = XorConstant1(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = XorConstant2(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = XorRaster(r1, r2)
}

case class XorConstant1(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(_ ^ c))
})

case class XorConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(_ ^ c))
})

/**
 * Xor's the cell values of two integer typed Rasters.
 */
case class XorRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)(_ ^ _))
})
