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
import geotrellis.feature.Extent

import java.io.{BufferedOutputStream, FileOutputStream}
import java.nio.ByteBuffer
import java.nio.channels.FileChannel.MapMode._

trait Writer {
  def write(path: String, raster: Tile, extent: Extent, name: String): Unit

  def cellType: String
  def dataType: String

  def writeMetadataJSON(path: String, name: String, re: RasterExtent) {
    val metadata = s"""{
        |  "layer": "$name",
        |  "datatype": "$cellType",
        |  "type": "$dataType",
        |  "xmin": ${re.extent.xmin},
        |  "xmax": ${re.extent.xmax},
        |  "ymin": ${re.extent.ymin},
        |  "ymax": ${re.extent.ymax},
        |  "cols": ${re.cols},
        |  "rows": ${re.rows},
        |  "cellwidth": ${re.cellwidth},
        |  "cellheight": ${re.cellheight},
        |  "epsg": 3785,
        |  "yskew": 0.0,
        |  "xskew": 0.0
        |}""".stripMargin

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}
