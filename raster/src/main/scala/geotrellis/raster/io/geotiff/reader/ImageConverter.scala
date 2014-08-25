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

import java.util.BitSet
import java.nio.ByteBuffer

import geotrellis._
import geotrellis.raster._

import geotrellis.raster.io.geotiff.reader.ImageDirectoryLenses._
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils._

import spire.syntax.cfor._

object ImageConverter {

  def apply(directory: ImageDirectory, isBigEndian: Boolean) =
    new ImageConverter(directory, isBigEndian)

}

class ImageConverter(directory: ImageDirectory, isBigEndian: Boolean) {

  def convert(uncompressedImage: Array[Array[Byte]]): Array[Byte] = {
    val stripedImage =
      if (!directory.hasStripStorage) tiledImageToRowImage(uncompressedImage)
      else if (directory.bitsPerPixel == 1) stripBitImageOverflow(uncompressedImage)
      else uncompressedImage.flatten

    if (directory.cellType == TypeFloat && !isBigEndian)
      flipToFloat(stripedImage)
    else if (directory.cellType == TypeDouble && !isBigEndian)
      flipToDouble(stripedImage)
    else
      stripedImage
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

  private def setCorrectOrientation(image: Array[Byte]) =
    OrientationConverter(directory).setCorrectOrientation(image)

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
}
