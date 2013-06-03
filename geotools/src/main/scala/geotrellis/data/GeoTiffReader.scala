package geotrellis.data

import geotrellis._
import org.geotools.referencing.CRS

class GeoTiffReader(path:String) extends FileReader(path) {
  def readStateFromPath(rasterType:RasterType, 
                        rasterExtent:RasterExtent,targetExtent:RasterExtent) = {
    val reader = GeoTiff.getReader(path)
    val rasterType= GeoTiff.getDataType(reader)

    if(rasterType.isDouble) {
      new GeoTiffDoubleReadState(path, rasterExtent, targetExtent, rasterType, reader)
    } else {
      new GeoTiffIntReadState(path, rasterExtent, targetExtent, rasterType, reader)
    }
  }

  def readStateFromCache(b:Array[Byte], 
                         rasterType:RasterType,
                         rasterExtent:RasterExtent,
                         targetExtent:RasterExtent) = 
    sys.error("caching geotif not supported")
}
