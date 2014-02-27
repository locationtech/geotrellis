package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterUrl {
  def apply(url:Op[String]):LoadRasterUrl = LoadRasterUrl(url,None)
}

/**
 * Load the raster from JSON metadata recieved from a URL
 */
case class LoadRasterUrl(url:Op[String],re:Op[Option[RasterExtent]]) extends Operation[Raster] {
  def _run() = runAsync(List(url,re))
  val nextSteps:Steps = {
    case (url:String) :: (re:Option[_]) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader
          .getRasterLayerFromUrl(url)
          .getRaster(re.asInstanceOf[Option[RasterExtent]])
      }
  }
}
