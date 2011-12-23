package trellis.operation

import trellis.RasterExtent
import trellis.process.Server
import trellis.process.catalog._

/**
  * Load the [[trellis.geoattrs.RasterExtent]] from the raster in the specified file.
  */
class LoadRasterExtentFromFile(path:String) extends RasterExtentOperation with SimpleOperation[RasterExtent] { 
  def childOperations = List.empty[Operation[_]]

  def _value(server:Server) = {
    //val rdr = server.getReader(path)
    //rdr.readMetadata
    //rdr.getRasterExtent()
    val i = path.lastIndexOf(".")
    val jsonPath = path.substring(0, i) + ".json"
    val layer = RasterLayer.fromPath(jsonPath)
    layer.rasterExtent
  }
}

object LoadRasterExtentFromFile {
  def apply(path:String) = new LoadRasterExtentFromFile(path)
}
