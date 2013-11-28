package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRaster {
  def apply(n: String):LoadRaster =
    LoadRaster(LayerId(n), None)

  def apply(n: String,re: RasterExtent):LoadRaster =
    LoadRaster(LayerId(n), Some(re))

  def apply(ds: String, n: String): LoadRaster =
    LoadRaster(LayerId(ds,n), None)

  def apply(ds: String, n: String,re: RasterExtent):LoadRaster =
    LoadRaster(LayerId(ds,n), Some(re))
}

/**
 * Load the raster data for a particular extent/resolution for the 
 * raster layer in the catalog with name 'n'
 */
case class LoadRaster(layerId:Op[LayerId],
                      r:Op[Option[RasterExtent]]) extends Op[Raster] {
  def _run() = runAsync(List(layerId, r))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: (re:Option[_]) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(layerId)
                   .getRaster(re.asInstanceOf[Option[RasterExtent]])
      }
  }
}
