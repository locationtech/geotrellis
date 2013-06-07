package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.raster.IntConstant

import java.io.File

abstract class FileReader(val path:String) {
  def readStateFromCache(bytes:Array[Byte], 
                         rasterType:RasterType, 
                         rasterExtent:RasterExtent,
                         targetExtent:RasterExtent):ReadState

  def readStateFromPath(rasterType:RasterType, 
                        rasterExtent:RasterExtent,
                        targetExtent:RasterExtent):ReadState

  def readPath(rasterType:RasterType,
               rasterExtent:RasterExtent,
               targetExtent:Option[RasterExtent]):Raster =
    readPath(rasterType, rasterExtent, targetExtent.getOrElse(rasterExtent))

  def readPath(rasterType:RasterType, 
               rasterExtent:RasterExtent, 
               target:RasterExtent): Raster = 
    readRaster(readStateFromPath(rasterType, 
                                 rasterExtent,
                                 target))

  def readCache(bytes:Array[Byte], 
                rasterType:RasterType, 
                rasterExtent:RasterExtent, 
                targetExtent:Option[RasterExtent]): Raster = 
    readCache(bytes,rasterType,rasterExtent,targetExtent.getOrElse(rasterExtent))

  def readCache(bytes:Array[Byte], 
                rasterType:RasterType, 
                rasterExtent:RasterExtent, 
                targetExtent:RasterExtent): Raster = 
    readRaster(readStateFromCache(bytes, rasterType, rasterExtent, targetExtent))

  private def readRaster(readState:ReadState) = 
    try {
      readState.loadRaster()
    } finally {
      readState.destroy()
    }
}
