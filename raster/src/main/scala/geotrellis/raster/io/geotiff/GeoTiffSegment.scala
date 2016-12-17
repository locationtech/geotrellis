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
        val bs = new BitSet(size)
        cfor(0)(_ < size, _ + 1) { i => if ((getInt(i) & 1) == 0) { bs.set(i) } }
        bs.toByteArray()
      case ByteCellType | UByteCellType =>
        val arr = Array.ofDim[Byte](size)
        cfor(0)(_ < size, _ + 1) { i => getInt(i).toByte }
        arr
      case ShortCellType | UShortCellType =>
        val arr = Array.ofDim[Short](size)
        cfor(0)(_ < size, _ + 1) { i => arr(i) = getInt(i).toShort }
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

  /**
   * Splits interleaved bit pixels into component bands
   *
   * @param bytes Pixel interleaved segment as bytes
   * @param bandCount Number of bit interleaved into each pixel
   * @param nbits Number of bits in each band
   */
  def deinterleaveBits(bytes: Array[Byte], bandCount: Int, nbits: Int): Array[Array[Byte]] = {
    val source = BitSet.valueOf(bytes)
    val bands: Array[BitSet] = new Array[BitSet](bandCount)
    cfor(0)(_ < bandCount, _ + 1) { i =>
      bands(i) = new BitSet(nbits)
    }

    cfor(0)(_ < nbits * bandCount, _ + 1) { si =>
      val ti = si / bandCount
      val bi = si % bandCount

      if (source.get(si)) bands(bi).set(ti)
      else bands(bi).clear(ti)
    }

    bands.map(_.toByteArray)
  }

}
