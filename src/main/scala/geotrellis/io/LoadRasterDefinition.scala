package geotrellis.io

import geotrellis._
import geotrellis.process._
import geotrellis.source._

object LoadRasterDefinition {
  def apply(n:String):LoadRasterDefinition =
    LoadRasterDefinition(LayerId(n))

  def apply(ds:String,n:String):LoadRasterDefinition =
    LoadRasterDefinition(LayerId(ds,n))
}

/**
  * Load the [[RasterDefinition]] from the raster layer with the specified name.
  */
case class LoadRasterDefinition(layerId:Op[LayerId]) extends Op[RasterDefinition] {
  def _run() = runAsync(List(layerId))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: Nil => 
      LayerResult { layerLoader =>
        val info = layerLoader.getRasterLayer(layerId).info
        RasterDefinition(layerId,info.rasterExtent,info.tileLayout)
      }
  }
}
