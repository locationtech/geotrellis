package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRaster {
  def apply(n:Op[String]):LoadRaster =
    LoadRaster(None, n, None)

  def apply(n:Op[String],re:RasterExtent):LoadRaster =
    LoadRaster(None, n, Some(re))

  def apply(ds:String, n: Op[String]): LoadRaster =
    LoadRaster(Some(ds), n, None)

  def apply(ds:String, n:Op[String],re:RasterExtent):LoadRaster =
    LoadRaster(Some(ds), n, Some(re))
}

/**
 * Load the raster data for a particular extent/resolution for the 
 * raster layer in the catalog with name 'n'
 */
case class LoadRaster(ds:Op[Option[String]], 
                      n:Op[String], 
                      r:Op[Option[RasterExtent]]) extends Op[Raster] {
  def _run() = runAsync(List(ds, n, r))
  val nextSteps:Steps = {
    case (ds:Option[_]) :: (name:String) :: (re:Option[_]) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(ds.asInstanceOf[Option[String]],name)
                   .getRaster(re.asInstanceOf[Option[RasterExtent]])
      }
  }
}
