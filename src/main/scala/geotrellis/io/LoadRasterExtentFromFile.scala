package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterExtent {
  def apply(name:Op[String]): LoadRasterExtent = 
    LoadRasterExtent(None,name)

  def apply(ds:String, name:Op[String]): LoadRasterExtent = 
    LoadRasterExtent(Some(ds),name)
}

/** Load the [[RasterExtent]] from the raster in the specified file.
  */
case class LoadRasterExtentFromFile(path:Op[String]) extends Op[RasterExtent] {
  def _run() = runAsync(List(path))
  val nextSteps:Steps = {
    case (path:String) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).info.rasterExtent
      }
  }
}

/** Load the [[RasterExtent]] from the raster layer with the specified name.
  */
case class LoadRasterExtent(ds:Op[Option[String]], nme:Op[String]) extends Op[RasterExtent] {
  def _run() = runAsync(List(ds, nme))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: (name:String) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]],name).info.rasterExtent
      }
  }
}
