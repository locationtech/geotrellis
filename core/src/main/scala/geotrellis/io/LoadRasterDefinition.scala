/*
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
 */

package geotrellis.io

import geotrellis._
import geotrellis.process._
import geotrellis.source._

object LoadRasterDefinition {
  def apply(n:String):LoadRasterDefinition =
    LoadRasterDefinition(LayerId(n))

  def apply(ds:String,n:String):LoadRasterDefinition =
    LoadRasterDefinition(LayerId(ds,n))
}

/**
  * Load the [[RasterDefinition]] from the raster layer with the specified name.
  */
case class LoadRasterDefinition(layerId:Op[LayerId]) extends Op[RasterDefinition] {
  def _run() = runAsync(List(layerId))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: Nil => 
      LayerResult { layerLoader =>
        val info = layerLoader.getRasterLayer(layerId).info
        RasterDefinition(layerId,info.rasterExtent,info.tileLayout,info.rasterType)
      }
  }
}
