package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import com.typesafe.config.Config

object AsciiRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(jsonPath:String, json:Config, cache:Option[Cache]):Option[RasterLayer] = {
    val path = 
      if(json.hasPath("path")) {
        json.getString("path")
      } else {
        val ascPath = Filesystem.basename(jsonPath) + ".asc"
        if(!new java.io.File(ascPath).exists) {
          ascPath
        } else {
          Filesystem.basename(jsonPath) + ".grd"
        }
      }

    if(!new java.io.File(path).exists) {
      System.err.println("[ERROR] Cannot find data (.asc or .grd file) for " +
                        s"Ascii Raster '${getName(json)}' in catalog.")
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

      Some(new AsciiRasterLayer(info,path,cache))
    }
  }
}

class AsciiRasterLayer(info:RasterLayerInfo, rasterPath:String, c:Option[Cache]) 
extends RasterLayer(info,c) {
  private var cached = false

  def getRaster(targetExtent:Option[RasterExtent]) =
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

  private def getReader = new AsciiReader(rasterPath)
}
