package geotrellis.raster.op.local

import scala.math.max

import geotrellis._
import geotrellis.process._

/**
 * Gets maximum values.
 *
 * @note               Max does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Max {
  def apply(x:Op[Int], y:Op[Int]) = logic.Do2(x, y)((z1, z2) => max(z1, z2))
  def apply(r:Op[Raster], c:Op[Int]) = MaxConstant(r, c)
  def apply(c:Op[Int], r:Op[Raster]) = MaxConstant2(c, r)
  def apply(r1:Op[Raster], r2:Op[Raster]) = MaxRaster(r1, r2)
}

/**
 * Gets the maximum value between cell values of a rasters and a constant.
 *
 * @note               MaxConstant does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class MaxConstant(r:Op[Raster], c:Op[Int]) extends Op2(r, c) ({
  (r, c) => Result(r.mapIfSet(z => max(z, c)))
})

/**
 * Gets the maximum value between cell values of a rasters and a constant.
 *
 * @note               MaxConstant2 does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class MaxConstant2(c:Op[Int], r:Op[Raster]) extends Op2(c, r) ({
  (c, r) => Result(r.mapIfSet(z => max(z, c)))
})

/**
 * Gets the maximum value between cell values of two rasters.
 *
 * @note               MaxRaster does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class MaxRaster(r1:Op[Raster], r2:Op[Raster]) extends Op2(r1, r2) ({
  (r1, r2) => Result(r1.combine(r2)((z1, z2) => max(z1, z2)))
})
