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

package geotrellis.io.geotiff

import monocle.syntax._
import monocle.Macro._

import java.util.BitSet

import geotrellis._
import geotrellis.raster._

import geotrellis.io.geotiff.ImageDirectoryLenses._
import geotrellis.io.geotiff.utils.ByteInverterUtils._
import geotrellis.io.geotiff.utils.BitSetUtils._

case class ImageConverter(directory: ImageDirectory) {

  def convert(uncompressedImage: Vector[Vector[Byte]]): Vector[Byte] =
    if (!directory.hasStripStorage) tiledImageToRowImage(uncompressedImage)
    else if (directory.bitsPerPixel == 1) stripBitImageOverflow(uncompressedImage)
    else if (directory.cellType == TypeFloat) flipToFloat(uncompressedImage.flatten)
    else if (directory.cellType == TypeDouble) flipToDouble(uncompressedImage.flatten)
    else uncompressedImage.flatten

  private def flipToFloat(image: Vector[Byte]) = flip(image, 4)

  private def flipToDouble(image: Vector[Byte]) = flip(image, 8)

  private def flip(image: Vector[Byte], flipSize: Int) = {
    val size = image.size
    val arr = Array.ofDim[Byte](size)

    var i = 0
    while (i < size) {
      for (j <- 0 until 4)
        arr(i + j) = image(i + flipSize - j - 1)

      i += flipSize
    }

    arr.toVector
  }

  private def tiledImageToRowImage(tiledImage: Vector[Vector[Byte]]) = {
    val arrayImage = tiledImage.map(_.toArray).toArray

    if (directory.bitsPerPixel == 1) bitTiledImageToRowImage(arrayImage)
    else byteTiledImageToRowImage(arrayImage)
  }

  private def bitTiledImageToRowImage(tiledImage: Array[Array[Byte]]) = {

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

    //println(s"widthRes: $widthRes, overflow: $overflow, imageLength: $imageLength")

    for (i <- 0 until imageLength) {
      for (j <- 0 until tilesWidth) {
        val index = j + (i / tileLength) * tilesWidth
        val tileBitSet = tiledImageBitSets(index)

        val start = (i % tileLength) * (tileWidth + overflow)
        val length = if (j == tilesWidth - 1 && widthRes != 0)
          widthRes else tileWidth

        /*if (j == tilesWidth - 1) {
          //println(s"tile bits: resIndex: $resIndex")
          for (i <- start until start + 14)
            //if (tileBitSet.get(i)) print("1   ") else print("0   ")

          //println()
          for (i <- 896 until 896 + 14)
            //print(s"$i ")
          //println()
        }*/

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

    resBitSet.toByteVector(resBitSetSize)
  }

  private def bitSetCopy(src: BitSet, srcPos: Int, dest: BitSet, destPos: Int, length: Int) =
    for (i <- 0 until length)
      if (src.get(srcPos + i)) dest.set(destPos + i)

  private def byteTiledImageToRowImage(tiledImage: Array[Array[Byte]]) = {

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

    for (i <- 0 until imageLength) {
      for (j <- 0 until tilesWidth) {
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

    resArray.toVector
  }

  private def stripBitImageOverflow(image: Vector[Vector[Byte]]) = {

    val imageWidth = (directory |-> imageWidthLens get).toInt
    val imageLength = (directory |-> imageLengthLens get).toInt

    val rowBitSetsArray = Array.ofDim[BitSet](imageLength)
    var rowBitSetsIndex = 0

    val rowByteSize = (imageWidth + 7) / 8
    val tempBitSetArray = Array.ofDim[Byte](rowByteSize)

    for (i <- 0 until image.size) {
      val rowsInSegment = directory.rowsInSegment(i)
      val current = image(i).toArray

      for (j <- 0 until current.size)
        current(j) = invertByte(current(j))

      for (j <- 0 until rowsInSegment) {
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

    for (i <- 0 until imageLength) {
      val bs = rowBitSetsArray(i)

      bitSetCopy(
        bs,
        0,
        resBitSet,
        imageWidth * i,
        imageWidth
      )
    }

    resBitSet.toByteVector(resSize)

  }

}
