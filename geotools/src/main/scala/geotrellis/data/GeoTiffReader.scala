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

package geotrellis.data

import geotrellis._
import org.geotools.referencing.CRS

class GeoTiffReader(path:String) extends FileReader(path) {
  def readStateFromPath(rasterType:RasterType, 
                        rasterExtent:RasterExtent,targetExtent:RasterExtent) = {
    val reader = GeoTiff.getReader(path)
    val rasterType= GeoTiff.getDataType(reader)

    if(rasterType.isDouble) {
      new GeoTiffDoubleReadState(path, rasterExtent, targetExtent, rasterType, reader)
    } else {
      new GeoTiffIntReadState(path, rasterExtent, targetExtent, rasterType, reader)
    }
  }

  def readStateFromCache(b:Array[Byte], 
                         rasterType:RasterType,
                         rasterExtent:RasterExtent,
                         targetExtent:RasterExtent) = 
    sys.error("caching geotif not supported")
}
