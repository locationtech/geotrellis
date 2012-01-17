package trellis.operation

import trellis._
import trellis.process._

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
case class AutomaticNormalize(r:Op[IntRaster], g:Op[(Int, Int)]) extends Op[IntRaster] {

  def _run(context:Context) = runAsync(List(r, g))

  val nextSteps:Steps = {
    case (raster:IntRaster) :: (goal:Tuple2[_, _]) :: Nil => {
      step2(raster, goal.asInstanceOf[(Int, Int)])
    }
  }

  def step2(raster:IntRaster, goal:(Int, Int)) = {
    val (zmin, zmax) = raster.findMinMax
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
  }
}

/**
 * Normalize the values in the given raster.
 * zmin and zmax are the lowest and highest values in the provided raster.
 * gmin and gmax are the desired minimum and maximum values in the output raster.
 */ 
case class PrecomputedNormalize(r:Op[IntRaster], c:Op[(Int, Int)],
                                g:Op[(Int, Int)]) extends Op[IntRaster] {

  def _run(context:Context) = runAsync(List(r, c, g))

  val nextSteps:Steps = {
    case (raster:IntRaster) :: (curr:Tuple2[_, _]) :: (goal:Tuple2[_, _]) :: Nil => {
      step2(raster, curr.asInstanceOf[(Int, Int)], goal.asInstanceOf[(Int, Int)])
    }
  }

  def step2(raster:IntRaster, curr:(Int, Int), goal:(Int, Int)) = {
    val (zmin, zmax) = curr
    val (gmin, gmax) = goal
    Result(raster.normalize(zmin, zmax, gmin, gmax))
  }
}
