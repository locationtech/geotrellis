package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.util._

import com.typesafe.config.Config

import java.io.File

object GeoTiffRasterLayerBuilder
extends RasterLayerBuilder {
  def addToCatalog() = {
    Catalog.addRasterLayerBuilder("geotiff", GeoTiffRasterLayerBuilder)
  }

  def apply(jsonPath:String, json:Config, cache:Option[Cache]):Option[RasterLayer] = {
    val path = 
      if(json.hasPath("path")) {
        new File(new File(jsonPath).getParentFile, json.getString("path")).getPath
      } else {
        Filesystem.basename(jsonPath) + ".tif"
      }

    if(!new java.io.File(path).exists) {
      System.err.println(s"[ERROR] Raster in catalog points to path $path, but file does not exist")
      System.err.println("[ERROR]   Skipping this raster layer...")
      None
    } else {


      val rasterExtent = new GeoTiffReader(path).loadRasterExtent()

      // Info should really come from the GeoTiff
      val info = RasterLayerInfo(getName(json),
                                 getRasterType(json),
                                 rasterExtent,
                                 getEpsg(json),
                                 getXskew(json),
                                 getYskew(json))

      Some(new GeoTiffRasterLayer(info,path,cache))
    }
  }
}

class GeoTiffRasterLayer(info:RasterLayerInfo, rasterPath:String, c:Option[Cache]) 
extends RasterLayer(info,c) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    new GeoTiffReader(rasterPath).readPath(Some(this), targetExtent)
  }

  def cache() = { } // TODO: implement
}
