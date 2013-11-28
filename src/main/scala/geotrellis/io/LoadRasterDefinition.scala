package geotrellis.io

import geotrellis._
import geotrellis.process._
import geotrellis.source._

object LoadRasterDefinition {
  def apply(n:Op[String]):LoadRasterDefinition =
    LoadRasterDefinition(None,n)

  def apply(ds:String,n:Op[String]):LoadRasterDefinition =
    LoadRasterDefinition(Some(ds),n)
}

/**
  * Load the [[RasterDefinition]] from the raster layer with the specified name.
  */
case class LoadRasterDefinition(ds: Op[Option[String]], n: Op[String]) extends Op[RasterDefinition] {
  def _run() = runAsync(List(ds, n))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: (n:String) :: Nil => 
      LayerResult { layerLoader =>
        val info = layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]],n).info
        RasterDefinition(n,info.rasterExtent,info.tileLayout)
      }
  }
}
