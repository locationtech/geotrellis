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

package geotrellis.spark.timeseries

import geotrellis.layers.TileLayerMetadata
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector._
import org.scalatest._


object TimeSeriesSpecFunctions {

  def projection(tile: Tile): Set[Int] =
    tile.toArray.toSet.filter(_ > 0)

  def reduction(left: Set[Int], right: Set[Int]): Set[Int] =
    left ++ right
}

class TimeSeriesSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val rdd = {
    val tile1 = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val tile2 = IntArrayTile(Array.fill[Int](25)(2), 5, 5)
    val tile3 = IntArrayTile(Array.fill[Int](25)(3), 5, 5)
    val tile4 = IntArrayTile(Array.fill[Int](25)(4), 5, 5)
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

  describe("Time Series Capability") {

    it("Should handle queries within tile boundaries") {
      val polygon = MultiPolygon(Polygon(
        Point(0,0),
        Point(4,0),
        Point(4,4),
        Point(0,4),
        Point(0,0)
      ))

      val expected = List(Set(1), Set(3))
      val actual =
        TimeSeries(
          rdd,
          TimeSeriesSpecFunctions.projection,
          TimeSeriesSpecFunctions.reduction,
          List(polygon)
        )
          .collect()
          .toList
          .map({ case (_, s) => s })

      actual should be (expected)
    }

    it("Should handle queries across tile boundaries") {
      val polygon = MultiPolygon(Polygon(
        Point(0,0),
        Point(11,0),
        Point(11,5),
        Point(0,5),
        Point(0,0)
      ))

      val expected = List(Set(1,2), Set(3,4))
      val actual =
        TimeSeries(
          rdd,
          TimeSeriesSpecFunctions.projection,
          TimeSeriesSpecFunctions.reduction,
          List(polygon)
        )
          .collect()
          .toList
          .map({ case (_, s) => s })

      actual should be (expected)
    }

  }

}
