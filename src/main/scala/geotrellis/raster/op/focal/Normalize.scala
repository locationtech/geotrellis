package geotrellis.raster.op.focal

import geotrellis._
import geotrellis.process._

// TODO: deprecate normalize
/**
 * We'd like to use the normalize name both when we want to automatically
 * detect the min/max values, and when we provide them explicitly.
 *
 * @note               Normalize does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Normalize {
  def apply(r:Op[Raster], g:Op[(Int, Int)]) = AutomaticNormalize(r, g)
  def apply(r:Op[Raster], c:Op[(Int, Int)], g:Op[(Int, Int)]) = PrecomputedNormalize(r, c, g)
}


/**
 * Rescale a raster when we want to automatically detect the min/max values,
 * and when we provide them explicitly.
 *
 * @note               Rescale does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
object Rescale {
  def apply(r:Op[Raster], g:Op[(Int, Int)]) = AutomaticNormalize(r, g)
  def apply(r:Op[Raster], c:Op[(Int, Int)], g:Op[(Int, Int)]) = PrecomputedNormalize(r, c, g)
}

/**
 * Normalize the values in the given raster so that all values are within the
 * specified minimum and maximum value range.
 * @note               AutomaticNormalize does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */
case class AutomaticNormalize(r:Op[Raster], g:Op[(Int, Int)]) extends Op2(r,g) ({
  (raster,goal) => 
    val (zmin, zmax) = raster.findMinMax
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
})

/**
 * Normalize the values in the given raster.
 * zmin and zmax are the lowest and highest values in the provided raster.
 * gmin and gmax are the desired minimum and maximum values in the output raster.
 *
 * @note               PrecomputedNormalize does not currently support Double raster data.
 *                     If you use a Raster with a Double RasterType (TypeFloat,TypeDouble)
 *                     the data values will be rounded to integers.
 */ 
case class PrecomputedNormalize(r:Op[Raster], c:Op[(Int, Int)],
                                g:Op[(Int, Int)]) extends Op3(r,c,g) ({
  (raster,curr,goal) =>
    val (zmin, zmax) = curr
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
})
