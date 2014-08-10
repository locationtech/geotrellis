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

package geotrellis.raster.io.geotiff.reader

import monocle.syntax._
import monocle.Macro._

import java.util.BitSet
import java.nio.ByteBuffer

import geotrellis._
import geotrellis.raster._

import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils._

import spire.syntax.cfor._

case class ImageConverter(directory: ImageDirectory) {

  def convert(uncompressedImage: Array[Array[Byte]]): Array[Byte] = {
    val stripedImage =
      if (!directory.hasStripStorage) tiledImageToRowImage(uncompressedImage)
      else if (directory.bitsPerPixel == 1) stripBitImageOverflow(uncompressedImage)
      else uncompressedImage.flatten

    val image =
      if (directory.cellType == TypeFloat) flipToFloat(stripedImage)
      else if (directory.cellType == TypeDouble) flipToDouble(stripedImage)
      else stripedImage

    directory |-> gdalInternalNoDataLens get match {
      case Some(gdalNoData) =>  
        replaceGDALNoDataWithNODATA(image, gdalNoData, directory.cellType)
      case None => // pass
    }

    image
  }

  private def flipToFloat(image: Array[Byte]): Array[Byte] = flip(image, 4)

  private def flipToDouble(image: Array[Byte]): Array[Byte] = flip(image, 8)

  private def flip(image: Array[Byte], flipSize: Int): Array[Byte] = {
    val size = image.size
    val arr = Array.ofDim[Byte](size)

    var i = 0
    while (i < size) {
      arr(i) = image(i + flipSize - 1)
      arr(i + 1) = image(i + flipSize - 2)
      arr(i + 2) = image(i + flipSize - 3)
      arr(i + 3) = image(i + flipSize - 4)

      i += flipSize
    }

    arr
  }

  private def tiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {
    if (directory.bitsPerPixel == 1) bitTiledImageToRowImage(tiledImage)
    else byteTiledImageToRowImage(tiledImage)
  }

  private def bitTiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {

    val tiledImageBitSets = tiledImage.map(x => BitSet.valueOf(x.map(invertByte(_))))

    val tileWidth = directory.rowSize
    val tileLength = directory.rowsInSegment(0)

    val imageWidth = (directory |-> imageWidthLens get).toInt
    val imageLength = (directory |-> imageLengthLens get).toInt

    val widthRes = imageWidth % tileWidth

    val tilesWidth = imageWidth / tileWidth + (if (widthRes > 0) 1 else 0)

    val resBitSetSize = imageWidth * imageLength
    val resBitSet = new BitSet(resBitSetSize)
    var resIndex = 0

    val overflow = (8 - tileWidth % 8) % 8

    cfor(0)(_ < imageLength, _ + 1) { i =>
      cfor(0)(_ < tilesWidth, _ + 1) { j =>
        val index = j + (i / tileLength) * tilesWidth
        val tileBitSet = tiledImageBitSets(index)

        val start = (i % tileLength) * (tileWidth + overflow)
        val length = if (j == tilesWidth - 1 && widthRes != 0)
          widthRes else tileWidth

        bitSetCopy(
          tileBitSet,
          start,
          resBitSet,
          resIndex,
          length
        )

        resIndex += length
      }
    }

    resBitSet.toByteArray()
  }

  private def bitSetCopy(src: BitSet, srcPos: Int, dest: BitSet, destPos: Int, length: Int): Unit = {
    cfor(0)(_ < length, _ + 1) { i =>
      if (src.get(srcPos + i)) dest.set(destPos + i)
    }
  }

  private def byteTiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {

    val bytesPerPixel = (directory.bitsPerPixel + 7) / 8

    val tileWidth = directory.rowSize
    val tileLength = directory.rowsInSegment(0)

    val imageWidth = (directory |-> imageWidthLens get).toInt
    val imageLength = (directory |-> imageLengthLens get).toInt

    val widthRes = imageWidth % tileWidth

    val tilesWidth = imageWidth / tileWidth + (if (widthRes > 0) 1 else 0)

    val lengthRes = imageLength % tileLength

    val tilesLength = imageLength / tileLength + (if (lengthRes > 0) 1 else 0)

    val resArray = Array.ofDim[Byte](imageWidth * imageLength * bytesPerPixel)
    var resArrayIndex = 0

    cfor(0)(_ < imageLength, _ + 1) { i =>
      cfor(0)(_ < tilesWidth, _ + 1) { j =>
        val tile = tiledImage(j + (i / tileLength) * tilesWidth)
        val start = (i % tileLength) * tileWidth * bytesPerPixel
        val length = bytesPerPixel * (if (j == tilesWidth - 1 && widthRes != 0)
          widthRes else tileWidth)

        System.arraycopy(
          tile,
          start,
          resArray,
          resArrayIndex,
          length
        )

        resArrayIndex += length
      }
    }

    resArray
  }

  private def stripBitImageOverflow(image: Array[Array[Byte]]) = {

    val imageWidth = (directory |-> imageWidthLens get).toInt
    val imageLength = (directory |-> imageLengthLens get).toInt

    val rowBitSetsArray = Array.ofDim[BitSet](imageLength)
    var rowBitSetsIndex = 0

    val rowByteSize = (imageWidth + 7) / 8
    val tempBitSetArray = Array.ofDim[Byte](rowByteSize)

    cfor(0)(_ < image.size, _ + 1) { i =>
      val rowsInSegment = directory.rowsInSegment(i)
      val current = image(i).toArray

      cfor(0)(_ < current.size, _ + 1) { j =>
        current(j) = invertByte(current(j))
      }

      cfor(0)(_ < rowsInSegment, _ + 1) { j => 
        System.arraycopy(
          current,
          j * rowByteSize,
          tempBitSetArray,
          0,
          rowByteSize
        )

        rowBitSetsArray(rowBitSetsIndex) = BitSet.valueOf(tempBitSetArray)
        rowBitSetsIndex += 1
      }
    }

    val resSize = imageWidth * imageLength
    val resBitSet = new BitSet(resSize)

    cfor(0)(_ < imageLength, _ + 1) { i =>
      val bs = rowBitSetsArray(i)

      bitSetCopy(
        bs,
        0,
        resBitSet,
        imageWidth * i,
        imageWidth
      )
    }

    resBitSet.toByteArray()
  }

  // Modifies iamge inplace
  private def replaceGDALNoDataWithNODATA(image: Array[Byte], gdalNoData: Double, cellType: CellType): Unit =
    cellType match {
      case TypeByte =>
        val noData = gdalNoData.toByte
        val size = image.size
        cfor(0)(_ < size, _ + 1) { i =>
          if(image(i) == noData) { image(i) = byteNODATA }
        }
      case TypeShort =>
        val noData = gdalNoData.toShort

        val NDbyte1 = (shortNODATA >> 8).toByte
        val NDbyte2 = (shortNODATA & 0xff).toByte
        var i = 0
        while (i < image.size) {
          val short = image(i) << 8 + image(i + 1)

          if (short == noData) {
            image(i) = NDbyte1
            image(i+2) = NDbyte2
          }

          i += 2
        }
      case TypeInt =>
        val noData = gdalNoData.toInt

        val NDByte1 = (NODATA >> 24).toByte
        val NDByte2 = ((NODATA & 0xff0000) >> 16).toByte
        val NDByte3 = ((NODATA & 0xff00) >> 8).toByte
        val NDByte4 = (NODATA & 0xff).toByte

        var i = 0
        while (i < image.size) {
          val v = image(i) << 24 + image(i + 1) << 16 + image(i + 2) << 8 + image(i + 3)

          if (v == noData) {
            image(i) = NDByte1
            image(i + 1) = NDByte2
            image(i + 2) = NDByte3
            image(i + 3) = NDByte4
          }

          i += 4
        }
      // case TypeFloat =>
      //   val noData = gdalNoData.toFloat

      //   val bb = ByteBuffer.allocate(4)

      //   bb.putFloat(noData)

      //   val ndByte1 = bb.get(0)
      //   val ndByte2 = bb.get(1)
      //   val ndByte3 = bb.get(2)
      //   val ndByte4 = bb.get(3)

      //   bb.position(0)
      //   bb.putFloat(Float.NaN)

      //   val NDByte1 = bb.get(0)
      //   val NDByte2 = bb.get(1)
      //   val NDByte3 = bb.get(2)
      //   val NDByte4 = bb.get(3)

      //   var i = 0
      //   while (i < image.size) {
      //     val v = image(i) << 24 + image(i + 1) << 16 + image(i + 2) << 8 + image(i + 3)

      //     if (v == noData) {
      //       image(i) = NDByte1
      //       image(i + 1) = NDByte2
      //       image(i + 2) = NDByte3
      //       image(i + 3) = NDByte4
      //     }

      //     i += 4
      //   }

      //   val bb = ByteBuffer.allocate(8)

      //   if (cellType == TypeFloat) bb.putFloat(Float.NaN)
      //   else bb.putDouble(Double.NaN)

      //   bb.position(0)

      //   val indexRun = if (cellType == TypeFloat) 4 else 8
      //   val loopBB = ByteBuffer.allocate(8)

      //   var i = 0
      //   while (i < image.size) {
      //     for (j <- i until i + indexRun)
      //       loopBB.put(image(j))

      //     loopBB.position(0)

      //     val current =
      //       if (cellType == TypeFloat) loopBB.getFloat.toDouble
      //       else loopBB.getDouble

      //     loopBB.clear

      //     if (current == noData) {
      //       for (j <- i until i + indexRun)
      //         newArr(j) = bb.get

      //       bb.position(0)
      //     }

      //     i += indexRun
      //   }
      case TypeFloat | TypeDouble =>
        val noData = gdalNoData
        val bb = ByteBuffer.allocate(8)

        if (cellType == TypeFloat) bb.putFloat(Float.NaN)
        else bb.putDouble(Double.NaN)

        bb.position(0)

        val indexRun = if (cellType == TypeFloat) 4 else 8
        val loopBB = ByteBuffer.allocate(8)

        var i = 0
        while (i < image.size) {
          for (j <- i until i + indexRun)
            loopBB.put(image(j))

          loopBB.position(0)

          val current =
            if (cellType == TypeFloat) loopBB.getFloat.toDouble
            else loopBB.getDouble

          loopBB.clear

          if (current == noData) {
            for (j <- i until i + indexRun)
              image(j) = bb.get

            bb.position(0)
          }

          i += indexRun
        }
      case TypeBit => //pass
    }
}
