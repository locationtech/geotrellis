package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.raster.IntConstant

import java.io.File

abstract class FileReader(val path:String) {
  def metadataPath = path.substring(0, path.lastIndexOf(".")) + ".json"

  def readMetadata() = RasterLayer.fromPath(metadataPath)

  def readStateFromCache(bytes:Array[Byte], layer:RasterLayer, target:RasterExtent):ReadState

  def readCache(bytes:Array[Byte], layer:RasterLayer, targetOpt:Option[RasterExtent]): Raster = {
    val target = targetOpt.getOrElse(layer.info.rasterExtent)
    val readState = readStateFromCache(bytes, layer, target)
    val raster = readState.loadRaster() // all the work is here
    readState.destroy()
    raster
  }

  def readStateFromPath(rasterType:RasterType, 
                        rasterExtent:RasterExtent,
                        targetExtent:RasterExtent):ReadState

  def readPath(rasterType:RasterType, 
               rasterExtent:RasterExtent, 
               target:RasterExtent): Raster = {
    val readState = readStateFromPath(rasterType, 
                                      rasterExtent,
                                      target)
    val raster = readState.loadRaster() // all the work is here
    readState.destroy()
    raster
  }

  def readPath(layerOpt:Option[RasterLayer], targetOpt:Option[RasterExtent]): Raster = {
    val layer:RasterLayer = layerOpt match {
      case Some(l) => l
      case None =>
        readMetadata() match {
          case Some(l) => l
          case None =>
            sys.error(s"FileReader could not read file at $path, not a valid raster layer metadata file.")
        }
    }
    val target = targetOpt.getOrElse(layer.info.rasterExtent)
    readPath(layer.info.rasterType,layer.info.rasterExtent,target)
  }
}
