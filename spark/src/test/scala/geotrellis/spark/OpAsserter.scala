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

  val basePath = {
    val workingDir = 
    localFS
      .getWorkingDirectory
      .toString
      .substring(5)
    s"${workingDir}/src/test/resources/"
  }

  def testArg(sc: SparkContext,
    path: String,
    layoutCols: Int = 4,
    layoutRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val tile = ArgReader.read(basePath + path)
    testTile(sc, tile, layoutCols, layoutRows)(rasterOp, sparkOp, asserter)
  }

  def testGeoTiff(sc: SparkContext,
    path: String,
    layoutCols: Int = 4,
    layoutRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val tile = SingleBandGeoTiff(basePath + path).tile
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
    val (rasterRDD, tile) = 
      createInputs(
        sc,
        input,
        layoutCols,
        layoutRows
      )

    val rasterResult = rasterOp(tile, rasterRDD.metaData.layout.rasterExtent)
    val sparkResult = sparkOp(rasterRDD).stitch.tile

    asserter(rasterResult, sparkResult)
  }

  private def createInputs(
    sc: SparkContext,
    input: Tile,
    layoutCols: Int,
    layoutRows: Int): (RasterRDD[SpatialKey], Tile) = {
    val (cols, rows) = (input.cols, input.rows)

    val tileLayout = 
      if (layoutCols >= cols || layoutRows >= rows)
        sys.error(s"Invalid for tile of dimensions ${(cols, rows)}: ${(layoutCols, layoutRows)}")
      else 
        TileLayout(layoutCols, layoutRows, cols / layoutCols, rows / layoutRows)

    val tile = 
      if(tileLayout.totalCols.toInt != cols || tileLayout.totalRows.toInt != rows)
        input.crop(tileLayout.totalCols.toInt, tileLayout.totalRows.toInt)
      else
        input

    (createRasterRDD(sc, input, tileLayout), tile)
  }
}
