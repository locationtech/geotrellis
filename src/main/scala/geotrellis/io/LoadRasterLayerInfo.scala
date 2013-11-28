package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterLayerInfo {
  def apply(n:Op[String]): LoadRasterLayerInfo = 
    LoadRasterLayerInfo(None,n)

  def apply(ds:String, n:Op[String]): LoadRasterLayerInfo = 
    LoadRasterLayerInfo(Some(ds),n)
}

/**
  * Load the [[RasterLayerInfo]] from the raster layer with the specified name.
  */
case class LoadRasterLayerInfo(ds:Op[Option[String]], n:Op[String]) extends Op[RasterLayerInfo] {
  def _run(context:Context) = runAsync(List(ds, n, context))
  val nextSteps:Steps = {
    case (ds:Option[String]) :: (n:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayer(ds, n).info)
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
      Result(context.getRasterLayerFromPath(path).info)
    }
  }
}
