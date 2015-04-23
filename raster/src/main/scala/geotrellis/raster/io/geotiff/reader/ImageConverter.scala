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

import geotrellis.raster._
import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.utils._

import monocle.syntax._

import java.util.BitSet

import java.nio.ByteBuffer

import spire.syntax.cfor._

object ImageConverter {

  def apply(tags: Tags, isBigEndian: Boolean) =
    new ImageConverter(tags, isBigEndian)

  def flip(image: Array[Byte], flipSize: Int): Array[Byte] = {
    val size = image.size
    val arr = Array.ofDim[Byte](size)

    var i = 0
    while (i < size) {
      var j = 0
      while (j < flipSize) {
        arr(i + j) = image(i + flipSize - 1 - j)
        j += 1
      }

      i += flipSize
    }

    arr
  }
}

class ImageConverter(tags: Tags, isBigEndian: Boolean) {

  def convert(uncompressedImage: Array[Array[Byte]]): Array[Byte] = {
    val bitsPerPixel = tags.bitsPerPixel

    val stripedImage =
      if (!tags.hasStripStorage) tiledImageToRowImage(uncompressedImage)
      else if (bitsPerPixel == 1) stripBitImageOverflow(uncompressedImage)
      else uncompressedImage.flatten

    if (!isBigEndian && bitsPerPixel > 8) ImageConverter.flip(stripedImage, bitsPerPixel / 8)
    else stripedImage
  }

  

  // TODO: Why isn't this used?
  private def setCorrectOrientation(image: Array[Byte]) =
    OrientationConverter(tags).setCorrectOrientation(image)

  private def tiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {
    if (tags.bitsPerPixel == 1) bitTiledImageToRowImage(tiledImage)
    else byteTiledImageToRowImage(tiledImage)
  }

  private def bitTiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {

    val tiledImageBitSets = tiledImage.map(x => BitSet.valueOf(x.map(invertByte(_))))

    val tileWidth = tags.rowSize
    val tileLength = tags.rowsInSegment(0)

    val imageWidth = tags.cols
    val imageLength = tags.rows

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

  private def byteTiledImageToRowImage(tiledImage: Array[Array[Byte]]): Array[Byte] = {
    val bytesPerPixel = (tags.bitsPerPixel + 7) / 8

    val tileCols = tags.rowSize
    val tileRows = 
      (tags &|->
        Tags._tileTags ^|->
        TileTags._tileLength get).get.toInt

    val totalCols = tags.cols
    val totalRows = tags.rows

    val widthOverflow = totalCols % tileCols

    val layoutCols = totalCols / tileCols + (if (widthOverflow > 0) 1 else 0)
    val tilesHieght = math.ceil(totalRows / tileRows).toInt

    val resArray = Array.ofDim[Byte](totalCols * totalRows * bytesPerPixel)
    var resArrayIndex = 0

    cfor(0)(_ < totalRows, _ + 1) { row =>
      cfor(0)(_ < layoutCols, _ + 1) { layoutCol =>
        val layoutRow = row / tileRows
        val tile = tiledImage( (layoutRow * layoutCols) + layoutCol)

        val start = (row % tileRows) * tileCols * bytesPerPixel
        val length = 
          bytesPerPixel * (if (layoutCol == layoutCols - 1 && widthOverflow != 0) widthOverflow else tileCols)

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

  private def bitSetCopy(src: BitSet, srcPos: Int, dest: BitSet, destPos: Int, length: Int): Unit = {
    cfor(0)(_ < length, _ + 1) { i =>
      if (src.get(srcPos + i)) dest.set(destPos + i)
    }
  }

  private def stripBitImageOverflow(image: Array[Array[Byte]]) = {
    val imageWidth = tags.cols
    val imageHeight = tags.rows

    val rowBitSetsArray = Array.ofDim[BitSet](imageHeight)
    var rowBitSetsIndex = 0

    val rowByteSize = (imageWidth + 7) / 8
    val tempBitSetArray = Array.ofDim[Byte](rowByteSize)

    cfor(0)(_ < image.size, _ + 1) { i =>
      val rowsInSegment = tags.rowsInSegment(i)
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

    val resSize = imageWidth * imageHeight
    val resBitSet = new BitSet(resSize)

    cfor(0)(_ < imageHeight, _ + 1) { i =>
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
