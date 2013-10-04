package geotrellis.process

import geotrellis._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import dispatch.classic._

import com.typesafe.config.Config

object ArgUrlRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config):Option[RasterLayer] = {
    val url = 
      if(json.hasPath("url")) {
        json.getString("url")
      } else {
        System.err.println(s"[ERROR] 'argurl' type rasters must have 'url' field in json.")
        System.err.println("[ERROR]   Skipping this raster layer...")
        return None
      }

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

    Some(new ArgUrlRasterLayer(info,url))
  }
}

class ArgUrlRasterLayer(info:RasterLayerInfo, rasterUrl:String) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) =
    if(isCached) {
      getCache.lookup[Array[Byte]](info.name) match {
        case Some(bytes) =>
          getReader.readCache(bytes, info.rasterType, info.rasterExtent, targetExtent)
        case None =>
          sys.error("Cache problem: Layer thinks it's cached but it is in fact not cached.")
      }
    } else {
      getReader.readCache(getBytes,
                          info.rasterType,
                          info.rasterExtent, targetExtent)
    }

  def getBytes = {
    val size = info.rasterExtent.cols * info.rasterExtent.rows * (info.rasterType.bits / 8)
    val result = Array.ofDim[Byte](size)
    val h = new Http()
    h(url(rasterUrl) >>  { (stream,charset) =>
      var bytesRead = 1
      while (bytesRead > 0) { 
        bytesRead = stream.read(result, bytesRead-1, size)
      }
    })
    h.shutdown
    result
  }

  def cache(c:Cache) = 
        c.insert(info.name, getBytes)

  private def getReader = new ArgReader("")
}

