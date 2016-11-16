/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.ascii

import geotrellis.raster._
import geotrellis.raster.io._

class AsciiReader(path: String, noDataValue: Int) extends FileReader(path) {
  def readStateFromPath(cellType: CellType, 
                        rasterExtent: RasterExtent,
                        targetExtent: RasterExtent): ReadState = {
    new AsciiReadState(path, rasterExtent, targetExtent, noDataValue)
  }

  def readStateFromCache(b: Array[Byte], 
                         cellType: CellType,
                         rasterExtent: RasterExtent,
                         targetExtent: RasterExtent) = 
    sys.error("caching ascii grid is not supported")
}
