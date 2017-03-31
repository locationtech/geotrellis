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

package geotrellis.spark.equalization

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest._

class RDDHistogramEqualizationSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val _a = List(0)
  val b  = List(1)
  val c  = List(2)
  val d  = List(4)
  val e  = List(8)
  val f  = List(16)
  val g  = List(32)
  val h  = List(64)

  val data1 = (_a ++ b ++ c ++ d ++ List(0, 0, 0, 0))
  val data2 = (h ++ g ++ f ++ e ++ List(0, 0, 0, 0))

  describe("RDD Histogram Equalization") {

    it("should work with floating-point rasters") {
      val tile1: Tile = DoubleArrayTile(data1.map(_.toDouble).toArray, 1, 8)
      val tile2: Tile = DoubleArrayTile(data2.map(_.toDouble).toArray, 1, 8)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.map(_._2.toArrayDouble)

      array.head.head should be (Double.MinValue)
      array.last.head should be (Double.MaxValue)
    }

    it("should work with unsigned integral rasters") {
      val tile1: Tile = UShortArrayTile(data1.map(_.toShort).toArray, 1, 8, UShortCellType)
      val tile2: Tile = UShortArrayTile(data2.map(_.toShort).toArray, 1, 8, UShortCellType)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.map(_._2.toArray)

      array.head.head should be (0)
      array.last.head should be ((1<<16)-1)
    }

    it("should work with signed integral rasters") {
      val tile1: Tile = ShortArrayTile(data1.map(_.toShort).toArray, 1, 8, ShortCellType)
      val tile2: Tile = ShortArrayTile(data2.map(_.toShort).toArray, 1, 8, ShortCellType)
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.map(_._2.toArray)

      array.head.head should be (-(1<<15))
      array.last.head should be ((1<<15)-1)
    }

    it("should work with multiband tiles") {
      val data1 = (_a ++ b ++ List(0,0,0,0,0,0))
      val data2 = (List(0,0) ++ c ++ d ++ List(0,0,0,0))
      val data3 = (List(0,0,0,0) ++ e ++ f ++ List(0,0))
      val data4 = (List(0,0,0,0,0,0) ++ g ++ h)

      val tile1: MultibandTile = ArrayMultibandTile(
        ShortArrayTile(data1.map(_.toShort).toArray, 1, 8, ShortCellType),
        ShortArrayTile(data2.map(_.toShort).toArray, 1, 8, ShortCellType))
      val tile2: MultibandTile = ArrayMultibandTile(
        ShortArrayTile(data3.map(_.toShort).toArray, 1, 8, ShortCellType),
        ShortArrayTile(data4.map(_.toShort).toArray, 1, 8, ShortCellType))
      val rdd = ContextRDD(sc.parallelize(List((0, tile1), (1, tile2))), 33).equalize
      val array = rdd.collect.flatMap(_._2.bands.flatMap(_.toArray))

      array.head should be (-(1<<15))
      array.last should be ((1<<15)-1)
    }

  }
}
