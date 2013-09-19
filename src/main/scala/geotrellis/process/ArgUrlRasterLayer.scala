package geotrellis.process

import geotrellis._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import com.typesafe.config.Config

object ArgUrlRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config):Option[RasterLayer] = {
    val path = 
      if(json.hasPath("path")) {
        json.getString("path")
      } else {
        Filesystem.basename(jsonPath) + ".arg"
      }

    if(!new java.io.File(path).exists) {
      System.err.println(s"[ERROR] Raster in catalog points to path $path, but file does not exist")
      System.err.println("[ERROR]   Skipping this raster layer...")
      None
    } else {

      val cols = json.getInt("cols")
      val rows = json.getInt("rows")

      val (cellWidth,cellHeight) = getCellWidthAndHeight(json)
      val rasterExtent = RasterExtent(getExtent(json), cellWidth, cellHeight, cols, rows)

      val info = RasterLayerInfo(getName(json),
        getRasterType(json),
        rasterExtent,
        getEpsg(json),
        getXskew(json),
        getYskew(json),
        getCacheFlag(json))

      Some(new ArgUrlRasterLayer(info,path))
    }
  }
}

class ArgUrlRasterLayer(info:RasterLayerInfo, rasterPath:String) 
extends RasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) =
    if(isCached) {
      getCache.lookup[Array[Byte]](info.name) match {
        case Some(bytes) =>
          getReader.readCache(bytes, info.rasterType, info.rasterExtent, targetExtent)
        case None =>
          sys.error("Cache problem: Layer thinks it's cached but it is in fact not cached.")
      }
    } else {
      getReader.readPath(info.rasterType, info.rasterExtent, targetExtent)
    }

  def cache(c:Cache) = 
        c.insert(info.name, Filesystem.slurp(rasterPath))

  private def getReader = new ArgReader(rasterPath)
}

