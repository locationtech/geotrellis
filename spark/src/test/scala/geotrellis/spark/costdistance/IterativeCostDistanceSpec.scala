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
import org.scalatest._


class IterativeCostDistanceSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val rdd1 = {
    val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val skey = SpatialKey(0, 0)
    val extent = Extent(0, 0, 5, 5)
    val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 5×5 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 5)
    val bounds = Bounds(skey, skey)
    val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
    val list = List((skey, tile))
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  val rdd2 = {
    val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val extent = Extent(0, 0, 10, 5)
    val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 10×5 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 10, 5)
    val bounds = Bounds(SpatialKey(0,0), SpatialKey(1,0))
    val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
    val list = List((SpatialKey(0,0), tile), (SpatialKey(1,0), tile))
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  val rdd3 = {
    val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val extent = Extent(0, 0, 5, 10)
    val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 5×10 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 5, 10)
    val bounds = Bounds(SpatialKey(0,0), SpatialKey(0,1))
    val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
    val list = List((SpatialKey(0,0), tile), (SpatialKey(0,1), tile))
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  describe("Iterative Cost Distance") {

    it("Should correctly compute resolution") {
      val resolution = IterativeCostDistance.computeResolution(rdd1)
      math.floor(resolution/1000) should be (111) // ≈ 111 kilometers per degree
    }

    it("Should correctly project input points") {
      val costs = IterativeCostDistance(rdd1, List(Point(2.5, 2.5)))
      val cost = costs.first._2
      cost.getDouble(2,2) should be (0.0)
    }

    it("Should propogate left") {
      val costs = IterativeCostDistance(rdd2, List(Point(2.5+5.0, 2.5)))
      val right = costs.filter({ case (k, _) => k == SpatialKey(1, 0) }).first._2
      val left = costs.filter({ case (k, _) => k == SpatialKey(0, 0) }).first._2
      val resolution = IterativeCostDistance.computeResolution(rdd2)
      val hops = (right.getDouble(3,2) - left.getDouble(3,2)) / resolution

      hops should be (5.0)
    }

    it("Should propogate right") {
      val costs = IterativeCostDistance(rdd2, List(Point(2.5, 2.5)))
      val right = costs.filter({ case (k, _) => k == SpatialKey(1, 0) }).first._2
      val left = costs.filter({ case (k, _) => k == SpatialKey(0, 0) }).first._2
      val resolution = IterativeCostDistance.computeResolution(rdd2)
      val hops = (right.getDouble(1,2) - left.getDouble(1,2)) / resolution

      hops should be (5.0)
    }

    it("Should propogate up") {
      val costs = IterativeCostDistance(rdd3, List(Point(2.5, 2.5)))
      val up = costs.filter({ case (k, _) => k == SpatialKey(0, 1) }).first._2
      val down = costs.filter({ case (k, _) => k == SpatialKey(0, 0) }).first._2
      val resolution = IterativeCostDistance.computeResolution(rdd3)
      val hops = (up.getDouble(2,3) - down.getDouble(2,3)) / resolution

      hops should be (5.0)
    }

    it("Should propogate down") {
      val costs = IterativeCostDistance(rdd3, List(Point(2.5, 2.5+5.0)))
      val up = costs.filter({ case (k, _) => k == SpatialKey(0, 1) }).first._2
      val down = costs.filter({ case (k, _) => k == SpatialKey(0, 0) }).first._2
      val resolution = IterativeCostDistance.computeResolution(rdd3)
      val hops = (up.getDouble(2,1) - down.getDouble(2,1)) / resolution

      hops should be (5.0)
    }

  }
}
