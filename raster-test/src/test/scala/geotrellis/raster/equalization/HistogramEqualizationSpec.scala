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

package geotrellis.raster.equalization

import geotrellis.raster._
import geotrellis.raster.histogram._

import org.scalatest._


class HistogramEqualizationSpec extends FunSpec with Matchers
{
  val _a = List(0)
  val b  = List(1)
  val c  = List(2)
  val d  = List(4)
  val e  = List(8)
  val f  = List(16)
  val g  = List(32)
  val h  = List(64)

  val data = (_a ++ b ++ c ++ d ++ e ++ f ++ g ++ h)

  describe("Histogram Equalization") {

    it("should work on floating-point rasters") {
      val tile = DoubleArrayTile(data.map(_.toDouble).toArray, 1, 8).equalize
      val array = tile.toArrayDouble

      array.head should be (Double.MinValue)
      array.last should be (Double.MaxValue)
    }

    it("should work on unsigned integral rasters") {
      val tile = UShortArrayTile(data.map(_.toShort).toArray, 1, 8, UShortCellType).equalize
      val array = tile.toArray

      array.head should be (0)
      array.last should be ((1<<16)-1)
    }

    it("should work on signed integral rasters") {
      val tile = ShortArrayTile(data.map(_.toShort).toArray, 1, 8, ShortCellType).equalize
      val array = tile.toArray

      array.head should be (-(1<<15))
      array.last should be ((1<<15)-1)
    }

    it("should work when values do not map exactly to buckets") {
      val histogram = {
        val h = StreamingHistogram(4)
        h.countItems(
          List(StreamingHistogram.Bucket(1,1),
          StreamingHistogram.Bucket(2,2),
          StreamingHistogram.Bucket(4,4),
          StreamingHistogram.Bucket(8,8))
        )
        h
      }
      val lowerTileArray = DoubleArrayTile(Array(1.0,2.0,4.0), 1, 3, DoubleCellType).equalize(histogram).toArrayDouble
      val targetTileArray = DoubleArrayTile(Array(1.5,3.0,6.0), 1, 3, DoubleCellType).equalize(histogram).toArrayDouble
      val higherTileArray = DoubleArrayTile(Array(2.0,4.0,8.0), 1, 3, DoubleCellType).equalize(histogram).toArrayDouble

      (0 until 3).foreach({ i =>
        lowerTileArray(i) should be < (targetTileArray(i))
        targetTileArray(i) should be < (higherTileArray(i))
      })
    }

    it("should work on multiband tiles") {
      val data1 = (_a ++ b ++ c ++ d ++ List(0,0,0,0))
      val data2 = (List(0,0,0,0) ++ e ++ f ++ g ++ h)

      val tile1 = ShortArrayTile(data1.map(_.toShort).toArray, 1, 8, ShortCellType)
      val tile2 = ShortArrayTile(data2.map(_.toShort).toArray, 1, 8, ShortCellType)
      val tile = ArrayMultibandTile(tile1, tile2).equalize
      val array = tile.bands.flatMap(_.toArray)

      array.head should be (-(1<<15))
      array.last should be ((1<<15)-1)
    }

  }
}
