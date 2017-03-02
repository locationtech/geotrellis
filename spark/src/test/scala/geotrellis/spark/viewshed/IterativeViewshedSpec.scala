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

import geotrellis.proj4.LatLng
import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.testkit.TestEnvironment
import geotrellis.spark.tiling.LayoutDefinition
import geotrellis.vector._

import org.scalatest._


class IterativeViewshedSpec extends FunSpec
    with Matchers
    with TestEnvironment {

  val rdd = {
    val tile = IntArrayTile(Array.fill[Int](25)(1), 5, 5)
    val extent = Extent(0, 0, 15, 15)
    val gridExtent = GridExtent(extent, 1, 1) // 15×15 pixels
    val layoutDefinition = LayoutDefinition(gridExtent, 5)
    val bounds = Bounds(SpatialKey(0, 0), SpatialKey(2, 2))
    val tileLayerMetadata = TileLayerMetadata(IntCellType, layoutDefinition, extent, LatLng, bounds)
    val list = for (col <- 0 to 2; row <- 0 to 2) yield (SpatialKey(col, row), tile)
    ContextRDD(sc.parallelize(list), tileLayerMetadata)
  }

  describe("Iterative Viewshed") {
    IterativeViewshed(rdd, Point(7, 7))
  }
}
