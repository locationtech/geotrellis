/*
 * Copyright 2017 Azavea
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

package geotrellis.spark.costdistance

import geotrellis.layers.TileLayerMetadata
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector._
import org.apache.spark.rdd.RDD
import org.scalatest._


class RDDTimeSeriesMethodsSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val rdd = {
    val tile1 = IntConstantNoDataArrayTile(Array.fill[Int](25)(1), 5, 5)
    val tile2 = IntConstantNoDataArrayTile(Array.fill[Int](25)(2), 5, 5)
    val tile3 = IntConstantNoDataArrayTile(Array.fill[Int](25)(3), 5, 5)
    val tile4 = IntConstantNoDataArrayTile(Array.fill[Int](25)(4), 5, 5)
    val extent = Extent(0, 0, 10, 5)
    val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 10×5 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 10, 5)
    val bounds = Bounds(SpaceTimeKey(0, 0, 0), SpaceTimeKey(1, 0, 1))
    val tileLayerMetadata = TileLayerMetadata(
      IntConstantNoDataCellType,
      layoutDefinition,
      extent,
      LatLng,
      bounds
    )
    val list: List[(SpaceTimeKey, Tile)] = List(
      (SpaceTimeKey(0, 0, 0), tile1),
      (SpaceTimeKey(1, 0, 0), tile2),
      (SpaceTimeKey(0, 0, 1), tile3),
      (SpaceTimeKey(1, 0, 1), tile4)
    )
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  val polygon = MultiPolygon(Polygon(
    Point(-100,-100),
    Point(+100,-100),
    Point(+100,+100),
    Point(-100,+100),
    Point(-100,-100)
  ))

  describe("Time Series Extentension Methods") {

    it("Mean") {
      val expected = List(1.5, 3.5)
      val actual = rdd.meanSeries(polygon).map({ case (_, x) => x })
      actual should be (expected)
    }

    it("Min") {
      val expected = List(1.0, 3.0)
      val actual = rdd.minSeries(polygon).map({ case (_, x) => x })
      actual should be (expected)
    }

    it("Max") {
      val expected = List(2.0, 4.0)
      val actual = rdd.maxSeries(polygon).map({ case (_, x) => x })
      actual should be (expected)
    }

    it("Histogram") {
      val expected = List(Some(1.0), Some(3.0))
      val actual = rdd.histogramSeries(polygon).map({ case (_, x) => x.minValue() })
      actual should be (expected)
    }

    it("Sum") {
      val expected = List(25.0*(1+2), 25.0*(3+4))
      val actual = rdd.sumSeries(polygon).map({ case (_, x) => x })
      actual should be (expected)
    }

  }

}
