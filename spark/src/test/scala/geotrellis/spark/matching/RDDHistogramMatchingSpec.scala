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

package geotrellis.spark.matching

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.testkit._
import geotrellis.raster.histogram.StreamingHistogram

import org.scalatest._

class RDDHistogramMatchingSpec extends FunSpec
    with Matchers
    with TestEnvironment {

    val sourceHistogram = {
      val h = StreamingHistogram(42)
      h.countItem(1, 1)
      h.countItem(2, 2)
      h.countItem(4, 3)
      h.countItem(8, 4)
      h.countItem(16, 5)
      h
    }

    val targetHistogram = {
      val h = StreamingHistogram(42)
      h.countItem(1, 1)
      h.countItem(2, 2)
      h.countItem(3, 3)
      h.countItem(4, 4)
      h.countItem(5, 5)
      h
    }

  val sourceHistograms = List(sourceHistogram, sourceHistogram)
  val targetHistograms = List(targetHistogram, targetHistogram)

  describe("RDD Histogram Matching") {

    it("should work with floating-point rasters") {
      val tile1 = DoubleArrayTile(Array[Double](16, 1, 2), 1, 3).asInstanceOf[Tile]
      val tile2 = DoubleArrayTile(Array[Double](4, 8, 16), 1, 3).asInstanceOf[Tile]
      val rdd = ContextRDD(sc.parallelize(List((SpatialKey(0,0), tile1), (SpatialKey(0,0), tile2))), 33)
      val actual = rdd.matchHistogram(sourceHistogram, targetHistogram).flatMap({ _._2.toArray.toList }).collect
      val expected = List[Double](5, 1, 2, 3, 4, 5)

      actual should be (expected)
    }

    it("should work with unsigned integral rasters") {
      val tile1 = UShortArrayTile(Array[Short](16, 1, 2), 1, 3).asInstanceOf[Tile]
      val tile2 = UShortArrayTile(Array[Short](4, 8, 16), 1, 3).asInstanceOf[Tile]
      val rdd = ContextRDD(sc.parallelize(List((SpatialKey(0,0), tile1), (SpatialKey(0,0), tile2))), 33)
      val actual = rdd.matchHistogram(sourceHistogram, targetHistogram).flatMap({ _._2.toArray.toList }).collect
      val expected = List[Double](5, 1, 2, 3, 4, 5)

      actual should be (expected)
    }

    it("should work with signed integral rasters") {
      val tile1 = ShortArrayTile(Array[Short](16, 1, 2), 1, 3).asInstanceOf[Tile]
      val tile2 = ShortArrayTile(Array[Short](4, 8, 16), 1, 3).asInstanceOf[Tile]
      val rdd = ContextRDD(sc.parallelize(List((SpatialKey(0,0), tile1), (SpatialKey(0,0), tile2))), 33)
      val actual = rdd.matchHistogram(sourceHistogram, targetHistogram).flatMap({ _._2.toArray.toList }).collect
      val expected = List[Double](5, 1, 2, 3, 4, 5)

      actual should be (expected)
    }

    it("should work with multiband tiles") {
      val band1 = ShortArrayTile(Array[Short](16, 1, 2), 1, 3).asInstanceOf[Tile]
      val band2 = ShortArrayTile(Array[Short](4, 8, 16), 1, 3).asInstanceOf[Tile]
      val tile1 = ArrayMultibandTile(band1, band2)
      val tile2 = ArrayMultibandTile(band2, band1)
      val rdd = ContextRDD(sc.parallelize(List((SpatialKey(0,0), tile1), (SpatialKey(0,0), tile2))), 33)
      val actual = rdd
        .matchHistogram(sourceHistograms, targetHistograms)
        .flatMap({ case (_, v) =>
          v.bands.flatMap({ _.toArray.toList })
        }).collect
      val expected = List[Double](5, 1, 2, 3, 4, 5, 3, 4, 5, 5, 1, 2)

      actual should be (expected)
    }

  }
}
