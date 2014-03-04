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

object LoadRaster {
  def apply(n: String):LoadRaster =
    LoadRaster(LayerId(n), None)

  def apply(n: String,re: RasterExtent):LoadRaster =
    LoadRaster(LayerId(n), Some(re))

  def apply(ds: String, n: String): LoadRaster =
    LoadRaster(LayerId(ds,n), None)

  def apply(ds: String, n: String,re: RasterExtent):LoadRaster =
    LoadRaster(LayerId(ds,n), Some(re))
}

/**
 * Load the raster data for a particular extent/resolution for the 
 * raster layer in the catalog with name 'n'
 */
case class LoadRaster(layerId:Op[LayerId],
                      r:Op[Option[RasterExtent]]) extends Op[Raster] {
  def _run() = runAsync(List(layerId, r))
  val nextSteps:Steps = {
    case (layerId:LayerId) :: (re:Option[_]) :: Nil => 
      LayerResult { layerLoader =>
        layerLoader.getRasterLayer(layerId)
                   .getRaster(re.asInstanceOf[Option[RasterExtent]])
      }
  }
}
