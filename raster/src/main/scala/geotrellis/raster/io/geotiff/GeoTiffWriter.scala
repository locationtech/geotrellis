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

package geotrellis.raster.io.geotiff

import geotrellis.raster._
import geotrellis.raster.io._
import geotrellis.vector.Extent
import geotrellis.proj4.CRS

object GeoTiffWriter {

  val cellType = "geotiff"
  val dataType = ""

  def write(path: String, tile: Tile, extent: Extent, crs: CRS) {
    Encoder.writePath(
      path,
      tile,
      RasterExtent(extent, tile.cols, tile.rows),
      crs,
      settings(tile.cellType)
    )
  }

  def write(path: String, tile: Tile, extent: Extent, crs: CRS, nodata: Double) {
    Encoder.writePath(
      path,
      tile,
      RasterExtent(extent, tile.cols, tile.rows),
      crs,
      settings(tile.cellType).setNodata(nodata)
    )
  }

  private def settings(cellType: CellType) = cellType match {
    case TypeBit | TypeByte => Settings.int8
    case TypeShort => Settings.int16
    case TypeInt => Settings.int32
    case TypeFloat => Settings.float32
    case TypeDouble => Settings.float64
  }

}
