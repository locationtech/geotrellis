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

package geotrellis.io

import geotrellis.process._
import geotrellis._

/**
 * Load the raster data for a particular extent/resolution from the specified file.
 */
case class LoadFile(p:Op[String]) extends Operation[Raster] {
  def _run() = runAsync(List(p))
  val nextSteps:Steps = {
    case (path:String) :: Nil =>
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).getRaster
      }
  }
}

/**
 * Load the raster data from the specified file, using the RasterExtent provided.
 */
case class LoadFileWithRasterExtent(p:Op[String], e:Op[RasterExtent]) extends Operation[Raster] {
  def _run() = runAsync(List(p,e))
  val nextSteps:Steps = {
    case (path:String) :: (re:RasterExtent) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).getRaster(Some(re))
      }
  }
}

object LoadFile {
  def apply(p:Op[String], e:Op[RasterExtent]) = LoadFileWithRasterExtent(p, e)
}
