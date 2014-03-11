/***
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
 ***/

package geotrellis.process

import geotrellis.process._
import geotrellis.util.Filesystem

import scala.util._

/** LayerLoader will be passed into Operation[T]'s that
  * mix in the LayerOp trait right before 'run' is called
  * on the operation step and cleared afterwards.
  */
class LayerLoader(server:Server) {
  /**
   * Clients can call the raster path loading functions
   * with either the .json metadata (prefered) or with
   * the .arg extension. This function moves the latter
   * path into the metadata path.
   */
  def processPath(path:String):String =
    if(path.endsWith(".arg")) {
      path.substring(0,path.length - 4) + ".json"
    } else if (path.endsWith("/")) {
      path.substring(0,path.length - 1) + ".json"
    } else if (!path.endsWith(".json")){
      path + ".json"
    } else {
      path
    }

  /**
   * Load RasterLayer from the catalog.
   */
  def getRasterLayer(layerId:LayerId): RasterLayer =
    server.catalog.getRasterLayer(layerId) match {
      case Success(layer) => layer
      case Failure(e) => throw e
    }

  /**
   * Load RasterLayer from the catalog from a path.
   */
  def getRasterLayerFromPath(path:String):RasterLayer =
    RasterLayer.fromPath(processPath(path)) match {
      case Success(layer) => layer
      case Failure(e) => throw e
    }

  /**
   * Load RasterLayer from the catalog from a url.
   */
  def getRasterLayerFromUrl(url:String):RasterLayer = 
    RasterLayer.fromUrl(url) match {
      case Success(layer) => layer
      case Failure(e) => throw e
    }
}
