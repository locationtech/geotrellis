package geotrellis.io

import geotrellis._
import geotrellis.process._

/**
  * Load the [[RasterLayerInfo]] from the raster layer with the specified name.
  */
case class LoadRasterLayerInfo(n:Op[String]) extends Op[RasterLayerInfo] {
  def _run(context:Context) = runAsync(List(n, context))
  val nextSteps:Steps = {
    case (n:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayerInfo(n))
    }
  }
}

/**
  * Load the [[RasterLayerInfo]] from the raster layer at the specified path.
  */
case class LoadRasterLayerInfoFromPath(path:Op[String]) extends Op[RasterLayerInfo] {
  def _run(context:Context) = runAsync(List(path, context))
  val nextSteps:Steps = {
    case (path:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayerInfoFromPath(path))
    }
  }
}
