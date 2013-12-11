package geotrellis.data.arg

import geotrellis._
import geotrellis.data._
import geotrellis.raster._
import geotrellis.process._

object ArgReader {
  def read(path:String,typ:RasterType,rasterExtent:RasterExtent) =
    Raster(readData(path,typ,rasterExtent),rasterExtent)

  def readData(path:String,typ:RasterType,rasterExtent:RasterExtent) = {
    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    RasterData.fromArrayByte(util.Filesystem.slurp(path),typ,cols,rows)
  }
}
