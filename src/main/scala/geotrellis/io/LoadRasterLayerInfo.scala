package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterLayerInfo {
  def apply(n:String): LoadRasterLayerInfo = 
    LoadRasterLayerInfo(LayerId(n))

  def apply(ds:String, n:String): LoadRasterLayerInfo = 
    LoadRasterLayerInfo(LayerId(ds,n))
}

/**
  * Load the [[RasterLayerInfo]] from the raster layer with the specified name.
  */
case class LoadRasterLayerInfo(layerId:Op[LayerId]) extends Op[RasterLayerInfo] {
  def _run() = runAsync(List(layerId))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(layerId).info
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
