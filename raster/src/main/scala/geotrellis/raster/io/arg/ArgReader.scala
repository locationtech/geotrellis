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

import geotrellis.raster._
import geotrellis.raster.io.Filesystem
import geotrellis.vector.Extent

import com.typesafe.config.ConfigFactory

import java.io.File
import java.nio.ByteBuffer

object ArgReader {
  /** Reads an arg from the jsom metadata file. */
  final def read(path: String): Tile =
    read(path, None)

  /** Reads an arg from the jsom metadata file. */
  final def read(path: String, targetRasterExtent: RasterExtent): Tile =
    read(path, Some(targetRasterExtent))

  /** Reads an arg from the jsom metadata file. */
  private final def read(path: String, targetRasterExtent: Option[RasterExtent]): Tile = {
    val json = ConfigFactory.parseString(Filesystem.readText(path))
    val layerType = json.getString("type").toLowerCase
    if(layerType != "arg") { sys.error(s"Cannot read raster layer type $layerType, must be arg") }

    val argPath =
      (if(json.hasPath("path")) {
        val f = new File(json.getString("path"))
        if(f.isAbsolute) {
          f
        } else {
          new File(new File(path).getParent, f.getPath)
        }
      } else {
        val layerName = json.getString("layer")
        // Default to a .arg file with the same name as the layer name.
        new File(new File(path).getParent, layerName + ".arg")
      }).getAbsolutePath


    val cellType =
      json.getString("datatype") match {
        case "bool" => TypeBit
        case "int8" => TypeByte
        case "int16" => TypeShort
        case "int32" => TypeInt
        case "float32" => TypeFloat
        case "float64" => TypeDouble
        case s => sys.error("unsupported datatype '%s'" format s)
      }

    val cols = json.getInt("cols")
    val rows = json.getInt("rows")

    targetRasterExtent match {
      case Some(te) =>

        val xmin = json.getDouble("xmin")
        val ymin = json.getDouble("ymin")
        val xmax = json.getDouble("xmax")
        val ymax = json.getDouble("ymax")
        val extent = Extent(xmin, ymin, xmax, ymax)

        read(argPath, cellType, RasterExtent(extent, cols, rows), te)
      case None =>
        read(argPath, cellType, cols, rows)
    }
  }

  final def read(path: String, typ: CellType, cols: Int, rows: Int): Tile = {
    ArrayTile.fromBytes(Filesystem.slurp(path), typ, cols, rows)
  }

  final def read(path: String, typ: CellType, rasterExtent: RasterExtent, targetExtent: RasterExtent): Tile = {
    val size = typ.numBytes(rasterExtent.size)

    val cols = rasterExtent.cols
    // Find the top-left most and bottom-right cell coordinates
    val GridBounds(colMin, rowMin, colMax, rowMax) = rasterExtent.gridBoundsFor(targetExtent.extent)

    // Get the indices, buffer one col and row on each side
    val startIndex = math.max(typ.numBytes((rowMin-1) * cols + colMin - 1), 0)
    val length = math.min(size-startIndex, typ.numBytes((rowMax+1) * cols + colMax+1) - startIndex)

    if(length > 0) {
      val bytes = Array.ofDim[Byte](size)
      Filesystem.mapToByteArray(path, bytes, startIndex, length)

      warpBytes(bytes, typ, rasterExtent, targetExtent)
    } else {
      ArrayTile.empty(typ, targetExtent.cols, targetExtent.rows)
    }
  }

  final def warpBytes(bytes: Array[Byte], typ: CellType, re: RasterExtent, targetRe: RasterExtent): Tile = {
    val cols = targetRe.cols
    val rows = targetRe.rows

    typ match {
      case TypeBit =>
        val warped = Array.ofDim[Byte]((cols*rows + 7)/8)
        Warp(re, targetRe, new BitWarpAssign(bytes, warped))
        BitArrayTile(warped, cols, rows)
      case TypeByte =>
        // ByteBuffer assign benchmarked faster than just using Array[Byte] for source.
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
        Warp(re, targetRe, new ByteBufferWarpAssign(buffer, warped))
        ByteArrayTile(warped, cols, rows)
      case TypeShort =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Short](cols*rows).fill(shortNODATA)
        Warp(re, targetRe, new ShortBufferWarpAssign(buffer, warped))
        ShortArrayTile(warped, cols, rows)
      case TypeInt =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Int](cols*rows).fill(NODATA)
        Warp(re, targetRe, new IntBufferWarpAssign(buffer, warped))
        IntArrayTile(warped, cols, rows)
      case TypeFloat =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Float](cols*rows).fill(Float.NaN)
        Warp(re, targetRe, new FloatBufferWarpAssign(buffer, warped))
        FloatArrayTile(warped, cols, rows)
      case TypeDouble =>
        val buffer = ByteBuffer.wrap(bytes)
        val warped = Array.ofDim[Double](cols*rows).fill(Double.NaN)
        Warp(re, targetRe, new DoubleBufferWarpAssign(buffer, warped))
        DoubleArrayTile(warped, cols, rows)
    }
  }
}
