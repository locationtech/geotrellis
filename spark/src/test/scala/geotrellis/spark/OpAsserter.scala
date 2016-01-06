package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff._

import java.io.File

import org.apache.spark._

import org.scalatest._
import spire.syntax.cfor._

trait OpAsserter extends FunSpec
    with RasterRDDBuilders
    with TestEnvironment
    with RasterMatchers  {

  def testGeoTiff(sc: SparkContext,
    path: String,
    layoutCols: Int = 4,
    layoutRows: Int = 3
  )(
    rasterOp: (Tile, RasterExtent) => Tile,
    sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
    asserter: (Tile, Tile) => Unit = tilesEqual
  ) = {
    val tile = SingleBandGeoTiff(new File(inputHomeLocalPath, path).getPath).tile
    testTile(sc, tile, layoutCols, layoutRows)(rasterOp, sparkOp, asserter)
  }

  def testTile(sc: SparkContext,
    input: Tile,
    layoutCols: Int = 4,
    layoutRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val (tile, rasterRDD) = 
      createRasterRDD(
        input,
        layoutCols,
        layoutRows
      )(sc)

    val rasterResult = rasterOp(tile, rasterRDD.metaData.layout.toRasterExtent)
    val sparkResult = sparkOp(rasterRDD).stitch.tile

    asserter(rasterResult, sparkResult)
  }
}
