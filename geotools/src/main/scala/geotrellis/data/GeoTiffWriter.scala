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

package geotrellis.data

import geotrellis._
import geotrellis.feature.Extent
import geotrellis.data.geotiff._


/**
 * This GeoTiffWriter is deprecated.
 *
 * See geotrellis.data.geotiff.Encoder for the preferred approach to
 * encoding rasters to geotiff files.
 */
object GeoTiffWriter extends Writer {
  def rasterType = "geotiff" 
  def dataType = ""

  def write(path: String, raster: Raster, extent: Extent, name: String) {
    Encoder.writePath(path, raster, RasterExtent(extent, raster.cols, raster.rows), settings(raster.rasterType))   
  }
  
  def write(path: String, raster: Raster, extent: Extent, nodata: Double) {
    Encoder.writePath(path, raster, RasterExtent(extent, raster.cols, raster.rows), settings(raster.rasterType).setNodata(nodata))   
  }
  
  private def settings(rasterType: RasterType) = rasterType match {
      case TypeBit | TypeByte => Settings.int8
      case TypeShort => Settings.int16
      case TypeInt => Settings.int32
      case TypeFloat => Settings.float32
      case TypeDouble => Settings.float64
    }
}
