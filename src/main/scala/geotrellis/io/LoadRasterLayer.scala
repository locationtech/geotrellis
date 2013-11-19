package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
  * Load the [[RasterLayer]] from the raster layer with the specified name.
  */
case class LoadRasterLayer(n:Op[String]) extends Op[RasterLayer] {
  def _run(context:Context) = runAsync(List(n, context))
  val nextSteps:Steps = {
    case (n:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayer(n))
    }
  }
}

/**
  * Load the [[RasterLayer]] from the raster layer at the specified path.
  */
case class LoadRasterLayerFromPath(path:Op[String]) extends Op[RasterLayer] {
  def _run(context:Context) = runAsync(List(path, context))
  val nextSteps:Steps = {
    case (path:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayerFromPath(path))
    }
  }
}
