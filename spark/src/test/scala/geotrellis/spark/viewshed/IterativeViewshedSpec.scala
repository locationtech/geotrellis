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

package geotrellis.spark.viewshed

import geotrellis.layers.TileLayerMetadata
import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.raster.viewshed.R2Viewshed._
import geotrellis.tiling._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.vector._

import scala.collection.mutable
import org.locationtech.jts.{geom => jts}
import org.scalatest._


class IterativeViewshedSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  describe("Iterative Viewshed") {
    val ninf = Double.NegativeInfinity

    it("should assert all pixels on a flat plane") {
      val rdd = {
        val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
        val extent = Extent(0, 0, 15, 15)
        val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 15×15 pixels
        val layoutDefinition = LayoutDefinition(gridExtent, 5)
        val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
        val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
        val list = for (col <- 0 to 2; row <- 0 to 2) yield (SpatialKey(col, row), tile)
        ContextRDD(sc.parallelize(list), tileLayerMetadata)
     }
      val point = Viewpoint(7, 7, -0.0, 0, -1.0, ninf)
      val viewshed = rdd.viewshed(
        points = List(point),
        maxDistance = Double.PositiveInfinity,
        curvature = false,
        operator = Or
      )
      var actual = 0 ; viewshed.collect.foreach({ case (_, v) => v.foreach({ z => if (isData(z)) actual += z }) })
      val expected = 15*15

      actual should be (expected)
    }

    it("should compute shadows") {
      val rdd = {
        val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5); tile.set(2, 2, 42)
        val extent = Extent(0, 0, 15, 15)
        val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 15×15 pixels
        val layoutDefinition = LayoutDefinition(gridExtent, 5)
        val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
        val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
        val list = for (col <- 0 to 2; row <- 0 to 2) yield (SpatialKey(col, row), tile)
        ContextRDD(sc.parallelize(list), tileLayerMetadata)
      }

      val point = Viewpoint(7, 7, -0.0, 0, -1.0, ninf)
      val viewshed = IterativeViewshed(rdd, List(point),
        maxDistance = Double.PositiveInfinity,
        curvature = false,
        operator = Or
      )
      var actual = 0 ; viewshed.collect.foreach({ case (_, v) => v.foreach({ z => if (isData(z)) actual += z }) })
      val expected = 171

      actual should be (expected)
    }

    it("should see tall items behind short items") {
      val rdd = {
        val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
        val specialTile = IntArrayTile(Array.fill[Int](25)(1), 5, 5); specialTile.set(2,0,3) ; specialTile.set(2,4,107)
        val extent = Extent(0, 0, 15, 15)
        val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 15×15 pixels
        val layoutDefinition = LayoutDefinition(gridExtent, 5)
        val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
        val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
        val list = for (col <- 0 to 2; row <- 0 to 2) yield (if (col == 1 && row == 2) (SpatialKey(col, row), specialTile); else (SpatialKey(col, row), tile))
        ContextRDD(sc.parallelize(list), tileLayerMetadata)
      }
      val point = Viewpoint(7, 7, -0.0, 0, -1.0, ninf)
      val viewshed = rdd.viewshed(
        points = List(point),
        maxDistance = Double.PositiveInfinity,
        curvature = false,
        operator = Or
      )
      val ND = NODATA
      val expected: Array[Int] = Array(
        1,     1,     1,     1,     1,
        1,     1,     ND,    1,     1,
        1,     1,     ND,    1,     1,
        1,     ND,    ND,    ND,    1,
        1,     ND,    1,     ND,    1
      )
      val actual = viewshed
        .collect
        .filter({ case (key, _) => key == SpatialKey(1,2) })
        .head._2.toArray

      actual should be (expected)
    }

    it("should work with multiple points") {
      val rdd = {
        val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
        val extent = Extent(0, 0, 15, 15)
        val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 15×15 pixels
        val layoutDefinition = LayoutDefinition(gridExtent, 5)
        val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
        val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
        val list = for (col <- 0 to 2; row <- 0 to 2) yield (SpatialKey(col, row), tile)
        ContextRDD(sc.parallelize(list), tileLayerMetadata)
      }
      val point1 = Viewpoint(2,  7, -0.0, 0, -1.0, ninf)
      val point2 = Viewpoint(7,  7, -0.0, 0, -1.0, ninf)
      val point3 = Viewpoint(12, 7, -0.0, 0, -1.0, ninf)
      val viewshed = rdd.viewshed(
        points = List(point1, point2, point3),
        maxDistance = Double.PositiveInfinity,
        curvature = false,
        operator = Or
      )
      val expected = 15 * 15 * 1
      var actual: Int = 0
      viewshed.collect.foreach({ case (k, v) => actual += v.toArray.sum })

      actual should be (expected)
    }

    it("should keep isolated points isolated") {
      val rdd = {
        val array: Array[Int] = Array(
          0,     107,   107,   107,   0,
          0,     107,   0,     107,   0,
          0,     107,   0,     107,   0,
          0,     107,   0,     107,   0,
          0,     107,   107,   107,   0
        )
        val tile = IntArrayTile(array, 5, 5)
        val extent = Extent(0, 0, 15, 15)
        val gridExtent = GridExtent[Long](extent, CellSize(1, 1)) // 15×15 pixels
        val layoutDefinition = LayoutDefinition(gridExtent, 5)
        val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
        val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
        val list = for (col <- 0 to 2; row <- 0 to 2) yield (SpatialKey(col, row), tile)
        ContextRDD(sc.parallelize(list), tileLayerMetadata)
      }

      val point1 = Viewpoint(2,  7, -0.0, 0, -1.0, ninf)
      val point2 = Viewpoint(7,  7, -0.0, 0, -1.0, ninf)
      val point3 = Viewpoint(12, 7, -0.0, 0, -1.0, ninf)
      val expected = 3
      val viewshed = rdd.viewshed(
        points = List(point1, point2, point3),
        maxDistance = Double.PositiveInfinity,
        curvature = false,
        operator = Or
      )
      var actual: Int = 0
      viewshed.collect.foreach({ case (k, v) => actual += v.get(2, 2) })

      actual should be (expected)
    }

  }
}
