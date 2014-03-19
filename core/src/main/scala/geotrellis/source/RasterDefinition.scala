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

package geotrellis.source

import geotrellis._
import geotrellis.process.LayerId
import geotrellis.raster.TileLayout

case class RasterDefinition(layerId:LayerId,
                            rasterExtent:RasterExtent,
                            tileLayout:TileLayout,
                            rasterType:RasterType,
                            catalogued:Boolean = true) {
  def isTiled = tileLayout.isTiled

  def withType(newType: RasterType) =
    new RasterDefinition(layerId, rasterExtent, tileLayout, newType)
}

object RasterDefinition {

  def fromRaster(r: Raster): RasterDefinition = 
    RasterDefinition(
      LayerId.MEM_RASTER,
      r.rasterExtent,
      TileLayout.singleTile(r.rasterExtent),
      r.rasterType,
      false)
}
