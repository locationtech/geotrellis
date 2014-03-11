/**************************************************************************
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
 **************************************************************************/

package geotrellis.data.arg

import java.io.{BufferedOutputStream, DataOutputStream, FileOutputStream}

import geotrellis._
import geotrellis.data._
import geotrellis.util._
import geotrellis.process._

/**
 * ArgWriter will write a raster to disk in ARG format.
 * 
 * Each instance of ArgWriter is provided a data type (e.g. int or float) to 
 * use for output files.
 */
case class ArgWriter(typ:RasterType) extends Writer {
  def width = typ.bits / 8
  def rasterType = typ.name
  def dataType = "arg"

  /**
   * Write a raster in ARG format to the specified path.
   *
   * The outputFilePath argument should be the full file path (including file name and extension)
   * to which the raster file should be written.  For example, "/var/data/geotrellis/myraster.arg".
   *
   * The metadataName argument is a logical name for the raster that will be included in the raster's
   * metadata.  It does not have to match the output filename.  This filename is used by the catalog
   * when a raster is loaded by name as opposed to filepath.
   *
   * @param outputFilePath: File path to which to write out arg file (can include .arg extension)
   * @param raster: Raster to write to disk
   * @param metadataName: Name to be included in json metadata as 'layer', used in catalog
   */
  def write(outputFilePath:String, raster:Raster, metadataName:String) {
    val path = outputFilePath
    val base:String = if (path.endsWith(".arg") || path.endsWith(".json") || path.endsWith(".")) {
      path.substring(0, path.lastIndexOf("."))
    } else {
      path 
    }
    writeMetadataJSON(base + ".json", metadataName, raster.rasterExtent)
    writeData(base + ".arg", raster)
  }

  private def writeBytes(data:RasterData, cols:Int, rows:Int, dos:DataOutputStream) {
    var row = 0
    while (row < rows) {
      var col = 0
      while (col < cols) {
        dos.writeByte(data.get(col, row))
        col += 1
      }
      row += 1
    }
  }

  private def writeData(path:String, raster:Raster) {
    val dos = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(path)))
    CellWriter.byType(typ).writeCells(raster, dos)
    dos.close()
  }
}
