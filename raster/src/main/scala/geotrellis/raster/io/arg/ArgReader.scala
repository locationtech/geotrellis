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
import geotrellis.vector.io.FileSystem
import geotrellis.vector.Extent

import com.typesafe.config.ConfigFactory

import java.io.File
import java.nio.ByteBuffer

object ArgReader {
  /** Reads an arg from the json metadata file. */
  final def read(path: String): Raster =
    read(path, None)

  /** Reads an arg from the json metadata file. */
  final def read(path: String, targetRasterExtent: RasterExtent): Raster =
    read(path, Some(targetRasterExtent))

  /** Reads an arg from the json metadata file. */
  private final def read(path: String, targetRasterExtent: Option[RasterExtent]): Raster = {
    val json = ConfigFactory.parseString(FileSystem.readText(path))

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

    val xmin = json.getDouble("xmin")
    val ymin = json.getDouble("ymin")
    val xmax = json.getDouble("xmax")
    val ymax = json.getDouble("ymax")
    val extent = Extent(xmin, ymin, xmax, ymax)

    val cols = json.getInt("cols")
    val rows = json.getInt("rows")

    val layerType = json.getString("type").toLowerCase
    if(layerType == "constant") {
      val v = json.getDouble("constant")
      cellType match {
        case TypeBit => Raster(BitConstantTile(d2i(v), cols, rows), extent)
        case TypeByte => Raster(ByteConstantTile(d2b(v), cols, rows), extent)
        case TypeShort => Raster(ShortConstantTile(d2s(v), cols, rows), extent)
        case TypeInt => Raster(IntConstantTile(d2i(v), cols, rows), extent)
        case TypeFloat => Raster(FloatConstantTile(d2f(v), cols, rows), extent)
        case TypeDouble => Raster(DoubleConstantTile(v, cols, rows), extent)
      }
    } else {

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
    
      targetRasterExtent match {
        case Some(te) =>
          Raster(read(argPath, cellType, RasterExtent(extent, cols, rows), te), te.extent)
        case None =>
          Raster(read(argPath, cellType, cols, rows), extent)
      }
    }
  }

  final def read(path: String, typ: CellType, cols: Int, rows: Int): Tile = {
    ArrayTile.fromBytes(FileSystem.slurp(path), typ, cols, rows)
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
      FileSystem.mapToByteArray(path, bytes, startIndex, length)

      resampleBytes(bytes, typ, rasterExtent, targetExtent)
    } else {
      ArrayTile.empty(typ, targetExtent.cols, targetExtent.rows)
    }
  }

  final def resampleBytes(bytes: Array[Byte], typ: CellType, re: RasterExtent, targetRe: RasterExtent): Tile = {
    val cols = targetRe.cols
    val rows = targetRe.rows

    typ match {
      case TypeBit =>
        val resampled = Array.ofDim[Byte]((cols*rows + 7)/8)
        Resample(re, targetRe, new BitResampleAssign(bytes, resampled))
        BitArrayTile(resampled, cols, rows)
      case TypeByte =>
        // ByteBuffer assign benchmarked faster than just using Array[Byte] for source.
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
        Resample(re, targetRe, new ByteBufferResampleAssign(buffer, resampled))
        ByteArrayTile(resampled, cols, rows)
      case TypeShort =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Short](cols*rows).fill(shortNODATA)
        Resample(re, targetRe, new ShortBufferResampleAssign(buffer, resampled))
        ShortArrayTile(resampled, cols, rows)
      case TypeInt =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Int](cols*rows).fill(NODATA)
        Resample(re, targetRe, new IntBufferResampleAssign(buffer, resampled))
        IntArrayTile(resampled, cols, rows)
      case TypeFloat =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Float](cols*rows).fill(Float.NaN)
        Resample(re, targetRe, new FloatBufferResampleAssign(buffer, resampled))
        FloatArrayTile(resampled, cols, rows)
      case TypeDouble =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Double](cols*rows).fill(Double.NaN)
        Resample(re, targetRe, new DoubleBufferResampleAssign(buffer, resampled))
        DoubleArrayTile(resampled, cols, rows)
    }
  }
}
