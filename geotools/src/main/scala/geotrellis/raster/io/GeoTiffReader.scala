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

package geotrellis.raster.io

import geotrellis.raster._

import org.geotools.referencing.CRS

class GeoTiffReader(path: String) extends FileReader(path) {
  def readStateFromPath(cellType: CellType, 
                        rasterExtent: RasterExtent, targetExtent: RasterExtent) = {
    val reader = GeoTiff.getReader(path)
    val cellType = GeoTiff.getDataType(reader)

    if(cellType.isFloatingPoint) {
      new GeoTiffDoubleReadState(path, rasterExtent, targetExtent, cellType, reader)
    } else {
      new GeoTiffIntReadState(path, rasterExtent, targetExtent, cellType, reader)
    }
  }

  def readStateFromCache(b: Array[Byte], 
                         cellType: CellType,
                         rasterExtent: RasterExtent,
                         targetExtent: RasterExtent) = 
    sys.error("caching geotif not supported")
}
