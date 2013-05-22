package geotrellis.process

import geotrellis._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import com.typesafe.config.Config

object ArgFileRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config, cache:Option[Cache]):Option[RasterLayer] = {
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
        getYskew(json))

      Some(new ArgFileRasterLayer(info,path,cache))
    }
  }
}

class ArgFileRasterLayer(info:RasterLayerInfo, rasterPath:String, c:Option[Cache]) 
extends RasterLayer(info,c) {
  private var cached = false

  def getRaster(targetExtent:Option[RasterExtent] = None) =
    if(cached) {
      c.get.lookup[Array[Byte]](info.name) match {
        case Some(bytes) =>
          getReader.readCache(bytes, this, targetExtent)
        case None =>
          sys.error("Cache problem: Layer things it's cached but it is in fact not cached.")
      }
    } else {
      getReader.readPath(Some(this), targetExtent)
    }

  def cache = 
    c match {
      case Some(cch) =>
        cch.insert(info.name, Filesystem.slurp(rasterPath))
        cached = true
      case None => //do nothing
    }

  private def getReader = new ArgReader(rasterPath)
}

