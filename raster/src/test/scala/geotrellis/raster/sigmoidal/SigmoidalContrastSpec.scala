/*
 * Copyright 2016 Azavea
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.sigmoidal

import geotrellis.raster._

import org.scalatest._


class SigmoidalContrastSpec extends FunSpec with Matchers
{
  describe("Sigmoidal Contrast") {

    it("should work on floating-point rasters") {
      val x = Double.MaxValue / 2.0
      val a = Double.MinValue
      val b = 0
      val c = Double.MaxValue
      val data = List(a, a+x, b, c-x, c)
      val tile = DoubleArrayTile(data.map(_.toDouble).toArray, 1, 5).sigmoidal(0.5, 10)
      val array = tile.toArrayDouble

      (array(0)/a)  should be <= (1.2)
      (array(1)/a) should be <= (1.2)
      (c/array(3)) should be <= (1.2)
      (c/array(4)) should be <= (1.2)
    }

    it("should work on unsigned integral rasters") {
      val x = 1<<14
      val a = 0
      val b = 1<<15
      val c = (1<<16)-1
      val data = List(a, a+x, b, c-x, c).map(_.toShort)
      val tile = UShortArrayTile(data.map(_.toShort).toArray, 1, 5, UShortCellType).sigmoidal(0.5, 10)
      val array = tile.toArray

      (array(0) - a)  should be <= (442)
      array(1) should be <= x
      (array(2) - b) should be <= (442)
      array(3) should be >= c-x
      (c - array(4)) should be <= (442)
    }

    it("should work on signed integral rasters") {
      val x = 1<<14
      val a = -(1<<15)
      val b = 0
      val c = (1<<15)-1
      val data = List(a, a+x, b, c-x, c).map(_.toShort)
      val tile = ShortArrayTile(data.map(_.toShort).toArray, 1, 5, ShortCellType).sigmoidal(0.5, 10)
      val array = tile.toArray

      (array(0) - a)  should be <= (442)
      array(1) should be <= x
      (array(2) - b) should be <= (442)
      array(3) should be >= c-x
      (c - array(4)) should be <= (442)
    }

    it("should work on multiband tiles") {
      val x = 1<<14
      val a = -(1<<15)
      val b = 0
      val c = (1<<15)-1
      val data1 = List(a, a+x, b).map(_.toShort)
      val data2 = List(b, c-x, c).map(_.toShort)

      val tile1 = ShortArrayTile(data1.map(_.toShort).toArray, 1, 3, ShortCellType)
      val tile2 = ShortArrayTile(data2.map(_.toShort).toArray, 1, 3, ShortCellType)
      val tile = ArrayMultibandTile(tile1, tile2).equalize
      val array = tile.bands.flatMap(_.toArray)

      (array.head - a) should be <= (442)
      (c - array.last) should be <= (442)
    }

  }
}
