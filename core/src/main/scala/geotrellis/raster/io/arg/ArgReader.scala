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
import geotrellis.util.Filesystem

import java.nio.ByteBuffer

object ArgReader {
  final def read(path: String, typ: CellType, rasterExtent: RasterExtent): Tile = {
    val cols = rasterExtent.cols
    val rows = rasterExtent.rows
    ArrayTile.fromArrayByte(Filesystem.slurp(path), typ, cols, rows)
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
