/*******************************************************************************
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
 ******************************************************************************/

package geotrellis.io

import geotrellis._
import geotrellis.process._

object LoadRasterExtent {
  def apply(name:String): LoadRasterExtent = 
    LoadRasterExtent(LayerId(name))

  def apply(ds:String, name:String): LoadRasterExtent = 
    LoadRasterExtent(LayerId(ds,name))
}

/** Load the [[RasterExtent]] from the raster layer with the specified name.
  */
case class LoadRasterExtent(layerId:Op[LayerId]) extends Op[RasterExtent] {
  def _run() = runAsync(List(layerId))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(layerId).info.rasterExtent
      }
  }
}

/** Load the [[RasterExtent]] from the raster in the specified file.
  */
case class LoadRasterExtentFromFile(path:Op[String]) extends Op[RasterExtent] {
  def _run() = runAsync(List(path))
  val nextSteps:Steps = {
    case (path:String) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayerFromPath(path).info.rasterExtent
      }
  }
}
