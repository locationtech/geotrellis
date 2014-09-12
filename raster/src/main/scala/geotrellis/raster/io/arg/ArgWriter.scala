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

package geotrellis.raster.io.arg

import java.io.{BufferedOutputStream, DataOutputStream, FileOutputStream}

import geotrellis.raster._
import geotrellis.vector.Extent

/**
 * ArgWriter will write a tile to disk in ARG format.
 * 
 * Each instance of ArgWriter is provided a data type (e.g. int or float) to 
 * use for output files.
 */
case class ArgWriter(typ: CellType) {
  def width = typ.bits / 8
  def cellType = typ.name
  def dataType = "arg"

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

  /**
   * Write a tile in ARG format to the specified path.
   *
   * The outputFilePath argument should be the full file path (including file name and extension)
   * to which the tile file should be written.  For example, "/var/data/geotrellis/mytile.arg".
   *
   * The metadataName argument is a logical name for the tile that will be included in the tile's
   * metadata.  It does not have to match the output filename.  This filename is used by the catalog
   * when a tile is loaded by name as opposed to filepath.
   *
   * @param outputFilePath: File path to which to write out arg file (can include .arg extension)
   * @param tile: Tile to write to disk
   * @param metadataName: Name to be included in json metadata as 'layer', used in catalog
   */
  def write(outputFilePath: String, tile: Tile, extent: Extent, metadataName: String) {
    val path = outputFilePath
    val base: String = 
      if (path.endsWith(".arg") || path.endsWith(".json") || path.endsWith(".")) {
        path.substring(0, path.lastIndexOf("."))
      } else {
        path
      }
    tile match {
      case ct: ConstantTile =>
        writeConstantTile(s"$base.json", ct, metadataName, RasterExtent(extent, tile.cols, tile.rows))
      case _ =>
        writeMetadataJSON(s"$base.json", metadataName, RasterExtent(extent, tile.cols, tile.rows))
        writeData(s"$base.arg", tile)
    }
  }

  def writeData(path: String, tile: Tile) {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
    CellWriter.byType(typ).writeCells(tile, dos)
    dos.close()
  }

  def writeConstantTile(path: String, ct: ConstantTile, name: String, re: RasterExtent): Unit = {
    val metadata = s"""{
        |  "layer": "$name",
        |  "datatype": "$cellType",
        |  "type": "constant",
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
        |  "xskew": 0.0,
        |  "constant": {ct.getDouble(0,0)}
        |}""".stripMargin

    val bos = new BufferedOutputStream(new FileOutputStream(path))
    bos.write(metadata.getBytes)
    bos.close
  }
}
