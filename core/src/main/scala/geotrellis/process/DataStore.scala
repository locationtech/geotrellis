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

package geotrellis.process

import scala.collection.mutable
import java.io.File
import geotrellis._
import geotrellis.util.Filesystem

import scala.util._

/**
 * Represents a location where data can be loaded from (e.g. the filesystem,
 * postgis, a web service, etc).
 */
case class DataStore(name:String, path:String, hasCacheAll:Boolean) {

  private val layers = mutable.Map.empty[String, RasterLayer]

  initRasterLayers()

  /**
   * Initialize raster layers from the absolute directory specified by the 'path' param.
   */
  private def initRasterLayers() {
    val f = new File(path)

    if(!f.exists) {
      sys.error(s"Invalid DataStore path $path: path does not exist.")
    }

    // Walk the directory to for raster layers;
    // also search subdirectories, but some directories
    // might be tiled rasters.
    initDirectory(f)
  }

  private def initDirectory(d:File) {
    val skipDirectories = mutable.Set[String]()
    for(f <- d.listFiles
              .filter(_.isFile)
              .filter(_.getPath.endsWith(".json"))) {
      // It's a JSON file
      // which may contain layer metadata,
      // or we just ignore it.
      RasterLayer.fromFile(f) match {
        case Success(layer) =>
          layers(layer.info.id.name) = layer
          // Skip the tile directory if it's a tiled raster.
          layer match {
            case tl:TileSetRasterLayer =>
              skipDirectories.add(new File(tl.tileDirPath).getAbsolutePath)
            case _ =>
          }
        case Failure(e) =>
          System.err.println(s"[ERROR] Skipping ${f.getPath}: $e")
      }
    }

    // Recurse through subdirectories. If a directory was marked
    // as containing a tile set, skip it.
    for(subdir <- d.listFiles
                   .filter(_.isDirectory)
                   .filter(f => !skipDirectories.contains(f.getAbsolutePath))) {
      initDirectory(subdir)
    }
  }

  /**
   * Sets the cache for all child layers.
   */
  def setCache(c:Option[Cache[String]]) = {
    for(layer <- layers.values) { layer.setCache(c) }
  }

  def cacheAll() =
    layers.values
          .map(_.cache)

  def getNames = layers.keys
  def getLayers = layers.values

  def getRasterLayer(name:String):Option[RasterLayer] = layers.get(name)
}
