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

import geotrellis.io.geotiff.ImageDirectoryLenses._
import geotrellis.io.geotiff.utils.BitSetUtils._

case class ImageConverter(directory: ImageDirectory) {

  def convert(uncompressedImage: Vector[Byte]): Vector[Byte] = {
    val rowImage = if (!directory.hasStripStorage)
      tiledImageToRowImage(uncompressedImage)
    else uncompressedImage

    rowImage
  }

  private def tiledImageToRowImage(tiledImage: Vector[Byte]) = {

    val bitsPerPixel = directory.bitsPerPixel
    val bytesPerPixel = bitsPerPixel / 8

    lazy val fromBitSet = BitSet.valueOf(tiledImage.toArray)

    val tilePixelWidth = directory.rowSize
    val tilePixelHeight = directory.rowsInSegment(0)

    val tilePixelArea = tilePixelWidth * tilePixelHeight

    val imageWidth = (directory |-> imageWidthLens get).toInt
    val imageHeight = (directory |-> imageLengthLens get).toInt

    val tilesWidth = (imageWidth / tilePixelWidth + (
      if (imageWidth % tilePixelWidth > 0) 1 else 0)).toInt

    val heightRes = imageHeight % tilePixelHeight

    val tilesHeight = (imageHeight / tilePixelHeight + (
      if (heightRes > 0) 1 else 0)).toInt

    lazy val resArray = Array.ofDim[Byte](imageWidth * imageHeight)
    lazy val toBitSet = new BitSet(imageWidth * imageHeight)
    lazy val tiledImageArray = tiledImage.toArray

    for (i <- 0 until imageHeight) {
      for (j <- 0 until tilesWidth) {
        val heightOffset = (i / tilesHeight) * tilesWidth * tilePixelArea
        val widthOffset = j * tilePixelArea

        val tileOffset = j * (imageHeight %
          tilePixelHeight) * tilePixelWidth

        val length = if (j == tilesWidth - 1 &&
          heightRes != 0) heightRes else tilePixelHeight

        val start = heightOffset + widthOffset + tileOffset

        if (bitsPerPixel == 1)
          copyToBitSet(fromBitSet, tileOffset, toBitSet, i + j * tilePixelWidth,
            length)
        else
          System.arraycopy(tiledImageArray, tileOffset, resArray,
            i + j * tilePixelWidth, length)
      }
    }

    if (bitsPerPixel == 1) toBitSet.toByteVector
    else resArray.toVector
  }

  private def copyToBitSet(from: BitSet, fromOffset: Int, to: BitSet,
    toOffset: Int, length: Int) = for (i <- 0 until length)
    if (from.get(i + fromOffset)) to.set(toOffset + i)

}
