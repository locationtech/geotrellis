package geotrellis.operation

import geotrellis._
import geotrellis.process._

/**
 * We'd like to use the normalize name both when we want to automatically
 * detect the min/max values, and when we provide them explicitly.
 */
object Normalize {
  def apply(r:Op[IntRaster], g:Op[(Int, Int)]) = AutomaticNormalize(r, g)
  def apply(r:Op[IntRaster], c:Op[(Int, Int)], g:Op[(Int, Int)]) = PrecomputedNormalize(r, c, g)
}

/**
 * Normalize the values in the given raster so that all values are within the
 * specified minimum and maximum value range.
 */
case class AutomaticNormalize(r:Op[IntRaster], g:Op[(Int, Int)]) extends Op2(r,g) ({
  (raster,goal) => {
    val (zmin, zmax) = raster.findMinMax
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
  }
})

/**
 * Normalize the values in the given raster.
 * zmin and zmax are the lowest and highest values in the provided raster.
 * gmin and gmax are the desired minimum and maximum values in the output raster.
 */ 
case class PrecomputedNormalize(r:Op[IntRaster], c:Op[(Int, Int)],
                                g:Op[(Int, Int)]) extends Op3(r,c,g) ({
  (raster,curr,goal) => {
    val (zmin, zmax) = curr
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
  }
})
