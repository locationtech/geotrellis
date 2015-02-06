package geotrellis.raster.compression

import geotrellis.raster._

import collection.mutable.ArrayBuffer
import java.nio.ByteBuffer

import spire.syntax.cfor._

object RLECompressor extends Compressor {

  override def compress(tile: Tile): Array[Byte] = {
    val in =
      if (tile.cellType == TypeBit) tile.toBytes
      else {
        val bb = ArrayBuffer[Byte]()
        val tileSize = tile.cols * tile.rows
        val canCountWithChar = tileSize < Char.MaxValue
        if (canCountWithChar) bb += 1.toByte else bb += 0.toByte
        var i = 0

        if (tile.cellType.isFloatingPoint) {
          val doubleArray = tile.toArrayDouble
          while (i < doubleArray.size) {
            val c = doubleArray(i)
            val oi = i
            i += 1
            if (c.isNaN)
              while (i < doubleArray.size && doubleArray(i).isNaN) i += 1
            else
              while (i < doubleArray.size && doubleArray(i) == c) i += 1

            val count = i - oi

            putCount(canCountWithChar, count, bb)

            if (tile.cellType == TypeDouble) putDouble(c, bb)
            else putFloat(c.toFloat, bb)
          }
        } else {
          val array = tile.toArray
          while (i < array.size) {
            val c = array(i)
            val oi = i
            i += 1
            while (i < array.size && array(i) == c) i += 1

            val count = i - oi

            putCount(canCountWithChar, count, bb)

            if (tile.cellType == TypeInt) putInt(c, bb)
            else if (tile.cellType == TypeShort) putShort(c.toShort, bb)
            else bb += c.toByte
          }
        }

        bb.toArray
      }

    val uncompressedSize = tile.cellType.bytes * tile.cols * tile.rows
    if (uncompressedSize <= in.size)
      Zipper(tile.toBytes)
    else
      Zipper(in)
  }

  @inline
  private def putCount(isChar: Boolean, value: Int, bb: ArrayBuffer[Byte]) =
    if (isChar) putShort(value.toShort, bb) else putInt(value, bb)

  @inline
  private def putShort(short: Short, bb: ArrayBuffer[Byte]) = {
    bb += (short & 0xff).toByte
    bb += ((short >> 8) & 0xff).toByte
  }

  @inline
  private def putInt(int: Int, bb: ArrayBuffer[Byte]) = {
    cfor(0)(_ < 4, _ + 1) { i =>
      bb += ((int >> (3 - i) * 8) & 0Xff).toByte
    }
  }

  @inline
  private def putFloat(float: Float, bb: ArrayBuffer[Byte]) = {
    val out = ByteBuffer.allocate(4).putFloat(float).array
    cfor(0)(_ < 4, _ + 1) { i =>
      bb += out(i)
    }
  }

  @inline
  private def putDouble(double: Double, bb: ArrayBuffer[Byte]) = {
    val out = ByteBuffer.allocate(8).putDouble(double).array
    cfor(0)(_ < 8, _ + 1) { i =>
      bb += out(i)
    }
  }

}

object RLEDecompressor extends Decompressor {

  override def decompress(
    in: Array[Byte],
    cellType: CellType,
    cols: Int,
    rows: Int): Tile = {
    val unzipped = UnZipper(in)
    val uncompressedSize = cellType.bytes * cols * rows
    val out =
      if (uncompressedSize == unzipped.size) unzipped
      else {
        val countsWithChar = unzipped(0) == 1
        val countIncrementer = if (countsWithChar) 2 else 4
        val bytes = cellType.bytes
        val out = Array.ofDim[Byte](uncompressedSize)
        var outIdx = 0

        var idx = 1
        while (idx < unzipped.size) {
          val count =
            if (countsWithChar) bytesToCharToInt(idx, unzipped)
            else bytesToInt(idx, unzipped)

          idx += countIncrementer

          cfor(0)(_ < count, _ + 1) { i =>
            System.arraycopy(unzipped, idx, out, outIdx, bytes)
            outIdx += bytes
          }

          idx += bytes
        }

        out
      }

    ArrayTile.fromBytes(out, cellType, cols, rows)
  }

  @inline
  private def bytesToCharToInt(idx: Int, arr: Array[Byte]): Int =
    ((arr(idx) & 0xff) | ((arr(idx + 1) & 0xff) << 8))

  @inline
  private def bytesToInt(idx: Int, arr: Array[Byte]): Int =
    ((arr(idx + 3) & 0xff) | ((arr(idx + 2) & 0xff) << 8) |
      ((arr(idx + 1) & 0xff) << 16) | ((arr(idx) & 0xff) << 24))

}

class RLECompressedTile(tile: Tile) extends CompressedTile(
  tile,
  RLECompressor,
  RLEDecompressor
)
