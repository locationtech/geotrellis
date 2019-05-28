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

package geotrellis.spark.mapalgebra.focal

import geotrellis.tiling._
import geotrellis.raster._
import geotrellis.layers.TileLayerCollection
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest.FunSpec

class AspectSpec extends FunSpec with TestEnvironment {

  describe("Aspect Elevation Spec") {

    it("should match gdal computed slope raster") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.aspect(re.cellSize)
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.aspect()

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

    it("should match gdal computed slope raster (collections api)") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.aspect(re.cellSize)
      val sparkOp = (collection: TileLayerCollection[SpatialKey]) => collection.aspect()

      val path = "aspect.tif"

      testGeoTiffCollection(sc, path)(rasterOp, sparkOp)
    }

  }
}
