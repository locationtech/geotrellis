/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.io.arg

import geotrellis.raster._
import geotrellis.raster.resample._
import geotrellis.vector.Extent
import geotrellis.util.Filesystem

import com.typesafe.config.ConfigFactory

import java.io.File
import java.nio.ByteBuffer

object ArgReader {
  /** Reads an arg from the json metadata file. */
  final def read(path: String): SinglebandRaster =
    read(path, None)

  /** Reads an arg from the json metadata file. */
  final def read(path: String, targetRasterExtent: RasterExtent): SinglebandRaster =
    read(path, Some(targetRasterExtent))

  /** Reads an arg from the json metadata file. */
  private final def read(path: String, targetRasterExtent: Option[RasterExtent]): SinglebandRaster = {
    val json = ConfigFactory.parseString(Filesystem.readText(path))

    val cellType =
      json.getString("datatype") match {
        case "bool" => BitCellType
        case "int8" => ByteConstantNoDataCellType
        case "uint8" => UByteConstantNoDataCellType
        case "int16" => ShortConstantNoDataCellType
        case "uint16" => UShortConstantNoDataCellType
        case "int32" => IntConstantNoDataCellType
        case "float32" => FloatConstantNoDataCellType
        case "float64" => DoubleConstantNoDataCellType
        case s => throw new IllegalArgumentException(s"Unsupported datatype $s")
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
        case BitCellType => Raster(BitConstantTile(d2i(v), cols, rows), extent)
        case ByteConstantNoDataCellType => Raster(ByteConstantTile(d2b(v), cols, rows), extent)
        case UByteConstantNoDataCellType => Raster(UByteConstantTile(d2b(v), cols, rows), extent)
        case ShortConstantNoDataCellType => Raster(ShortConstantTile(d2s(v), cols, rows), extent)
        case UShortConstantNoDataCellType => Raster(UShortConstantTile(d2s(v), cols, rows), extent)
        case IntConstantNoDataCellType => Raster(IntConstantTile(d2i(v), cols, rows), extent)
        case FloatConstantNoDataCellType => Raster(FloatConstantTile(d2f(v), cols, rows), extent)
        case DoubleConstantNoDataCellType => Raster(DoubleConstantTile(v, cols, rows), extent)
        case _ => throw new IllegalArgumentException(s"Unsupported datatype $cellType")
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

      targetRasterExtent match {
        case Some(te) =>
          Raster(read(argPath, cellType, RasterExtent(extent, cols, rows), te), te.extent)
        case None =>
          Raster(read(argPath, cellType, cols, rows), extent)
      }
    }
  }

  final def read(path: String, cellType: CellType, cols: Int, rows: Int): Tile = {
    ArrayTile.fromBytes(Filesystem.slurp(path), cellType, cols, rows)
  }

  final def read(path: String, cellType: CellType, rasterExtent: RasterExtent, targetExtent: RasterExtent): Tile = {
    val size = cellType.numBytes(rasterExtent.size)

    val cols = rasterExtent.cols
    // Find the top-left most and bottom-right cell coordinates
    val GridBounds(colMin, rowMin, colMax, rowMax) = rasterExtent.gridBoundsFor(targetExtent.extent)

    // Get the indices, buffer one col and row on each side
    val startIndex = math.max(cellType.numBytes((rowMin-1) * cols + colMin - 1), 0)
    val length = math.min(size-startIndex, cellType.numBytes((rowMax+1) * cols + colMax+1) - startIndex)

    if(length > 0) {
      val bytes = Array.ofDim[Byte](size)
      Filesystem.mapToByteArray(path, bytes, startIndex, length)

      resampleBytes(bytes, cellType, rasterExtent, targetExtent)
    } else {
      ArrayTile.empty(cellType, targetExtent.cols, targetExtent.rows)
    }
  }

  final def resampleBytes(bytes: Array[Byte], cellType: CellType, re: RasterExtent, targetRe: RasterExtent): Tile = {
    val cols = targetRe.cols
    val rows = targetRe.rows

    // TODO: Add stuff for other celltypes
    cellType match {
      case BitCellType =>
        val resampled = Array.ofDim[Byte]((cols*rows + 7)/8)
        ResampleAssign(re, targetRe, new BitResampleAssign(bytes, resampled))
        BitArrayTile(resampled, cols, rows)
      case ByteConstantNoDataCellType =>
        // ByteBuffer assign benchmarked faster than just using Array[Byte] for source.
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
        ResampleAssign(re, targetRe, new ByteBufferResampleAssign(buffer, resampled))
        ByteArrayTile(resampled, cols, rows)
      case UByteConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Byte](cols*rows).fill(byteNODATA)
        ResampleAssign(re, targetRe, new ByteBufferResampleAssign(buffer, resampled))
        UByteArrayTile(resampled, cols, rows)
      case ShortConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Short](cols*rows).fill(shortNODATA)
        ResampleAssign(re, targetRe, new ShortBufferResampleAssign(buffer, resampled))
        ShortArrayTile(resampled, cols, rows)
      case UShortConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Short](cols*rows).fill(shortNODATA)
        ResampleAssign(re, targetRe, new ShortBufferResampleAssign(buffer, resampled))
        UShortArrayTile(resampled, cols, rows)
      case IntConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Int](cols*rows).fill(NODATA)
        ResampleAssign(re, targetRe, new IntBufferResampleAssign(buffer, resampled))
        IntArrayTile(resampled, cols, rows)
      case FloatConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Float](cols*rows).fill(Float.NaN)
        ResampleAssign(re, targetRe, new FloatBufferResampleAssign(buffer, resampled))
        FloatArrayTile(resampled, cols, rows)
      case DoubleConstantNoDataCellType =>
        val buffer = ByteBuffer.wrap(bytes)
        val resampled = Array.ofDim[Double](cols*rows).fill(Double.NaN)
        ResampleAssign(re, targetRe, new DoubleBufferResampleAssign(buffer, resampled))
        DoubleArrayTile(resampled, cols, rows)
      case _ => throw new IllegalArgumentException(s"Unsupported datatype $cellType")
    }
  }
}
