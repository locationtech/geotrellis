/**************************************************************************
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **************************************************************************/

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

  def apply(ds:Option[String], jsonPath:String, json:Config):RasterLayer = {
    val path = 
      if(json.hasPath("path")) {
        new File(new File(jsonPath).getParentFile, json.getString("path")).getPath
      } else {
        Filesystem.basename(jsonPath) + ".tif"
      }

    if(!new File(path).exists) {
      throw new java.io.IOException(s"[ERROR] Raster in catalog points to path $path, but file does not exist" +
                                     "[ERROR]   Skipping this raster layer...")
    } else {
      val rasterExtent = GeoTiff.loadRasterExtent(path)

      // Info should really come from the GeoTiff
      val info = 
        RasterLayerInfo(
          LayerId(ds,getName(json)),
          getRasterType(json),
          rasterExtent,
          getEpsg(json),
          getXskew(json),
          getYskew(json)
        )

      new GeoTiffRasterLayer(info,path)
    }
  }

  def fromTif(path:String):GeoTiffRasterLayer = {
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
    val info = 
      RasterLayerInfo(
        LayerId(name),
        rasterType,
        rasterExtent,
        epsg,
        0,
        0
      )

    new GeoTiffRasterLayer(info,path)
  }
}

class GeoTiffRasterLayer(info:RasterLayerInfo, rasterPath:String) 
extends UntiledRasterLayer(info) {
  def getRaster(targetExtent:Option[RasterExtent]) = {
    new GeoTiffReader(rasterPath).readPath(info.rasterType,info.rasterExtent,targetExtent)
  }

  def cache(c:Cache[String]) = { } // TODO: implement
}
