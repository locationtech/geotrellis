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
  def _run() = runAsync(List(ds, n))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: (n:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]], n).info
      }
  }
}

/**
  * Load the [[RasterLayerInfo]] from the raster layer at the specified path.
  */
case class LoadRasterLayerInfoFromPath(path:Op[String]) extends Op[RasterLayerInfo] {
  def _run() = runAsync(List(path))
  val nextSteps:Steps = {
    case (path:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).info
      }
  }
}
