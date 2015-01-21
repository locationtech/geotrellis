package geotrellis.spark

import geotrellis.raster._
import geotrellis.raster.io.arg.ArgReader
import geotrellis.raster.io.geotiff.reader.GeoTiffReader

import java.io.File

import org.apache.spark._

import org.scalatest._
import spire.syntax.cfor._

trait OpAsserter extends FunSpec
    with RasterRDDBuilders
    with TestEnvironment
    with RasterMatchers  {

  val basePath = localFS
    .getWorkingDirectory
    .toString
    .substring(5) + "/src/test/resources/"

  def testArg(sc: SparkContext,
    path: String,
    tileCols: Int = 4,
    tileRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val tile = ArgReader.read(basePath + path)
    testTile(sc, tile, tileCols, tileRows)(rasterOp, sparkOp, asserter)
  }

  def testGeoTiff(sc: SparkContext,
    path: String,
    tileCols: Int = 4,
    tileRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val tile = GeoTiffReader.read(basePath + path).firstBand.tile
    testTile(sc, tile, tileCols, tileRows)(rasterOp, sparkOp, asserter)
  }

  def testTile(sc: SparkContext,
    input: Tile,
    tileCols: Int = 4,
    tileRows: Int = 3)
    (
      rasterOp: (Tile, RasterExtent) => Tile,
      sparkOp: RasterRDD[SpatialKey] => RasterRDD[SpatialKey],
      asserter: (Tile, Tile) => Unit = tilesEqual
    ) = {
    val rasterRDD = createRasterRDDFromSingleTile(
      sc,
      input,
      tileCols,
      tileRows
    )

    val rasterResult = rasterOp(input, rasterRDD.metaData.rasterExtent)
    val sparkResult = sparkOp(rasterRDD).stitch

    asserter(rasterResult, sparkResult)
  }

  private def createRasterRDDFromSingleTile(sc: SparkContext,
    input: Tile,
    tileCols: Int,
    tileRows: Int) = {
    val (cols, rows) = (input.cols, input.rows)

    val tileLayout = if (tileCols >= cols || tileRows >= rows)
      TileLayout(1, 1, cols, rows)
    else getTileLayout(cols, rows, tileCols, tileRows)

    createRasterRDD(sc, input, tileLayout)
  }

  private lazy val Default = 256

  private def getTileLayout(cols: Int, rows: Int, tileCols: Int, tileRows: Int) = {
    val ((tc, tx), (tr, ty)) = (
      calculateDimension(cols, tileCols),
      calculateDimension(rows, tileRows)
    )

    (tc, tr) match {
      case ((Default, Default)) => TileLayout(tx, ty, tc, tr)
      case ((Default, r)) => {
        val (tr, ty) = default(rows)
        TileLayout(tx, ty, tc, tr)
      }
      case ((r, Default)) => {
        val (tc, tx) = default(cols)
        TileLayout(tx, ty, tc, tr)
      }
      case _ => TileLayout(tx, ty, tc, tr)
    }
  }

  private def calculateDimension(whole: Int, candidate: Int) =
    if (whole % candidate == 0) (candidate, whole / candidate)
    else Stream.range(candidate, whole - 1).dropWhile(whole % _ != 0).headOption match {
      case Some(c) => (c, whole / c)
      case None => default(whole)
    }

  private def default(whole: Int) = (Default, (whole + (Default - 1)) / Default)

}
