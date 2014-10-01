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

package geotrellis.raster.io.ascii

import geotrellis.raster._

import java.io.File

abstract class FileReader(val path: String) {
  def readStateFromCache(bytes: Array[Byte], 
                         cellType: CellType, 
                         rasterExtent: RasterExtent,
                         targetExtent: RasterExtent): ReadState

  def readStateFromPath(cellType: CellType, 
                        rasterExtent: RasterExtent,
                        targetExtent: RasterExtent): ReadState

  def readPath(cellType: CellType,
               rasterExtent: RasterExtent,
               targetExtent: Option[RasterExtent]): Tile =
    readPath(cellType, rasterExtent, targetExtent.getOrElse(rasterExtent))

  def readPath(cellType: CellType, 
               rasterExtent: RasterExtent, 
               target: RasterExtent): Tile = 
    readRaster(readStateFromPath(cellType, 
                                 rasterExtent,
                                 target))

  def readCache(bytes: Array[Byte], 
                cellType: CellType, 
                rasterExtent: RasterExtent, 
                targetExtent: Option[RasterExtent]): Tile = 
    readCache(bytes, cellType, rasterExtent, targetExtent.getOrElse(rasterExtent))

  def readCache(bytes: Array[Byte], 
                cellType: CellType, 
                rasterExtent: RasterExtent, 
                targetExtent: RasterExtent): Tile = 
    readRaster(readStateFromCache(bytes, cellType, rasterExtent, targetExtent))

  private def readRaster(readState: ReadState) = 
    try {
      readState.loadRaster()
    } finally {
      readState.destroy()
    }
}
