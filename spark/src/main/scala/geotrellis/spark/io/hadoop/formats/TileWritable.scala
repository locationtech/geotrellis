/*
 * Copyright (c) 2014 DigitalGlobe.
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

package geotrellis.spark.io.hadoop.formats

import geotrellis.spark._
import geotrellis.raster._

import org.apache.hadoop.io.Writable
import java.io.{ DataInput, DataOutput }

class TileWritable() extends Writable with Serializable {
  private var _bytes: Array[Byte] = Array()

  def set(tile: Tile): Unit = 
    _bytes = tile.toBytes

  def write(out: DataOutput): Unit = {
    val size = _bytes.size
    out.writeInt(size)
    out.write(_bytes, 0, size)
  }

  def readFields(in: DataInput): Unit = {
    val size = in.readInt
    _bytes = Array.ofDim[Byte](size)
    in.readFully(_bytes, 0, size)
  }

  def toTile(metaData: RasterMetaData): Tile =
    toTile(metaData.cellType, metaData.tileLayout.tileCols, metaData.tileLayout.tileRows)

  def toTile(cellType: CellType, cols: Int, rows: Int): Tile =
    ArrayTile.fromBytes(_bytes, cellType, cols, rows)
}

object TileWritable {
  def apply(tile: Tile) = {
    val tw = new TileWritable
    tw.set(tile)
    tw
  }
}
