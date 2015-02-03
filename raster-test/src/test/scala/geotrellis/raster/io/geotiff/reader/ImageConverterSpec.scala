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
import geotrellis.raster.io.geotiff.reader._

import geotrellis.testkit._

import scala.io.{Source, Codec}

import java.util.BitSet

import org.scalatest._

class ImageConverterSpec extends FunSpec with Matchers {

  private def createTiledDirectory(width: Int, length: Int,
    bitsPerPixel: Int, tWidth: Int, tLength: Int) = ImageDirectory(
    count = 0,
      basicTags = BasicTags(bitsPerSample = Some(Array(bitsPerPixel)),
        imageLength = length, imageWidth = width
      ),
      tileTags = TileTags(tileWidth = Some(tWidth), tileLength = Some(tLength))
  )

  describe ("byte tiled image to byte row image conversion") {

    it ("should convert first byte tiled image to a byte row image and match correct result") {

      val imageWidth = 8
      val imageLength = 5

      val tileWidth = 3
      val tileLength = 3

      val bitsPerPixel = 8

      val directory = createTiledDirectory(imageWidth, imageLength, bitsPerPixel,
        tileWidth, tileLength)

      val imageConverter = ImageConverter(directory, false)

      val tiled: Array[Array[Byte]] = Array(
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        )
      )

      val convertedImage = imageConverter.convert(tiled)

      val correct: Array[Byte] = Array(
        1, 2, 1, 1, 2, 1, 1, 2,
        1, 2, 1, 1, 2, 1, 1, 2,
        3, 1, 2, 3, 1, 2, 3, 1,
        1, 2, 1, 1, 2, 1, 1, 2,
        1, 2, 1, 1, 2, 1, 1, 2
      )

      convertedImage.size should equal (correct.size)
      convertedImage should equal (correct)
    }

    it ("should convert second byte tiled image to a byte row image and match correct result") {

      val imageWidth = 6
      val imageLength = 6

      val tileWidth = 3
      val tileLength = 3

      val bitsPerPixel = 8

      val directory = createTiledDirectory(imageWidth, imageLength, bitsPerPixel,
        tileWidth, tileLength)

      val imageConverter = ImageConverter(directory, false)

      val tiled: Array[Array[Byte]] = Array(
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Array[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        )
      )

      val convertedImage = imageConverter.convert(tiled)

      val correct: Array[Byte] = Array(
        1, 2, 1, 1, 2, 1,
        1, 2, 1, 1, 2, 1,
        3, 1, 2, 3, 1, 2,
        1, 2, 1, 1, 2, 1,
        1, 2, 1, 1, 2, 1,
        3, 1, 2, 3, 1, 2
      )

      convertedImage.size should equal (correct.size)
      convertedImage should equal (correct)

    }
  }

  // Note that the TIFF format has byte inversions on these bit images.
  // So in each test I invert the bytes before sending it into the converter.
  describe ("bit tiled image to bit row image conversion") {

    it ("should convert first bit tiled image to a bit row image and match correct result") {

      /* The target matrix looks like this:

       0 1 0 1 0 1
       1 1 1 1 1 1
       0 1 0 1 0 1
       1 1 1 1 1 1
       0 1 0 1 0 1
       1 1 1 1 1 1

       */

      /* The tiled matrix looks like this:

       0 1 0 1 0 1 0 1
       1 1 1 1 1 1 1 1
       0 1 0 1 0 1 0 1
       1 1 1 1 1 1 1 1
       0 1 0 1 0 1 0 1
       1 1 1 1 1 1 1 1
       0 1 0 1 0 1 0 1
       1 1 1 1 1 1 1 1

       */

      val imageWidth = 6
      val imageLength = 6

      val tileWidth = 4
      val tileLength = 4

      val bitsPerPixel = 1

      val directory = createTiledDirectory(imageWidth, imageLength, bitsPerPixel,
        tileWidth, tileLength)

      val imageConverter = ImageConverter(directory, false)

      val firstTileBitSet = new BitSet(4)
      val secondTileBitSet = new BitSet(4)

      for (i <- 0 until tileWidth) {
        secondTileBitSet.set(i)

        if (i % 2 == 1)
          firstTileBitSet.set(i)
      }

      val tile = (firstTileBitSet.toByteArray ++
        secondTileBitSet.toByteArray ++ firstTileBitSet.toByteArray ++
        secondTileBitSet.toByteArray).map(invertByte(_))



      val tiled: Array[Array[Byte]] = Array(
        tile, tile,
        tile, tile
      )

      val convertedImage = imageConverter.convert(tiled)

      val correctBitSet = new BitSet()
      for (i <- 0 until imageWidth)
        for (j <- 0 until imageLength)
          if (i % 2 == 1 || j % 2 == 1) correctBitSet.set(i * imageWidth + j)

      val correct = correctBitSet.toByteArray

      convertedImage.size should equal (correct.size)
      convertedImage should equal (correct)

    }

    it ("should convert second bit tiled image to a bit row image and match correct result") {

      /* The target matrix looks like this:

       0 1 0 1 0 1
       1 1 1 1 1 1
       0 1 0 1 0 1
       1 1 1 1 1 1
       0 1 0 1 0 1
       1 1 1 1 1 1

       */

      val imageWidth = 6
      val imageLength = 6

      val tileWidth = 3
      val tileLength = 3

      val bitsPerPixel = 1

      val directory = createTiledDirectory(imageWidth, imageLength, bitsPerPixel,
        tileWidth, tileLength)

      val imageConverter = ImageConverter(directory, false)

      val firstTileBitSet = new BitSet(3)
      val secondTileBitSet = new BitSet(3)
      val thirdTileBitSet = new BitSet(3)

      for (i <- 0 until tileWidth) {
        secondTileBitSet.set(i)

        if (i % 2 == 1) firstTileBitSet.set(i)
        else thirdTileBitSet.set(i)
      }

      val firstTile = (firstTileBitSet.toByteArray ++
        secondTileBitSet.toByteArray ++ firstTileBitSet.toByteArray)
        .map(invertByte(_))

      val secondTile = (thirdTileBitSet.toByteArray ++
        secondTileBitSet.toByteArray ++ thirdTileBitSet.toByteArray)
        .map(invertByte(_))

      val thirdTile = (secondTileBitSet.toByteArray ++
        firstTileBitSet.toByteArray ++ secondTileBitSet.toByteArray)
        .map(invertByte(_))

      val fourthTile = (secondTileBitSet.toByteArray ++
        thirdTileBitSet.toByteArray ++ secondTileBitSet.toByteArray)
        .map(invertByte(_))

      val tiled: Array[Array[Byte]] = Array(
        firstTile, secondTile,
        thirdTile, fourthTile
      )

      val convertedImage = imageConverter.convert(tiled)

      val correctBitSet = new BitSet()
      for (i <- 0 until imageWidth)
        for (j <- 0 until imageLength)
          if (i % 2 == 1 || j % 2 == 1) correctBitSet.set(i * imageWidth + j)

      val correct = correctBitSet.toByteArray

      convertedImage.size should equal (correct.size)
      convertedImage should equal (correct)

    }

  }

}
