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

import geotrellis.raster.io.geotiff.tags._
import geotrellis.raster.io.geotiff.tags.codes.Orientations._

import monocle.syntax._

import spire.syntax.cfor._

object OrientationConverter {

  def apply(directory: Tags) = new OrientationConverter(
    directory.bitsPerPixel,
    (directory &|->
      Tags._nonBasicTags ^|->
      NonBasicTags._orientation get),
    directory.rows,
    directory.cols
  )

}

class OrientationConverter(
  bitsPerPixel: Int,
  orientation: Int,
  rows: Int,
  cols: Int) {

  def setCorrectOrientation(image: Array[Byte]): Array[Byte] = orientation match {
    case TopLeft => image
    case TopRight => convertTopRight(image)
    case BottomRight => convertBottomRight(image)
    case BottomLeft => convertBottomLeft(image)
    case LeftTop => convertLeftTop(image)
    case RightTop => convertRightTop(image)
    case RightBottom => convertRightBottom(image)
    case LeftBottom => convertLeftBottom(image)
    case _ =>
      throw new MalformedGeoTiffException(s"Orientation $orientation is not correct.")
  }

  private def convertTopRight(image: Array[Byte]) =
    swapColumns(image)

  private def convertBottomRight(image: Array[Byte]) =
    swapRows(swapColumns(image))

  private def convertBottomLeft(image: Array[Byte]) =
    swapRows(image)

  private def convertLeftTop(image: Array[Byte]) = ???

  private def convertRightTop(image: Array[Byte]) = ???

  private def convertRightBottom(image: Array[Byte]) = ???

  private def convertLeftBottom(image: Array[Byte]) = ???

  private def swapColumns(image: Array[Byte]) =
    if (bitsPerPixel != 1) {
      val bytes = bitsPerPixel / 8

      cfor(0)(_ < rows, _ + 1) { i =>
        cfor(0)(_ < cols / 2, _ + 1) { j =>
          cfor(0)(_ < bytes, _ + 1) { k =>
            val firstIndex = cols * i * bytes + (j * bytes) + k
            val secondIndex = cols * (i + 1) * bytes - (j * bytes) + k - bytes

            val t = image(firstIndex)
            image(firstIndex) = image(secondIndex)
            image(secondIndex) = t
          }
        }
      }

      image
    } else {
      cfor(0)(_ < rows, _ + 1) { i =>
        cfor(0)(_ < cols / 2, _ + 1) { j =>
          val firstIndex = cols * i + j
          val firstByteIndex = firstIndex / 8

          val secondIndex = cols * (i + 1) - j - 1
          val secondByteIndex = secondIndex / 8

          val firstBitIndex = (Int.MaxValue - firstIndex) % 8
          val secondBitIndex = (Int.MaxValue - secondIndex) % 8

          val first = (image(firstByteIndex) & (1 << firstBitIndex)) != 0
          val second = (image(secondByteIndex) & (1 << secondBitIndex)) != 0

          if (first != second) {
            image(firstByteIndex) = (image(firstByteIndex) ^ (1 << firstBitIndex)).toByte
            image(secondByteIndex) = (image(secondByteIndex) ^ (1 << secondBitIndex)).toByte
          }
        }
      }

      image
    }

  private def swapRows(image: Array[Byte]) =
    if (bitsPerPixel != 1) {
      val bytes = bitsPerPixel / 8

      cfor(0)(_ < rows / 2, _ + 1) { i =>
        cfor(0)(_ < cols * bytes, _ + 1) { j =>
          val firstIndex = i * cols * bytes + j
          val secondIndex = (rows - i - 1) * cols * bytes + j

          val t = image(firstIndex)
          image(firstIndex) = image(secondIndex)
          image(secondIndex) = t
        }
      }

      image
    } else {
      cfor(0)(_ < rows / 2, _ + 1) { i =>
        cfor(0)(_ < cols, _ + 1) { j =>
          val firstIndex = i * cols + j
          val firstByteIndex = firstIndex / 8

          val secondIndex = (rows - i - 1) * cols + j
          val secondByteIndex = secondIndex / 8

          val firstBitIndex = (Int.MaxValue - firstIndex) % 8
          val secondBitIndex = (Int.MaxValue - secondIndex) % 8

          val first = (image(firstByteIndex) & (1 << firstBitIndex)) != 0
          val second = (image(secondByteIndex) & (1 << secondBitIndex)) != 0

          if (first != second) {
            image(firstByteIndex) = (image(firstByteIndex) ^ (1 << firstBitIndex)).toByte
            image(secondByteIndex) = (image(secondByteIndex) ^ (1 << secondBitIndex)).toByte
          }
        }
      }

      image
    }

}
