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

import geotrellis.layers.{Metadata, TileLayerMetadata}
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector._
import org.apache.spark.rdd.RDD
import org.scalatest._


class RDDCostDistanceMethodsSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val rdd: RDD[(SpatialKey, Tile)] with Metadata[TileLayerMetadata[SpatialKey]] = {
    val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val extent = Extent(0, 0, 10, 5)
    val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 10×5 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 10, 5)
    val bounds = Bounds(SpatialKey(0,0), SpatialKey(1,0))
    val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
    val list = List((SpatialKey(0,0), tile), (SpatialKey(1,0), tile))
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  val points = List(Point(2.5+5.0, 2.5))

  describe("Cost-Distance Extension Methods") {

    it("The costdistance Method Should Work (1/2)") {
      val expected = IterativeCostDistance(rdd, points).collect.toList
      val actual = rdd.costdistance(points).collect.toList

      actual should be (expected)
    }

    it("The costdistance Method Should Work (2/2)") {
      val resolution = IterativeCostDistance.computeResolution(rdd)
      val expected = IterativeCostDistance(rdd, points, resolution).collect.toList
      val actual = rdd.costdistance(points, resolution).collect.toList

      actual should be (expected)
    }

  }

}
