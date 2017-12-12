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

package geotrellis.spark.mapalgebra.focal.tobler

import geotrellis.raster._
import geotrellis.raster.mapalgebra.focal.tobler._

import geotrellis.spark._
import geotrellis.spark.testkit._

import geotrellis.vector.Extent

import org.scalatest._
import spire.syntax.cfor._

class ToblerSpec extends FunSpec with TestEnvironment {

  describe("Tobler walking speed") {

    it("should get the same Tobler result for spark op as single raster op") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.tobler(re.cellSize)
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.tobler()

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

    it("should get the same Tobler result for spark op as single raster op (collection api)") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.tobler(re.cellSize)
      val sparkOp = (collection: TileLayerCollection[SpatialKey]) => collection.tobler()

      val path = "aspect.tif"

      testGeoTiffCollection(sc, path)(rasterOp, sparkOp)
    }
  }
}
