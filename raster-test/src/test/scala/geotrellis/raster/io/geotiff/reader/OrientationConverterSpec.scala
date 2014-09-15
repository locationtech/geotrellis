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
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils._

import org.scalatest._

class OrientationConverterSpec extends FunSpec with Matchers {

  describe ("top right orientations to top left orientations") {

    it ("should convert single byte top right orientation with odd nr of cols correctly") {
      val input = Array[Byte](
        1, 2, 3, 4, 5,
        5, 4, 3, 2, 1
      )

      val correct = Array[Byte](
        5, 4, 3, 2, 1,
        1, 2, 3, 4, 5
      )

      val orientationConverter = new OrientationConverter(8, 2, 2, 5)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

    it ("should convert single byte top right orientation with even nr of cols correctly") {
      val input = Array[Byte](
        1, 2, 3, 4,
        4, 3, 2, 1
      )

      val correct = Array[Byte](
        4, 3, 2, 1,
        1, 2, 3, 4
      )

      val orientationConverter = new OrientationConverter(8, 2, 2, 4)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

    it ("should convert two byte top right orientation with odd nr of cols correctly") {
      val input = Array[Byte](
        1, 2, 3, 4, 5, 6,
        6, 5, 4, 3, 2, 1
      )

      val correct = Array[Byte](
        5, 6, 3, 4, 1, 2,
        2, 1, 4, 3, 6, 5
      )

      val orientationConverter = new OrientationConverter(16, 2, 2, 3)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

    it ("should convert two byte top right orientation with even nr of cols correctly") {
      val input = Array[Byte](
        1, 2, 3, 4, 5, 6, 7, 8,
        8, 7, 6, 5, 4, 3, 2, 1
      )

      val correct = Array[Byte](
        7, 8, 5, 6, 3, 4, 1, 2,
        2, 1, 4, 3, 6, 5, 8, 7
      )

      val orientationConverter = new OrientationConverter(16, 2, 2, 4)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

    it ("should convert single bit top right orientation with odd nr of columns correctly") {
      val input = Array[Byte](
        -112,
        -102,
        -79
      )

      /*
       100 first byte: 100 100 00
       100 second byte: 1 001 101 0
       001 third byte: 10 110 001
       001
       101
       010
       110
       001

       001 first byte:
       001
       100
       100
       101
       010
       011
       100
       */

      val correct = Array[Byte](
        38,
        74,
        -100
      )

      val orientationConverter = new OrientationConverter(1, 2, 8, 3)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

    it ("should convert single bit top right orientation with even nr of columns correctly") {
      val input = Array[Byte](
        -16,
        15
      )

      val correct = Array[Byte](
        15,
        -16
      )

      val orientationConverter = new OrientationConverter(1, 2, 2, 8)

      val converted = orientationConverter.setCorrectOrientation(input)

      converted should equal (correct)
    }

  }

}
