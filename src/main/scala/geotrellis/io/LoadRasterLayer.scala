package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterLayer {
  def apply(n:Op[String]): LoadRasterLayer =
    LoadRasterLayer(None,n)

  def apply(ds:String, n:Op[String]): LoadRasterLayer =
    LoadRasterLayer(Some(ds), n)
}

/**
  * Load the [[RasterLayer]] from the raster layer with the specified name.
  */
case class LoadRasterLayer(ds: Op[Option[String]], n:Op[String]) extends Op[RasterLayer] {
  def _run() = runAsync(List(ds, n))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: (n:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]], n)
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
