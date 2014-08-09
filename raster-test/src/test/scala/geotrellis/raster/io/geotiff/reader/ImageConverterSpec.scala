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
import geotrellis.testkit._

import scala.io.{Source, Codec}

import java.util.BitSet
import geotrellis.raster.io.geotiff.reader.utils.BitSetUtils._
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils._

import org.scalatest._

class ImageConverterSpec extends FunSpec with MustMatchers {

  private def createTiledDirectory(width: Int, length: Int,
    bitsPerPixel: Int, tWidth: Int, tLength: Int) = ImageDirectory(
    count = 0,
      basicTags = BasicTags(bitsPerSample = Some(Vector(bitsPerPixel)),
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

      val imageConverter = ImageConverter(directory)

      val tiled: Vector[Vector[Byte]] = Vector(
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        )
      )

      val convertedImage = imageConverter.convert(tiled)

      val correct: Vector[Byte] = Vector(
        1, 2, 1, 1, 2, 1, 1, 2,
        1, 2, 1, 1, 2, 1, 1, 2,
        3, 1, 2, 3, 1, 2, 3, 1,
        1, 2, 1, 1, 2, 1, 1, 2,
        1, 2, 1, 1, 2, 1, 1, 2
      )

      convertedImage.size must equal (correct.size)
      convertedImage must equal (correct)
    }

    it ("should convert second byte tiled image to a byte row image and match correct result") {

      val imageWidth = 6
      val imageLength = 6

      val tileWidth = 3
      val tileLength = 3

      val bitsPerPixel = 8

      val directory = createTiledDirectory(imageWidth, imageLength, bitsPerPixel,
        tileWidth, tileLength)

      val imageConverter = ImageConverter(directory)

      val tiled: Vector[Vector[Byte]] = Vector(
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        ),
        Vector[Byte](
          1, 2, 1,
          1, 2, 1,
          3, 1, 2
        )
      )

      val convertedImage = imageConverter.convert(tiled)

      val correct: Vector[Byte] = Vector(
        1, 2, 1, 1, 2, 1,
        1, 2, 1, 1, 2, 1,
        3, 1, 2, 3, 1, 2,
        1, 2, 1, 1, 2, 1,
        1, 2, 1, 1, 2, 1,
        3, 1, 2, 3, 1, 2
      )

      convertedImage.size must equal (correct.size)
      convertedImage must equal (correct)

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

      val imageConverter = ImageConverter(directory)

      val firstTileBitSet = new BitSet(4)
      val secondTileBitSet = new BitSet(4)

      for (i <- 0 until tileWidth) {
        secondTileBitSet.set(i)

        if (i % 2 == 1)
          firstTileBitSet.set(i)
      }

      val tile = (firstTileBitSet.toByteVector(4) ++
        secondTileBitSet.toByteVector(4) ++ firstTileBitSet.toByteVector(4) ++
        secondTileBitSet.toByteVector(4)).map(invertByte(_))



      val tiled: Vector[Vector[Byte]] = Vector(
        tile, tile,
        tile, tile
      )

      val convertedImage = imageConverter.convert(tiled)

      val correctBitSet = new BitSet()
      for (i <- 0 until imageWidth)
        for (j <- 0 until imageLength)
          if (i % 2 == 1 || j % 2 == 1) correctBitSet.set(i * imageWidth + j)

      val correct = correctBitSet.toByteVector(36)

      convertedImage.size must equal (correct.size)
      convertedImage must equal (correct)

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

      val imageConverter = ImageConverter(directory)

      val firstTileBitSet = new BitSet(3)
      val secondTileBitSet = new BitSet(3)
      val thirdTileBitSet = new BitSet(3)

      for (i <- 0 until tileWidth) {
        secondTileBitSet.set(i)

        if (i % 2 == 1) firstTileBitSet.set(i)
        else thirdTileBitSet.set(i)
      }

      val firstTile = (firstTileBitSet.toByteVector(3) ++
        secondTileBitSet.toByteVector(3) ++ firstTileBitSet.toByteVector(3))
        .map(invertByte(_))

      val secondTile = (thirdTileBitSet.toByteVector(3) ++
        secondTileBitSet.toByteVector(3) ++ thirdTileBitSet.toByteVector(3))
        .map(invertByte(_))

      val thirdTile = (secondTileBitSet.toByteVector(3) ++
        firstTileBitSet.toByteVector(3) ++ secondTileBitSet.toByteVector(3))
        .map(invertByte(_))

      val fourthTile = (secondTileBitSet.toByteVector(3) ++
        thirdTileBitSet.toByteVector(3) ++ secondTileBitSet.toByteVector(3))
        .map(invertByte(_))

      val tiled: Vector[Vector[Byte]] = Vector(
        firstTile, secondTile,
        thirdTile, fourthTile
      )

      val convertedImage = imageConverter.convert(tiled)

      val correctBitSet = new BitSet()
      for (i <- 0 until imageWidth)
        for (j <- 0 until imageLength)
          if (i % 2 == 1 || j % 2 == 1) correctBitSet.set(i * imageWidth + j)

      val correct = correctBitSet.toByteVector(36)

      convertedImage.size must equal (correct.size)
      convertedImage must equal (correct)

    }

  }

}
