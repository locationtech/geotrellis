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

import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.raster.mapalgebra.focal.ZFactor
import geotrellis.layer._
import geotrellis.spark._
import geotrellis.spark.testkit._

import org.scalatest.funspec.AnyFunSpec
import java.io._

class SlopeSpec extends AnyFunSpec with TestEnvironment {

  describe("Slope Elevation Spec") {
    val calculator = ZFactor({ _ => 1.0 })

    it("should match gdal computed slope raster") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize, calculator.fromExtent(re.extent))
      val sparkOp = (rdd: TileLayerRDD[SpatialKey]) => rdd.slope(calculator)

      val path = "aspect.tif"

      testGeoTiff(sc, path)(rasterOp, sparkOp)
    }

    it("should update RDD cellType of DoubleConstantNoDataCellType") {
      val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, "aspect.tif").getPath).tile.toArrayTile()

      val (_, rasterRDD) = createTileLayerRDD(tile, 4, 3)
      val slopeRDD = rasterRDD.slope(calculator)
      slopeRDD.metadata.cellType should be (DoubleConstantNoDataCellType)
      slopeRDD.collect().head._2.cellType should be (DoubleConstantNoDataCellType)
    }

    it("should match gdal computed slope raster (collections api)") {
      val rasterOp = (tile: Tile, re: RasterExtent) => tile.slope(re.cellSize, calculator.fromExtent(re.extent))
      val sparkOp = (collection: TileLayerCollection[SpatialKey]) => collection.slope(calculator)

      val path = "aspect.tif"

      testGeoTiffCollection(sc, path)(rasterOp, sparkOp)
    }

  }
}
