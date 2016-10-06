package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.stitch._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff._

import java.io.File

import geotrellis.spark.testkit._

import org.apache.spark._
import org.scalatest._
import spire.syntax.cfor._

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
    val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, path).getPath).tile
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
    val tile = SinglebandGeoTiff(new File(inputHomeLocalPath, path).getPath).tile
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

    asserter(rasterResult, sparkResult)
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

    asserter(rasterResult, sparkResult)
  }
}
