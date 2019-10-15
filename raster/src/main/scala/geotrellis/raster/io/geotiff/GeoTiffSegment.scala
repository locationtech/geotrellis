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

package geotrellis.raster.io.geotiff

import geotrellis.raster.io.geotiff.util._
import geotrellis.raster._

import java.util.BitSet
import java.nio.ByteBuffer
import spire.syntax.cfor._

/**
 * Base trait of GeoTiffSegment
 */
trait GeoTiffSegment {
  def size: Int
  def getInt(i: Int): Int
  def getDouble(i: Int): Double

  /** represents all of the bytes in the segment */
  def bytes: Array[Byte]

  def map(f: Int => Int): Array[Byte]
  def mapDouble(f: Double => Double): Array[Byte]
  def mapWithIndex(f: (Int, Int) => Int): Array[Byte]
  def mapDoubleWithIndex(f: (Int, Double) => Double): Array[Byte]

  /**
   * Converts the segment to the given CellType
   *
   * @param cellType: The desired [[CellType]] to convert to
   * @return An Array[Byte] that contains the new CellType values
   */
  def convert(cellType: CellType): Array[Byte] =
    cellType match {
      case BitCellType =>
        val dsize = size >> 3
        val arr = Array.ofDim[Byte](dsize)
        cfor(0)(_ < size, _ + 1) { i => BitArrayTile.update(arr, i, getInt(i))  }
        // Our BitCellType rasters have the bits encoded in a order inside of each byte that is
        // the reverse of what a GeoTiff wants.
        cfor(0)(_ < dsize, _ + 1) { i => arr(i) = ((Integer.reverse(arr(i)) >>> 24) & 0xff).toByte }
        arr
      case ByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => getInt(i).toByte }
        arr
      case UByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => i2ub(getInt(i)) }
        arr
      case ShortCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i).toShort }
        arr.toArrayByte()
      case UShortCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2us(getInt(i)) }
        arr.toArrayByte()
      case IntCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i).toFloat }
        arr.toArrayByte()
      case DoubleCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
      case ByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2b(getInt(i)) }
        arr
      case UByteConstantNoDataCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2ub(getInt(i)) }
        arr
      case ShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2s(getInt(i)) }
        arr.toArrayByte()
      case UShortConstantNoDataCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = i2us(getInt(i)) }
        arr.toArrayByte()
      case IntConstantNoDataCellType =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i) }
        arr.toArrayByte()
      case FloatConstantNoDataCellType =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = d2f(getDouble(i)) }
        arr.toArrayByte()
      case DoubleConstantNoDataCellType =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getDouble(i) }
        arr.toArrayByte()
      case ByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toByte
        }
        arr
      case UByteUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toByte
        }
        arr
      case ShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toShort
        }
        arr.toArrayByte()
      case UShortUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v.toShort
        }
        arr.toArrayByte()
      case IntUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Int](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getInt(i)
          arr(i) = if (v == Int.MinValue) nd else v
        }
        arr.toArrayByte()
      case FloatUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Float](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getDouble(i)
          arr(i) = if (v == Double.NaN) nd else v.toFloat
        }
        arr.toArrayByte()
      case DoubleUserDefinedNoDataCellType(nd) =>
        val arr = Array.ofDim[Double](size)
        cfor(0)(_ < size, _ + 1) { i =>
          val v = getDouble(i)
          arr(i) = if (v == Double.NaN) nd else v
        }
        arr.toArrayByte()
    }
}

object GeoTiffSegment {
  /**
   * Splits interleave pixel segment into component band bytes
   *
   * @param bytes Pixel interleaved segment bytes
   * @param bandCount Number of samples interleaved in each pixel
   * @param bytesPerSample Number of bytes in each sample
   */
  private[raster]
  def deinterleave(bytes: Array[Byte], bandCount: Int, bytesPerSample: Int): Array[Array[Byte]] = {
    val bands: Array[Array[Byte]] = new Array[Array[Byte]](bandCount)
    val segmentSize = bytes.length / bandCount
    cfor(0)(_ < bandCount, _ + 1) { i =>
      bands(i) = new Array[Byte](segmentSize)
    }

    val bb = ByteBuffer.wrap(bytes)
    cfor(0)(_ < segmentSize, _ + bytesPerSample) { offset =>
      cfor(0)(_ < bandCount, _ + 1) { band =>
        bb.get(bands(band), offset, bytesPerSample)
      }
    }

    bands
  }

  private[raster]
  def deinterleave(bytes: Array[Byte], bandCount: Int, bytesPerSample: Int, index: Int): Array[Byte] =
    deinterleave(bytes, bandCount, bytesPerSample, index :: Nil).head

  private[raster]
  def deinterleave(bytes: Array[Byte], bandCount: Int, bytesPerSample: Int, indices: Traversable[Int]): Array[Array[Byte]] = {
    val indicesList = indices.toList
    val bandToIndex = indicesList.zipWithIndex.toMap
    val actualBandCount = indicesList.length

    val bands: Array[Array[Byte]] = new Array[Array[Byte]](actualBandCount)
    val segmentSize = bytes.length / bandCount
    cfor(0)(_ < actualBandCount, _ + 1) { i =>
      bands(i) = new Array[Byte](segmentSize)
    }

    val bb = ByteBuffer.wrap(bytes)
    cfor(0)(_ < segmentSize, _ + bytesPerSample) { offset =>
      cfor(0)(_ < bandCount, _ + 1) { band =>
        if(indicesList.contains(band)) bb.get(bands(bandToIndex(band)), offset, bytesPerSample)
        else bb.position(bb.position() + bytesPerSample)
      }
    }

    bands
  }

  /**
    * Splits interleaved bit pixels into component bands
    *
    * @param segment segment of pixel interleaved bits
    * @param cols number of pixel columns in each band
    * @param rows number of pixel rows in each band
    * @param bandCount Number of bit interleaved into each pixel
    */
  private[raster]
  def deinterleaveBitSegment(segment: GeoTiffSegment, dims: Dimensions[Int], bandCount: Int): Array[Array[Byte]] = {
    val cols = dims.cols
    val rows = dims.rows
    val paddedCols = {
      val bytesWidth = (cols + 7) / 8
      bytesWidth * 8
    }
    val resultByteCount = (paddedCols / 8) * rows

    // packed byte arrays for each band in this segment
    val bands = Array.fill[Array[Byte]](bandCount)(Array.ofDim[Byte](resultByteCount))

    cfor(0)(_ < segment.size, _ + 1) { i =>
      val bandIndex = i % bandCount
      val j = i / bandCount
      val col = j % cols
      val row = j / cols
      val i2 = (row * paddedCols) + col
      BitArrayTile.update(bands(bandIndex), i2, segment.getInt(i))
    }

    // Inverse the byte, to account for endian mismatching.
    cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
      val bytes = bands(bandIndex)
      cfor(0)(_ < bytes.length, _ + 1) { i =>
        bytes(i) = invertByte(bytes(i))
      }
    }

    bands
  }

  private[raster]
  def deinterleaveBitSegment(segment: GeoTiffSegment, dims: Dimensions[Int], bandCount: Int, index: Int): Array[Byte] =
    deinterleaveBitSegment(segment, dims, bandCount, index :: Nil).head

  private[raster]
  def deinterleaveBitSegment(segment: GeoTiffSegment, dims: Dimensions[Int], bandCount: Int, indices: Traversable[Int]): Array[Array[Byte]] = {
    val cols = dims.cols
    val rows = dims.rows
    val paddedCols = {
      val bytesWidth = (cols + 7) / 8
      bytesWidth * 8
    }
    val resultByteCount = (paddedCols / 8) * rows
    val indicesList = indices.toList
    val bandToIndex = indicesList.zipWithIndex.toMap
    val actualBandCount = indicesList.length

    // packed byte arrays for each band in this segment
    val bands = Array.fill[Array[Byte]](actualBandCount)(Array.ofDim[Byte](resultByteCount))

    cfor(0)(_ < segment.size, _ + 1) { i =>
      val bandIndex = i % bandCount
      // TODO: flip this loop to avoid conditional test per-pixel
      if(indicesList.contains(bandIndex)) {
        val j = i / bandCount
        val col = j % cols
        val row = j / cols
        val i2 = (row * paddedCols) + col
        BitArrayTile.update(bands(bandToIndex(bandIndex)), i2, segment.getInt(i))
      }
    }

    // Inverse the byte, to account for endian mismatching.
    cfor(0)(_ < actualBandCount, _ + 1) { bandIndex =>
      val bytes = bands(bandIndex)
      cfor(0)(_ < bytes.length, _ + 1) { i =>
        bytes(i) = invertByte(bytes(i))
      }
    }

    bands
  }

  private[raster]
  def pixelInterleave(tile: MultibandTile): Array[Byte] = {
    val bandCount = tile.bandCount
    val byteCount = tile.cellType.bytes
    val bytes = Array.ofDim[Byte](byteCount * bandCount * tile.cols * tile.rows)
    val bandBytes: Vector[Array[Byte]] = tile.bands.map(_.toBytes)

    var segIndex = 0
    cfor(0)(_ < tile.cols * tile.rows, _ + 1) { cellIndex =>
      cfor(0)(_ < bandCount, _ + 1) { bandIndex =>
        val bandByteArr = bandBytes(bandIndex)
        cfor(0)(_ < byteCount, _ + 1) { b =>
          val bandByteIndex = cellIndex * byteCount + b
          bytes(segIndex) = bandByteArr(cellIndex * byteCount + b)
          segIndex += 1
        }
      }
    }

    bytes
  }
}
