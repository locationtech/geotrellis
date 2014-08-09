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
import geotrellis.raster.io.geotiff.reader.utils.BitSetUtils._

case class ImageConverter(directory: ImageDirectory) {

  def convert(uncompressedImage: Vector[Vector[Byte]]): Vector[Byte] = {
    val stripedImage =
      if (!directory.hasStripStorage) tiledImageToRowImage(uncompressedImage)
      else if (directory.bitsPerPixel == 1) stripBitImageOverflow(uncompressedImage)
      else uncompressedImage.flatten

    val image =
      if (directory.cellType == TypeFloat) flipToFloat(stripedImage)
      else if (directory.cellType == TypeDouble) flipToDouble(stripedImage)
      else stripedImage

    directory |-> gdalInternalNoDataLens get match {
      case Some(gdalNoData) => replaceGDALNoDataWithNODATA(image,
        gdalNoData, directory.cellType)
      case None => image
    }
  }

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

    for (i <- 0 until imageLength) {
      for (j <- 0 until tilesWidth) {
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

  def replaceGDALNoDataWithNODATA(image: Vector[Byte], gdalNoData: Double,
    cellType: CellType) = cellType match {
    case TypeByte => {
      val noData = gdalNoData.toByte
      image.map(x => if (x == noData) byteNODATA else x)
    }
    case TypeShort => {
      val newArr = image.toArray

      val noData = gdalNoData.toInt

      val noDataGT = Vector[Byte](
        (shortNODATA >> 8).toByte,
        (shortNODATA & 0xff).toByte
      )

      var i = 0
      while (i < image.size) {
        val short = image(i) << 8 + image(i + 1)

        if (short == noData)
          for (j <- 0 until 2)
            newArr(i + j) = noDataGT(j)

        i += 2
      }

      newArr.toVector
    }
    case TypeShort | TypeInt => {
      val newArr = image.toArray

      val noData = gdalNoData.toInt

      val indexRun = if (cellType == TypeShort) 2 else 4

      val noDataGT = Vector[Byte](
        (NODATA >> 24).toByte,
        ((NODATA & 0xff0000) >> 16).toByte,
        ((NODATA & 0xff00) >> 8).toByte,
        (NODATA & 0xff).toByte
      ).drop( if (cellType == TypeShort) 2 else 0)

      var i = 0
      while (i < image.size) {
        val v =
          if (cellType == TypeShort) image(i) << 8 + image(i + 1)
          else  image(i) << 24 + image(i + 1) << 16 + image(i + 2) << 8 + image(i + 3)

        if (v == noData)
          for (j <- 0 until indexRun)
            newArr(j + i) = noDataGT(j)

        i += indexRun
      }

      newArr.toVector
    }
    case TypeFloat | TypeDouble => {
      val newArr = image.toArray

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
            newArr(j) = bb.get

          bb.position(0)
        }

        i += indexRun
      }

      newArr.toVector
    }
    case TypeBit => image
  }

}
