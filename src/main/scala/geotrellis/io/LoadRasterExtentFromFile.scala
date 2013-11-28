package geotrellis.io

import geotrellis._
import geotrellis.process._

/** Load the [[RasterExtent]] from the raster in the specified file.
  */
case class LoadRasterExtentFromFile(path:Op[String]) extends Op[RasterExtent] {
  def _run(context:Context) = runAsync(List(path, context))
  val nextSteps:Steps = {
    case (path:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayerFromPath(path).info.rasterExtent)
    }
  }
}

object LoadRasterExtent {
  def apply(name:Op[String]): LoadRasterExtent = 
    LoadRasterExtent(None,name)

  def apply(ds:String, name:Op[String]): LoadRasterExtent = 
    LoadRasterExtent(Some(ds),name)
}

/** Load the [[RasterExtent]] from the raster layer with the specified name.
  */
case class LoadRasterExtent(ds:Op[Option[String]], nme:Op[String]) extends Op[RasterExtent] {
  def _run(context:Context) = runAsync(List(ds, nme, context))
  val nextSteps:Steps = {
    case (ds:Option[String]) :: (name:String) :: (context:Context) :: Nil => {
      Result(context.getRasterLayer(ds,name).info.rasterExtent)
    }
  }
}
