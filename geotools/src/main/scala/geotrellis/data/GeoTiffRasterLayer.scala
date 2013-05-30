package geotrellis.data

import geotrellis._
import geotrellis.process._
import geotrellis.util._

import com.typesafe.config.Config

import java.io.File

import org.geotools.factory.Hints
import org.geotools.gce
import org.geotools.referencing.CRS

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

    if(!new File(path).exists) {
      System.err.println(s"[ERROR] Raster in catalog points to path $path, but file does not exist")
      System.err.println( "[ERROR]   Skipping this raster layer...")
      None
    } else {
      val rasterExtent = GeoTiff.loadRasterExtent(path)

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

  def fromTif(path:String, cache:Option[Cache] = None):GeoTiffRasterLayer = {
    val f = new File(path)
    if(!f.exists) {
      sys.error(s"File $path does not exist")
    }

    val reader = GeoTiff.getReader(path)
    val cov = GeoTiff.getGridCoverage2D(reader)
    val epsgLookup = CRS.lookupEpsgCode(cov.getCoordinateReferenceSystem,true)
    val epsg =
      if(epsgLookup != null) { epsgLookup.toInt }
      else { 0 }

    val rasterExtent = GeoTiff.loadRasterExtent(cov)
    val rasterType = GeoTiff.getDataType(cov)
    val name = Filesystem.basename(f.getName)

    // Info should really come from the GeoTiff
    val info = RasterLayerInfo(name,
      rasterType,
      rasterExtent,
      epsg,
      0,
      0)

    new GeoTiffRasterLayer(info,path,cache)
  }
}

class GeoTiffRasterLayer(info:RasterLayerInfo, rasterPath:String, c:Option[Cache]) 
extends RasterLayer(info,c) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    new GeoTiffReader(rasterPath).readPath(info.rasterType,info.rasterExtent,targetExtent)
  }

  def cache() = { } // TODO: implement
}
