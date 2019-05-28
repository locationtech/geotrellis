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

package geotrellis.spark.testkit

import geotrellis.tiling.SpatialKey
import geotrellis.raster._
import geotrellis.raster.io.geotiff._
import geotrellis.layers._
import geotrellis.spark._

import org.apache.spark._

import java.io.File

trait OpAsserter { self: TestEnvironment =>

  def testGeoTiff(
    sc: SparkContext,
    path: String,
    layoutCols: Int = 4,
    layoutRows: Int = 3
  )(
    rasterOp: (Tile, RasterExtent) => Tile,
    sparkOp: TileLayerRDD[SpatialKey] => TileLayerRDD[SpatialKey],
    asserter: (Tile, Tile) => Unit = tilesEqual
  ) = {
    val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, path).getPath).tile.toArrayTile
    testTile(sc, tile, layoutCols, layoutRows)(rasterOp, sparkOp, asserter)
  }

  def testGeoTiffCollection(
    sc: SparkContext,
    path: String,
    layoutCols: Int = 4,
    layoutRows: Int = 3
   )(
     rasterOp: (Tile, RasterExtent) => Tile,
     sparkOp: TileLayerCollection[SpatialKey] => TileLayerCollection[SpatialKey],
     asserter: (Tile, Tile) => Unit = tilesEqual
   ) = {
    val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, path).getPath).tile.toArrayTile
    testTileCollection(sc, tile, layoutCols, layoutRows)(rasterOp, sparkOp, asserter)
  }

  def testTile(
    sc: SparkContext,
    input: Tile,
    layoutCols: Int = 4,
    layoutRows: Int = 3
  )(
    rasterOp: (Tile, RasterExtent) => Tile,
    sparkOp: TileLayerRDD[SpatialKey] => TileLayerRDD[SpatialKey],
    asserter: (Tile, Tile) => Unit = tilesEqual
  ) = {
    val (tile, rasterRDD) =
      createTileLayerRDD(
        input,
        layoutCols,
        layoutRows
      )(sc)

    val rasterResult = rasterOp(tile, rasterRDD.metadata.layout.toRasterExtent)
    val sparkResult = sparkOp(rasterRDD).stitch

    asserter(rasterResult, sparkResult.tile)
  }

  def testTileCollection(sc: SparkContext,
    input: Tile,
    layoutCols: Int = 4,
    layoutRows: Int = 3
  )(
    rasterOp: (Tile, RasterExtent) => Tile,
    sparkOp: TileLayerCollection[SpatialKey] => TileLayerCollection[SpatialKey],
    asserter: (Tile, Tile) => Unit = tilesEqual
   ) = {
    val (tile, rasterRDD) =
      createTileLayerRDD(
        input,
        layoutCols,
        layoutRows
      )(sc)

    val rasterCollection = rasterRDD.toCollection

    val rasterResult = rasterOp(tile, rasterCollection.metadata.layout.toRasterExtent)
    val sparkResult = sparkOp(rasterCollection).stitch

    asserter(rasterResult, sparkResult.tile)
  }
}
