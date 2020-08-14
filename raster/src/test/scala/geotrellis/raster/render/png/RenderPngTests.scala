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

package geotrellis.raster.render.png

import geotrellis.raster._
import geotrellis.raster.geotiff.GeoTiffRasterSource
import geotrellis.raster.render.{ColorMap, ColorRamp, Png, RGB, RGBA}
import geotrellis.raster.testkit._
import spire.syntax.cfor._

import java.io._
import javax.imageio._

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class RenderPngTests extends AnyFunSuite with Matchers with TileBuilders with RasterMatchers {
  def testPng(png: Png, tile: Tile, colorMap: ColorMap): Unit = {
    val img = ImageIO.read(new ByteArrayInputStream(png.bytes))

    img.getWidth should be (tile.cols)
    img.getHeight should be (tile.rows)

    cfor(0)(_ < img.getWidth, _ + 1) { col =>
      cfor(0)(_ < img.getHeight, _ + 1) { row =>
        val argb = img.getRGB(col, row)
        val actual = (argb << 8) | ((argb >> 24) & 0xFF)
        val expected = colorMap.map(tile.get(col, row))

        withClue(f"$actual%02X does not equal $expected%02X") {
          actual should be (expected)
        }
      }
    }
  }

  test("should render a PNG and match what is read in by ImageIO when written as Indexed") {
    val tileNW =
      createValueTile(50, 1)
    val tileNE =
      createValueTile(50, 2)
    val tileSW =
      createValueTile(50, 3)
    val tileSE =
      createValueTile(50, 4)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val colorMap =
      ColorMap(
        Map(
          1 -> RGBA(255, 0, 0, 255),
          2 -> RGBA(0, 255, 0, 255),
          3 -> RGBA(0, 0, 255, 255),
          4 -> RGBA(0, 255, 255, 0xBB)
        )
      )

    val png = tile.renderPng(colorMap)

    testPng(png, tile, colorMap)
  }

  test("should render a PNG from an Int tile and match what is read in by ImageIO when written as Indexed with nodata values") {
    val tileNW =
      createValueTile(50, 1)
    val tileNE =
      createValueTile(50, 2)
    val tileSW =
      createValueTile(50, 3)
    val tileSE =
      createValueTile(50, NODATA)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val colorMap =
      ColorMap(
        Map(
          1 -> RGBA(255, 0, 0, 255),
          2 -> RGBA(0, 255, 0, 255),
          3 -> RGBA(0, 0, 255, 255),
          4 -> RGBA(0, 255, 255, 0xBB)
        )
      ).withNoDataColor(0xFFFFFFAA)

    val png = tile.renderPng(colorMap)

    testPng(png, tile, colorMap)
  }

  test("should render a PNG from a Double tile and match what is read in by ImageIO when written as Indexed with nodata values") {
    val tileNW =
      createValueTile(50, 1)
    val tileNE =
      createValueTile(50, 2)
    val tileSW =
      createValueTile(50, 3)
    val tileSE =
      createValueTile(50, NODATA)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50))
        .convert(DoubleConstantNoDataCellType)
        .toArrayTile

    val colorMap =
      ColorMap(
        Map(
          1.0 -> RGBA(255, 0, 0, 255),
          2.0 -> RGBA(0, 255, 0, 255),
          3.0 -> RGBA(0, 0, 255, 255),
          4.0 -> RGBA(0, 255, 255, 0xBB)
        )
      ).withNoDataColor(0xFFFFFFAA)

    val png = tile.renderPng(colorMap)

    testPng(png, tile, colorMap)
  }

  test("render int and double tiles similarly") {
    val tileNW =
      createValueTile(50, 1)
    val tileNE =
      createValueTile(50, 2)
    val tileSW =
      createValueTile(50, 3)
    val tileSE =
      createValueTile(50, 4)

    val intTile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val doubleTile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50))
        .convert(DoubleConstantNoDataCellType)
        .toArrayTile


    val intColorMap =
      ColorMap(
        Map(
          1 -> RGB(255, 0, 0),
          2 -> RGB(0, 255, 0),
          3 -> RGB(0, 0, 255),
          4 -> RGB(0, 255, 255)
        )
      )

    val doubleColorMap =
      ColorMap(
        Map(
          1.0 -> RGB(255, 0, 0),
          2.0 -> RGB(0, 255, 0),
          3.0 -> RGB(0, 0, 255),
          4.0 -> RGB(0, 255, 255)
        )
    )

    val intPng = intTile.renderPng(intColorMap)
    val doublePng = doubleTile.renderPng(doubleColorMap)

    val intImg = ImageIO.read(new ByteArrayInputStream(intPng.bytes))
    val doubleImg = ImageIO.read(new ByteArrayInputStream(doublePng.bytes))

    cfor(0)(_ < intImg.getWidth, _ + 1) { col =>
      cfor(0)(_ < intImg.getHeight, _ + 1) { row =>
        intImg.getRGB(col, row) should be (doubleImg.getRGB(col, row))
      }
    }
  }

  test("should render a PNG and match what is read in by ImageIO when written as RGBA") {
    val tileNW =
      createConsecutiveTile(50, 50, 1)
    val tileNE =
      createConsecutiveTile(50, 50, 2501)
    val tileSW =
      createConsecutiveTile(50, 50, 5001)
    val tileSE =
      createConsecutiveTile(50, 50, 7501)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val colorMap =
      ColorRamp(0xFF0000FF, 0x0000FFFF)
        .stops(1000)
        .setAlphaGradient(0xFF, 0xAA)
        .toColorMap(tile.histogram)

    val png = tile.renderPng(colorMap)

    testPng(png, tile, colorMap)
  }

  test("should render a PNG and match what is read in by ImageIO when written as RGBA with float tile") {
    val tileNW =
      createConsecutiveTile(50, 50, 1).convert(FloatConstantNoDataCellType)
    val tileNE =
      createConsecutiveTile(50, 50, 2501).convert(FloatConstantNoDataCellType)
    val tileSW =
      createConsecutiveTile(50, 50, 5001).convert(FloatConstantNoDataCellType)
    val tileSE =
      createConsecutiveTile(50, 50, 7501).convert(FloatConstantNoDataCellType)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val colorMap =
      ColorRamp(0xFF0000FF, 0x0000FFFF)
        .stops(1000)
        .setAlphaGradient(0xFF, 0xAA)
        .toColorMap(tile.histogram)

    val colored = colorMap.render(tile)
    colored.combineDouble(tile) { (z1, z2) =>
      val expected = colorMap.mapDouble(z2)
      withClue(f"${z1.toInt}%02X does not equal ${expected.toInt}%02X (${expected.toFloat.toInt}%02X) -") {
        z1 should be (expected)
      }
      1.0
    }
  }

  test("should render a PNG and match what is read in by ImageIO when written as RGBA with nodata values") {
    val tileNW =
      createConsecutiveTile(50, 50, 1)
    val tileNE =
      createConsecutiveTile(50, 50, 2501)
    val tileSW =
      createConsecutiveTile(50, 50, 5001)
    val tileSE =
      createValueTile(50, NODATA)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50)).toArrayTile

    val colorMap =
      ColorRamp(0xFF0000FF, 0x0000FFFF)
        .stops(1000)
        .setAlphaGradient(0xFF, 0xAA)
        .toColorMap(tile.histogram)
        .withNoDataColor(0xFFFFFFAA)

    val png = tile.renderPng(colorMap)

    testPng(png, tile, colorMap)
  }

  test("png encoding produces the same colors for indexed and RGBA") {
    val tile: IntArrayTile = IntArrayTile(1 to 256*256 toArray, 256, 256)
    val ramp = ColorRamp(0xff0000ff, 0x0000ffff)  // red to blue
    val stops = Array(10000, 20000, 30000, 40000, 50000, 60000, 70000)
    val colorMap = ColorMap(stops, ramp)

    val indexedPng = tile.renderPng(colorMap)
    val rgbaPng = colorMap.render(tile).renderPng()

    val indexedImg = ImageIO.read(new ByteArrayInputStream(indexedPng))
    val rgbaImg = ImageIO.read(new ByteArrayInputStream(rgbaPng))

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val actual = indexedImg.getRGB(col, row)
        val expected = rgbaImg.getRGB(col, row)
        withClue(f"$actual%02X does not equal $expected%02X") {
          actual should be (expected)
        }
      }
    }
  }

  test("png encoding produces the same colors for indexed and RGBA with a float tile") {
    val tile: FloatArrayTile = FloatArrayTile((1 to 256*256).map(_.toFloat).toArray, 256, 256)
    val ramp = ColorRamp(0xff0000ff, 0x0000ffff)  // red to blue
    val stops = Array(10000, 20000, 30000, 40000, 50000, 60000, 70000)
    val colorMap = ColorMap(stops, ramp)

    val indexedPng = tile.renderPng(colorMap)
    val rgbaPng = colorMap.render(tile).renderPng()

    val indexedImg = ImageIO.read(new ByteArrayInputStream(indexedPng))
    val rgbaImg = ImageIO.read(new ByteArrayInputStream(rgbaPng))

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val actual = indexedImg.getRGB(col, row)
        val expected = rgbaImg.getRGB(col, row)
        withClue(f"At $col, $row: $actual%02X does not equal $expected%02X - ") {
          actual should be (expected)
        }
      }
    }
  }

  test("png encoding produces the same colors for RGB and RGBA") {
    val tile: IntArrayTile = IntArrayTile(1 to 256*256 toArray, 256, 256)
    val ramp = ColorRamp(0xff0000ff, 0x0000ffff)  // red to blue
    val stops = Array(10000, 20000, 30000, 40000, 50000, 60000, 70000)
    val colorMap = ColorMap(stops, ramp)

    val rgbPng = colorMap.render(tile).map(z => z >> 8).renderPng(RgbPngEncoding(0x00))
    val rgbaPng = colorMap.render(tile).renderPng()

    val rgbImg = ImageIO.read(new ByteArrayInputStream(rgbPng))
    val rgbaImg = ImageIO.read(new ByteArrayInputStream(rgbaPng))

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val actual = rgbImg.getRGB(col, row)
        val expected = rgbaImg.getRGB(col, row)
        withClue(f"$actual%02X does not equal $expected%02X") {
          actual should be (expected)
        }
      }
    }
  }

  test("png encoding produces the same colors for Grey and Greya") {
    val tile: IntArrayTile = IntArrayTile(1 to 256*256 toArray, 256, 256)
    val ramp = ColorRamp(0xff0000ff, 0x0000ffff)  // red to blue
    val stops = Array(10000, 20000, 30000, 40000, 50000, 60000, 70000)
    val colorMap = ColorMap(stops, ramp)

    val greyPng = colorMap.render(tile).map(z => z >> 8 & 0xFF).renderPng(GreyPngEncoding(0x00))
    val greyaPng = colorMap.render(tile).map(z => (z & 0xFF00) | 0xFF).renderPng(GreyaPngEncoding)

    val greyImg = ImageIO.read(new ByteArrayInputStream(greyPng))
    val greyaImg = ImageIO.read(new ByteArrayInputStream(greyaPng))

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val actual = greyImg.getRGB(col, row)
        val expected = greyaImg.getRGB(col, row)
        withClue(f"FAIL ($col, $row): $actual%02X does not equal $expected%02X") {
          actual should be (expected)
        }
      }
    }
  }

  test("png encoding respects set NoData color") {
    val tile: IntArrayTile = IntArrayTile((0 to 256*256 - 1) toArray, 256, 256)
    val ramp = ColorRamp(0xff0000ff, 0x0000ffff)  // red to blue
    val stops = Array(10000, 20000, 30000, 40000, 50000, 60000, 70000)
    val colorMap = ColorMap(stops, ramp)

    val greyPng = colorMap.render(tile).map(z => z >> 8 & 0xFF).renderPng(GreyPngEncoding)
    val greyaPng = colorMap.render(tile).map(z => (z & 0xFF00) | 0xFF).renderPng(GreyaPngEncoding)

    val greyImg = ImageIO.read(new ByteArrayInputStream(greyPng))
    val greyaImg = ImageIO.read(new ByteArrayInputStream(greyaPng))

    cfor(0)(_ < tile.rows, _ + 1) { row =>
      cfor(0)(_ < tile.cols, _ + 1) { col =>
        val actual = greyImg.getRGB(col, row)
        val expected = greyaImg.getRGB(col, row)
        withClue(f"FAIL ($col, $row): $actual%02X does not equal $expected%02X") {
          actual should be (expected)
        }
      }
    }
  }

  test("RGBA raster should respect NoData colors set") {
    val baseDataPath = "raster/data"
    val path = s"$baseDataPath/rgba-test.tif"

    val tiff = GeoTiffRasterSource(path).tiff
    val colors = Vector(0x000000FF, 0xFFFFFFFF)
    val colorRamp = ColorRamp(colors)
    val (min, max) = 54D -> 255D
    val steps = 255

    val step = (max - min) / steps
    val breaks = for (j <- 0 until steps) yield min + j * step

    val singleBandTile = tiff.raster.tile.band(0)

    val cmap =
      ColorMap(breaks.toArray, ColorRamp(colorRamp.stops(steps)), ColorMapOptions.DEFAULT)
        .withFallbackColor(colors.last)

    val renderedTile = cmap.render(singleBandTile)

    val r = renderedTile.map(_.red).interpretAs(UByteCellType)
    val g = renderedTile.map(_.green).interpretAs(UByteCellType)
    val b = renderedTile.map(_.blue).interpretAs(UByteCellType)
    val a = renderedTile.map(_.alpha).interpretAs(UByteCellType)

    val rmm = r.findMinMax
    val gmm = g.findMinMax
    val bmm = b.findMinMax
    val amm = a.findMinMax

    rmm shouldBe (0, 36)
    gmm shouldBe (0, 36)
    bmm shouldBe (0, 36)
    amm shouldBe (0, 255)

    val color = MultibandTile(r, g, b, a).color

    color.findMinMax shouldBe (0, rmm._2 << 24 | gmm._2 << 16 | bmm._2 << 8 | amm._2)

    cfor(0)(_ < color.cols, _ + 1) { col =>
      cfor(0)(_ < color.rows, _ + 1) { row =>
        val az = a.get(col, row)
        val rz = r.get(col, row)
        val gz = g.get(col, row)
        val bz = b.get(col, row)
        val z = color.get(col, row)

        if(az == 0) z shouldBe 0
        else z shouldBe rz << 24 | gz << 16 | bz << 8 | az
      }
    }
  }

  test("RGB raster should respect NoData colors set") {
    val baseDataPath = "raster/data"
    val path = s"$baseDataPath/rgba-test.tif"

    val tiff = GeoTiffRasterSource(path).tiff
    val colors = Vector(0x000000FF, 0xFFFFFFFF)
    val colorRamp = ColorRamp(colors)
    val (min, max) = 54D -> 255D
    val steps = 255

    val step = (max - min) / steps
    val breaks = for (j <- 0 until steps) yield min + j * step

    val singleBandTile = tiff.raster.tile.band(0)

    val cmap =
      ColorMap(breaks.toArray, ColorRamp(colorRamp.stops(steps)), ColorMapOptions.DEFAULT)
        .withFallbackColor(colors.last)

    val renderedTile = cmap.render(singleBandTile)

    val r = renderedTile.map(_.red).interpretAs(IntUserDefinedNoDataCellType(36))
    val g = renderedTile.map(_.green).interpretAs(IntUserDefinedNoDataCellType(36))
    val b = renderedTile.map(_.blue).interpretAs(IntUserDefinedNoDataCellType(36))

    val rmm = r.findMinMax
    val gmm = g.findMinMax
    val bmm = b.findMinMax

    rmm shouldBe (0, 35)
    gmm shouldBe (0, 35)
    bmm shouldBe (0, 35)

    val color = MultibandTile(r, g, b).color

    color.findMinMax shouldBe (0, rmm._2 << 24 | gmm._2 << 16 | bmm._2 << 8 | 0xFF)

    // original tiles not with the reinterpreted cellType
    val rr = renderedTile.map(_.red)
    val gg = renderedTile.map(_.green)
    val bb = renderedTile.map(_.blue)

    var expectedTransparentCounter = 0
    var expectedNonTransparentZerosCounter = 0

    var transparentCounter = 0
    var nonTransparentZerosCounter = 0

    cfor(0)(_ < color.cols, _ + 1) { col =>
      cfor(0)(_ < color.rows, _ + 1) { row =>
        val rz = r.get(col, row)
        val gz = g.get(col, row)
        val bz = b.get(col, row)

        // count the amount of NoDatas and Zeros
        // to prepare "Expected" accumulators
        val rzz = rr.get(col, row)
        val gzz = gg.get(col, row)
        val bzz = bb.get(col, row)

        if(rzz == 36 && gzz == 36 && bzz == 36)
          expectedTransparentCounter += 1

        if(rzz + gzz + bzz == 0)
          expectedNonTransparentZerosCounter += 1

        val transparent = isNoData(rz) && isNoData(gz) && isNoData(bz)

        val z = color.get(col, row)

        if(transparent) {
          transparentCounter += 1
          z shouldBe 0
          rz shouldBe NODATA
          gz shouldBe NODATA
          bz shouldBe NODATA
        }
        else if(z == 0xFF) {
          nonTransparentZerosCounter += 1
          rz shouldNot be(NODATA)
          gz shouldNot be(NODATA)
          bz shouldNot be(NODATA)
        } else z shouldBe rz << 24 | gz << 16 | bz << 8 | 0xFF
      }
    }

    transparentCounter shouldBe expectedTransparentCounter
    nonTransparentZerosCounter shouldBe expectedNonTransparentZerosCounter
  }
}
