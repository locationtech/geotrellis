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
    with Matchers {

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
    val tile = GeoTiffReader(basePath + path).read.imageDirectories.head.toRaster._1
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
      TileLayout(cols, rows, cols, rows)
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

  val Eps = 1e-3

  def tilesEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb, Eps)

  def tilesEqual(ta: Tile, tb: Tile, eps: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)

    (cols, rows) should be((tb.cols, tb.rows))

    cfor(0)(_ < cols, _ + 1) { i =>
      cfor(0)(_ < rows, _ + 1) { j =>
        val v1 = ta.getDouble(i, j)
        val v2 = tb.getDouble(i, j)
        if (v1.isNaN) v2.isNaN should be (true)
        else if (v2.isNaN) v1.isNaN should be (true)
        else v1 should be (v2 +- eps)
      }
    }
  }

  def arraysEqual(a1: Array[Double], a2: Array[Double], eps: Double = Eps) =
    a1.zipWithIndex.foreach { case (v, i) => v should be (a2(i) +- eps) }
}
