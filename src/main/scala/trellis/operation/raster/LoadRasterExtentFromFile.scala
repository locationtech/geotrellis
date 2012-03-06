package geotrellis.operation

import geotrellis.RasterExtent
import geotrellis.process._
import geotrellis.process._

/**
  * Load the [[geotrellis.geoattrs.RasterExtent]] from the raster in the specified file.
  */
case class LoadRasterExtentFromFile(path:String) extends Op1(path)({
  (path) => {
    val i = path.lastIndexOf(".")
    val jsonPath = (if (i == -1) path else path.substring(0, i)) + ".json"
    val layer = RasterLayer.fromPath(jsonPath)
    Result(layer.rasterExtent)
  }
})

//object LoadRasterExtentFromFile {
//  def apply(path:String) = LoadRasterExtentFromFile(path)
//}
