package geotrellis.process

import geotrellis._
import geotrellis.raster._
import geotrellis.util._
import geotrellis.data.arg.ArgReader

import com.typesafe.config.Config

import java.io.File

object ArgFileRasterLayerBuilder
extends RasterLayerBuilder {
  def apply(ds:Option[String],jsonPath:String, json:Config):Option[RasterLayer] = {
    val f = 
      if(json.hasPath("path")) {
        val f = new File(json.getString("path"))
        if(f.isAbsolute) {
          f
        } else {
          new File(new File(jsonPath).getParent, f.getPath)
        }
      } else {
        // Default to a .arg file with the same name as the layer name.
        new File(new File(jsonPath).getParent, getName(json) + ".arg")
      }

    if(!f.exists) {
      System.err.println(s"[ERROR] Raster in catalog points to path ${f.getAbsolutePath}, but file does not exist")
      System.err.println("[ERROR]   Skipping this raster layer...")
      None
    } else {

      val cols = json.getInt("cols")
      val rows = json.getInt("rows")

      val (cellWidth,cellHeight) = getCellWidthAndHeight(json)
      val rasterExtent = RasterExtent(getExtent(json), cellWidth, cellHeight, cols, rows)

      val info = 
        RasterLayerInfo(
          LayerId(ds,getName(json)),
          getRasterType(json),
          rasterExtent,
          getEpsg(json),
          getXskew(json),
          getYskew(json),
          getCacheFlag(json)
        )

      Some(new ArgFileRasterLayer(info,f.getAbsolutePath))
    }
  }
}

class ArgFileRasterLayer(info:RasterLayerInfo, val rasterPath:String) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    val data =
      if(isCached) {
        getCache.lookup[Array[Byte]](info.id.toString) match {
          case Some(bytes) =>
            RasterData.fromArrayByte(bytes,info.rasterType,info.rasterExtent.cols,info.rasterExtent.rows)
          case None =>
            sys.error("Cache problem: Layer thinks it's cached but it is in fact not cached.")
        }
      } else {
        ArgReader.readData(rasterPath, info.rasterType, info.rasterExtent)
      }
    targetExtent match {
      case Some(re) =>
        Raster(data.warp(info.rasterExtent,re),re)
      case None =>
        Raster(data,info.rasterExtent)
    }
  }

  def cache(c:Cache[String]) = 
        c.insert(info.id.toString, Filesystem.slurp(rasterPath))
}
