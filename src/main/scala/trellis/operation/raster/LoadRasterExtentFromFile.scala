package trellis.operation

import trellis.RasterExtent
import trellis.process._
import trellis.process._

/**
  * Load the [[trellis.geoattrs.RasterExtent]] from the raster in the specified file.
  */
class LoadRasterExtentFromFile(path:String) extends Op1(path)({
  (path) => {
    val i = path.lastIndexOf(".")
    val jsonPath = (if (i == -1) path else path.substring(0, i)) + ".json"
    val layer = RasterLayer.fromPath(jsonPath)
    Result(layer.rasterExtent)
  }
})

object LoadRasterExtentFromFile {
  def apply(path:String) = new LoadRasterExtentFromFile(path)
}
