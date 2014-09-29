package geotrellis.raster.io.ascii

import geotrellis.raster._
import geotrellis.raster.io._

class AsciiReader(path: String, noDataValue: Int) extends FileReader(path) {
  def readStateFromPath(cellType: CellType, 
                        rasterExtent: RasterExtent,
                        targetExtent: RasterExtent): ReadState = {
    new AsciiReadState(path, rasterExtent, targetExtent, noDataValue)
  }

  def readStateFromCache(b: Array[Byte], 
                         cellType: CellType,
                         rasterExtent: RasterExtent,
                         targetExtent: RasterExtent) = 
    sys.error("caching ascii grid is not supported")
}
