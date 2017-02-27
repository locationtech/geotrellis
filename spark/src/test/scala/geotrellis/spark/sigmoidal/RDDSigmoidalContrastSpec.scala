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

package geotrellis.spark.sigmoidal

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest._

class RDDSigmoidalContrastSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  describe("RDD Sigmoidal Contrast") {

    it("should work with floating-point rasters") {
      val x = Double.MaxValue / 2.0
      val a = Double.MinValue
      val b = 0
      val c = Double.MaxValue
      val data1 = List(a, a+x)
      val data2 = List(b, c-x, c)

      val tile1: Tile = DoubleArrayTile(data1.map(_.toDouble).toArray, 1, 2)
      val tile2: Tile = DoubleArrayTile(data2.map(_.toDouble).toArray, 1, 3)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).sigmoidal(.5, 10)
      val array = rdd.collect.flatMap(_._2.toArrayDouble)

      (array(0)/a)  should be <= (1.2)
      (array(1)/a) should be <= (1.2)
      (c/array(3)) should be <= (1.2)
      (c/array(4)) should be <= (1.2)
    }

    it("should work with unsigned integral rasters") {
      val x = 1<<14
      val a = 0
      val b = 1<<15
      val c = (1<<16)-1
      val data1 = List(a, a+x).map(_.toShort)
      val data2 = List(b, c-x, c).map(_.toShort)

      val tile1: Tile = UShortArrayTile(data1.toArray, 1, 2, UShortCellType)
      val tile2: Tile = UShortArrayTile(data2.toArray, 1, 3, UShortCellType)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.flatMap(_._2.toArray)

      (array(0) - a)  should be <= (442)
      array(1) should be <= x
      (array(2) - b) should be <= (442)
      array(3) should be >= c-x
      (c - array(4)) should be <= (442)
    }

    it("should work with signed integral rasters") {
      val x = 1<<14
      val a = -(1<<15)
      val b = 0
      val c = (1<<15)-1
      val data1 = List(a, a+x).map(_.toShort)
      val data2 = List(b, c-x, c).map(_.toShort)

      val tile1: Tile = ShortArrayTile(data1.toArray, 1, 2, ShortCellType)
      val tile2: Tile = ShortArrayTile(data2.toArray, 1, 3, ShortCellType)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.flatMap(_._2.toArray)

      (array(0) - a)  should be <= (442)
      array(1) should be <= x
      (array(2) - b) should be <= (442)
      array(3) should be >= c-x
      (c - array(4)) should be <= (442)
    }

    it("should work with multiband tiles") {
      val x = 1<<14
      val a = -(1<<15)
      val b = 0
      val c = (1<<15)-1
      val data1 = List(a, a+x, b).map(_.toShort)
      val data2 = List(b, c-x, c).map(_.toShort)

      val tile1: MultibandTile = ArrayMultibandTile(
        ShortArrayTile(data1.toArray, 1, 3, ShortCellType),
        ShortArrayTile(data1.toArray, 1, 3, ShortCellType))
      val tile2: MultibandTile = ArrayMultibandTile(
        ShortArrayTile(data2.toArray, 1, 3, ShortCellType),
        ShortArrayTile(data2.toArray, 1, 3, ShortCellType))
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.flatMap(_._2.bands.flatMap(_.toArray))

      (array.head - a) should be <= (442)
      (c - array.last) should be <= (442)
    }

  }
}
