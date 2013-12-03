package geotrellis.io

import geotrellis.process._
import geotrellis._

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
case class LoadFile(p:Op[String]) extends Operation[Raster] {
  def _run() = runAsync(List(p))
  val nextSteps:Steps = {
    case (path:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).getRaster
      }
  }
}

/**
 * Load the raster data from the specified file, using the RasterExtent provided.
 */
case class LoadFileWithRasterExtent(p:Op[String], e:Op[RasterExtent]) extends Operation[Raster] {
  def _run() = runAsync(List(p,e))
  val nextSteps:Steps = {
    case (path:String) :: (re:RasterExtent) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).getRaster(Some(re))
      }
  }
}

object LoadFile {
  def apply(p:Op[String], e:Op[RasterExtent]) = LoadFileWithRasterExtent(p, e)
}
