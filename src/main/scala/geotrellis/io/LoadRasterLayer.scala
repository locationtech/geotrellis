package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterLayer {
  def apply(n:String): LoadRasterLayer =
    LoadRasterLayer(LayerId(n))

  def apply(ds:String, n:String): LoadRasterLayer =
    LoadRasterLayer(LayerId(ds,n))
}

/**
  * Load the [[RasterLayer]] from the raster layer with the specified name.
  */
case class LoadRasterLayer(layerId:Op[LayerId]) extends Op[RasterLayer] {
  def _run() = runAsync(List(layerId))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(layerId)
      }
  }
}

/**
  * Load the [[RasterLayer]] from the raster layer at the specified path.
  */
case class LoadRasterLayerFromPath(path:Op[String]) extends Op[RasterLayer] {
  def _run() = runAsync(List(path))
  val nextSteps:Steps = {
    case (path:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path)
      }
  }
}
