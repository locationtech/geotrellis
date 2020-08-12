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

package geotrellis.raster.testkit

import geotrellis.proj4.CRS
import geotrellis.raster._
import geotrellis.raster.io.geotiff.GeoTiff
import geotrellis.raster.render.ascii.AsciiArtEncoder
import geotrellis.raster.render.png.{PngColorEncoding, RgbaPngEncoding}
import spire.implicits.cfor
import spire.math.Integral
import org.scalatest.matchers.{HavePropertyMatchResult, HavePropertyMatcher}
import org.scalatest.matchers.should.Matchers
import org.scalatest.tools.BetterPrinters

import scala.reflect.ClassTag
import java.nio.file.{Files, Paths}

import geotrellis.vector.Extent

import scala.util.Random

trait RasterMatchers extends Matchers {
  import RasterMatchers._

  val Eps = 1e-3

  def assertEqual(r: Tile, arr: Array[Int]): Unit = {
    withClue(s"Sizes do not match.") {
      (r.cols * r.rows) should be (arr.length)
    }

    r.foreach { (col, row, z) =>
      withClue(s"Value at ($col, $row) are not the same") {
        z should be (arr(row * r.cols + col))
      }
    }
  }

  def assertEqual(r: Tile, arr: Array[Double], threshold: Double = Eps): Unit = {
    withClue(s"Sizes do not match.") {
      (r.cols * r.rows) should be (arr.length)
    }

    r.foreachDouble { (col, row, v1) =>
      val v2 = arr(row * r.cols + col)
      if (isNoData(v1)) {
        withClue(s"Value at ($col, $row) are not the same: v1 = NoData, v2 = $v2") {
          isNoData(v2) should be(true)
        }
      } else {
        if (isNoData(v2)) {
          withClue(s"Value at ($col, $row) are not the same: v1 = $v1, v2 = NoData") {
            isNoData(v1) should be(true)
          }
        } else {
          withClue(s"Value at ($col, $row) are not the same: ") {
            v1 should be(v2 +- threshold)
          }
        }
      }
    }
  }

  def assertEqual(r1: Raster[Tile], r2: Raster[Tile])(implicit di: DummyImplicit): Unit = {
    assertEqual(r1.tile, r2.tile)
    assert(r1.extent == r2.extent, s"${r1.extent} != ${r2.extent}")
  }

  def assertEqual(r1: Raster[MultibandTile], r2: Raster[MultibandTile]): Unit = {
    assertEqual(r1.tile, r2.tile)
    assert(r1.extent == r2.extent, s"${r1.extent} != ${r2.extent}")
  }

  def assertEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb)

  def assertEqual(ta: Tile, tb: Tile, threshold: Double): Unit = tilesEqual(ta, tb, threshold)

  def arraysEqual(a1: Array[Double], a2: Array[Double], eps: Double = Eps) =
    a1.zipWithIndex.foreach { case (v, i) => v should be (a2(i) +- eps) }

  def tilesEqual(ta: Tile, tb: Tile): Unit = tilesEqual(ta, tb, Eps)

  def tilesEqual(ta: Tile, tb: Tile, eps: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)

    (cols, rows) should be((tb.cols, tb.rows))

    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v1 = ta.getDouble(col, row)
        val v2 = tb.getDouble(col, row)
        withClue(s"Wasn't equal on col: $col, row: $row (v1=$v1, v2=$v2)") {
          if (v1.isNaN) v2.isNaN should be (true)
          else if (v2.isNaN) v1.isNaN should be (true)
          else v1 should be (v2 +- eps)
        }
      }
    }
  }

  def assertEqual(ta: MultibandTile, tb: MultibandTile): Unit = assertEqual(ta, tb, Eps)

  def assertEqual(ta: MultibandTile, tb: MultibandTile, threshold: Double): Unit = {
    val (cols, rows) = (ta.cols, ta.rows)
    val (bands1, bands2) = (ta.bandCount, tb.bandCount)

    (cols, rows) should be((tb.cols, tb.rows))
    bands1 should be (bands2)

    cfor(0)(_ < bands1, _ + 1) { b =>
      val tab = ta.band(b)
      val tbb = tb.band(b)
      cfor(0)(_ < cols, _ + 1) { col =>
        cfor(0)(_ < rows, _ + 1) { row =>
          val v1 = tab.getDouble(col, row)
          val v2 = tbb.getDouble(col, row)
          withClue(s"BAND $b wasn't equal on col: $col, row: $row (v1=$v1, v2=$v2)") {
            if (v1.isNaN) v2.isNaN should be (true)
            else if (v2.isNaN) v1.isNaN should be (true)
            else v1 should be (v2 +- threshold)
          }
        }
      }
    }
  }

  /*
   * Takes a function and checks if each f(x, y) == tile.get(x, y)
   *  - Specialized for int so the function can check if an
   *    (x, y) pair are NODATA. Prior to this, the tile's value
   *    would be converted to a double, and NODATA would become NaN.
   */
  def rasterShouldBeInt(tile: Tile, f: (Int, Int) => Int): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(col, row)
        val v = tile.get(col, row)
        withClue(s"(col=$col, row=$row)") { v should be(exp) }
      }
    }
  }

  /*
   * Takes a value and a count and checks
   * a. if every pixel == value, and
   * b. if number of tiles == count
   */
  def rasterShouldBe(tile: Tile, value: Int): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        withClue(s"(col=$col, row=$row)") { tile.get(col, row) should be(value) }
      }
    }
  }

  def rasterShouldBe(tile: Tile, f: (Tile, Int, Int) => Double): Unit =
    rasterShouldBeAbout(tile, f, 1e-100)

  def rasterShouldBeAbout(tile: Tile, f: (Tile, Int, Int) => Double, epsilon: Double): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(tile, col, row)
        val v = tile.getDouble(col, row)
        if (!exp.isNaN || !v.isNaN) {
          withClue(s"(col=$col, row=$row)") { v should be(exp +- epsilon) }
        }
      }
    }
  }

  def rasterShouldBe(tile: Tile, f: (Int, Int) => Double): Unit =
    rasterShouldBeAbout(tile, f, 1e-100)

  def rasterShouldBeAbout(tile: Tile, f: (Int, Int) => Double, epsilon: Double): Unit = {
    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val exp = f(col, row)
        val v = tile.getDouble(col, row)
        if (!exp.isNaN || !v.isNaN) {
          withClue(s"(col=$col, row=$row)") { v should be(exp +- epsilon) }
        }
      }
    }
  }

  private def dimsToString[T <: Grid[N], N: Integral](t: T): String =
    s"""(${t.cols}, ${t.rows})"""


  // def dimensions(dims: (Int, Int)) = HavePropertyMatcher[CellGrid[Int], (Int, Int)] { grid =>
  //   HavePropertyMatchResult(grid.dimensions == dims, "dimensions", dims, grid.dimensions)
  // }

  def cellType[T<: CellGrid[_]: ClassTag] (ct: CellType) = HavePropertyMatcher[T, CellType] { grid =>
    HavePropertyMatchResult(grid.cellType == ct, "cellType", ct, grid.cellType)
  }

  def bandCount(count: Int) = HavePropertyMatcher[MultibandTile, Int] { tile =>
    HavePropertyMatchResult(tile.bandCount == count, "bandCount", count, tile.bandCount)
  }

  def assertTilesEqual(actual: Tile, expected: Tile): Unit =
    assertTilesEqual(actual, expected, Eps)

  def assertTilesEqual(actual: Tile, expected: Tile, threshold: Double): Unit =
    assertTilesEqual(MultibandTile(actual), MultibandTile(expected), threshold: Double)

  def assertTilesEqual(actual: MultibandTile, expected: MultibandTile): Unit =
    assertTilesEqual(actual, expected, Eps)

  def assertTilesEqual(actual: MultibandTile, expected: MultibandTile, threshold: Double): Unit = {
    actual should have (
      cellType (expected.cellType),
      // dimensions (expected.dimensions),
      bandCount (expected.bandCount)
    )

    withAsciiDiffClue(actual, expected){
      assertEqual(actual, expected, threshold)
    }
  }

  def assertRastersEqual(actual: Raster[Tile], expected: Raster[MultibandTile])(implicit d: DummyImplicit): Unit =
    assertRastersEqual(actual.mapTile(MultibandTile(_)), expected)

  def assertRastersEqual(actual: Raster[Tile], expected: Raster[MultibandTile], threshold: Double)(implicit d: DummyImplicit): Unit =
    assertRastersEqual(actual.mapTile(MultibandTile(_)), expected, threshold)

  def assertRastersEqual(actual: Raster[Tile], expected: Raster[MultibandTile], threshold: Double, thresholdExtent: Double)(implicit d: DummyImplicit): Unit =
    assertRastersEqual(actual.mapTile(MultibandTile(_)), expected, threshold, thresholdExtent)

  def assertRastersEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile]): Unit =
    assertRastersEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile], Eps)

  def assertRastersEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile], threshold: Double): Unit =
    assertRastersEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile], threshold: Double, Eps)

  def assertRastersEqual(actual: Raster[MultibandTile], expected: Raster[MultibandTile], threshold: Double, thresholdExtent: Double): Unit = {
    actual.extent.xmin shouldBe expected.extent.xmin +- thresholdExtent
    actual.extent.ymin shouldBe expected.extent.ymin +- thresholdExtent
    actual.extent.xmax shouldBe expected.extent.xmax +- thresholdExtent
    actual.extent.ymax shouldBe expected.extent.ymax +- thresholdExtent

    actual.tile should have (
      cellType (expected.cellType),
      // dimensions (expected.dimensions),
      bandCount (expected.tile.bandCount)
    )

    withAsciiDiffClue(actual.tile, expected.tile){
      assertEqual(actual.tile, expected.tile, threshold)
    }
  }

  /** Renders scaled diff tiles as a clue */
  def withAsciiDiffClue[T](
    actual: MultibandTile,
    expect: MultibandTile,
    mode: DiffMode = DiffMode.DiffSum,
    eps: Double = 0,
    palette: AsciiArtEncoder.Palette = AsciiArtEncoder.Palette(" ░▒▓█"),
    size: Int = 24
  )(fun: => T) = withClue({
    require(actual.bandCount == expect.bandCount, s"Band count doesn't match: ${actual.bandCount} != ${expect.bandCount}")
    val diffs = for (b <- 0 until actual.bandCount) yield
      scaledDiff(actual.band(b), expect.band(b), mode = mode, maxDim = size, eps = eps)

    val asciiDiffs = diffs.map(_.renderAscii(palette))

    val joinedDiffs: String = asciiDiffs
      .map(_.lines.toSeq)
      .transpose
      .map(_.mkString("\t"))
      .mkString("\n")

    val bandList = (0 until actual.bandCount).mkString(",")
    val scale = s"1 char == ${actual.rows / diffs(0).rows} rows == ${actual.cols / diffs(0).cols} cols"
    s"""
       |+ Diff: band(${bandList}) @ ($scale)
       |${joinedDiffs}
       |
    """.stripMargin
  })(fun)

  def withGeoTiffClue[T](
    actual: Raster[MultibandTile],
    expect: Raster[MultibandTile],
    crs: CRS
  )(fun: => T): T = withClue({
    val tmpDir = Files.createTempDirectory(getClass.getSimpleName)
    val actualFile = tmpDir.resolve("actual.tiff")
    val expectFile = tmpDir.resolve("expect.tiff")
    var diffFile = tmpDir.resolve("diff.tiff")
    GeoTiff(actual, crs).write(actualFile.toString, optimizedOrder = true)
    GeoTiff(expect, crs).write(expectFile.toString, optimizedOrder = true)

    if ((actual.tile.bandCount == expect.tile.bandCount) && (actual.dimensions == expect.dimensions)) {
      val diff = actual.tile.bands.zip(expect.tile.bands).map { case (l, r) => l - r }.toArray
      GeoTiff(ArrayMultibandTile(diff), actual.extent, crs).write(diffFile.toString, optimizedOrder = true)
    } else {
      diffFile = null
    }

    s"""
       |+ actual: ${actualFile}
       |+ expect: ${expectFile}
       |+ diff  : ${Option(diffFile).getOrElse("--")}
    """stripMargin
  })(fun)

  def writePngOutputTile(
    tile: MultibandTile,
    colorEncoding: PngColorEncoding = RgbaPngEncoding,
    band: Int = 0,
    name: String = "output",
    discriminator: String = "",
    outputDir: Option[String] = None
  ): MultibandTile = {
    val tmpDir = outputDir.fold(Files.createTempDirectory(getClass.getSimpleName))(Paths.get(_))
    val outputFile = tmpDir.resolve(s"${name}${discriminator}.png")
    tile.band(band).renderPng().write(outputFile.toString)

    val msg = s"""
                 |+ png output path  : ${outputFile}
    """stripMargin

    BetterPrinters.printAnsiGreen(msg)
    tile
  }

  def writePngOutputRaster(
    raster: Raster[MultibandTile],
    colorEncoding: PngColorEncoding = RgbaPngEncoding,
    band: Int = 0,
    name: String = "output",
    discriminator: String = "",
    outputDir: Option[String] = None
  ): Raster[MultibandTile] =
    raster.mapTile(writePngOutputTile(_, colorEncoding, band, name, discriminator, outputDir))

  def randomExtentWithin(extent: Extent, sampleScale: Double = 0.10): Extent = {
    assert(sampleScale > 0 && sampleScale <= 1)
    val extentWidth = extent.xmax - extent.xmin
    val extentHeight = extent.ymax - extent.ymin

    val sampleWidth = extentWidth * sampleScale
    val sampleHeight = extentHeight * sampleScale

    val testRandom = Random.nextDouble()
    val subsetXMin = (testRandom * (extentWidth - sampleWidth)) + extent.xmin
    val subsetYMin = (Random.nextDouble() * (extentHeight - sampleHeight)) + extent.ymin

    Extent(subsetXMin, subsetYMin, subsetXMin + sampleWidth, subsetYMin + sampleHeight)
  }
}


sealed trait DiffMode {
  def apply(acc: Double, next: Double): Double
}

object DiffMode {
  case object DiffCount extends DiffMode {
    def apply(acc: Double, next: Double) = if (isNoData(acc)) 1 else acc + 1
  }
  case object DiffSum extends DiffMode {
    def apply(acc: Double, next: Double) = if (isNoData(acc)) next else acc + next
  }
  case object DiffMax extends DiffMode{
    def apply(acc: Double, next: Double) = if (isNoData(acc)) next else math.max(acc, next)
  }
  case object DiffMin extends DiffMode{
    def apply(acc: Double, next: Double) = if (isNoData(acc)) next else math.min(acc, next)
  }
}

object RasterMatchers {
  def scaledDiff(actual: Tile, expect: Tile, maxDim: Int, mode: DiffMode = DiffMode.DiffSum, eps: Double = 0): Tile = {
    require(actual.dimensions == expect.dimensions,
      s"dimensions mismatch: ${actual.dimensions}, ${expect.dimensions}")

    val cols = actual.cols
    val rows = actual.rows
    val scale: Double = maxDim / math.max(cols, rows).toDouble
    val diff = ArrayTile.empty(FloatConstantNoDataCellType, (cols * scale).toInt, (rows * scale).toInt)
    val colScale: Double = diff.cols.toDouble / actual.cols.toDouble
    val rowScale: Double = diff.rows.toDouble / actual.rows.toDouble
    var diffs = 0
    cfor(0)(_ < cols, _ + 1) { col =>
      cfor(0)(_ < rows, _ + 1) { row =>
        val v1 = actual.getDouble(col, row)
        val v2 = expect.getDouble(col, row)
        val vd: Double =
          if (isNoData(v1) && isNoData(v2)) Double.NaN
          else if (isData(v1) && isNoData(v2)) math.abs(v1)
          else if (isNoData(v1) && isData(v2)) math.abs(v2)
          else math.abs(v1 - v2)

        if (isData(vd) && (vd > eps)) {
          val dcol = (colScale * col).toInt
          val drow = (rowScale * row).toInt
          val ac = diff.getDouble(dcol, drow)
          if (isData(ac)) {
            diff.setDouble(dcol, drow, ac + vd)
          } else
            diff.setDouble(dcol, drow, vd)
          diffs += 1
        }
      }
    }
    diff
  }
}
