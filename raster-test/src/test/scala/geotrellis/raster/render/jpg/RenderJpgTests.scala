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

package geotrellis.raster.render.jpg

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.raster.testkit._

import spire.syntax.cfor._
import org.scalatest._

import java.io._
import javax.imageio._

class RenderJpgTests extends FunSuite with Matchers with TileBuilders with RasterMatchers {
  def naiveRgbDistance(c1: Int, c2: Int): Double =
    (c1.red - c2.red) * (c1.red - c2.red) + (c1.green - c2.green) * (c1.green - c2.green) + (c1.blue - c2.blue) * (c1.blue - c2.blue)

  /** Find the average (naive) RGB distance between two images and test that it is below the
    * specified threshold (somewhat imprecise due to RGB colorspace not mapping neatly onto
    * the visual colorspace)
    */
  def testJpg(jpg: Jpg, tile: Tile, colorMap: ColorMap, threshold: Double): Unit = {
    val img = ImageIO.read(new ByteArrayInputStream(jpg.bytes))

    img.getWidth should be (tile.cols)
    img.getHeight should be (tile.rows)

    var distances = 0.0
    cfor(0)(_ < img.getWidth, _ + 1) { col =>
      cfor(0)(_ < img.getHeight, _ + 1) { row =>
        val argb = img.getRGB(col, row)
        val actual = (argb << 8) | ((argb >> 24) & 0xFF)
        val expected = colorMap.map(tile.get(col, row))
        distances = distances + naiveRgbDistance(actual, expected)
      }
    }
    distances / (tile.cols * tile.rows) should be < (threshold)
  }

  def createConsecutiveTile(d: Int, start: Int): Tile = {
    val tile = ArrayTile.empty(IntCellType, d, d)
    var i = start
    cfor(0)(_ < d, _ + 1) { row =>
      cfor(0)(_ < d, _ + 1) { col =>
        tile.set(col, row, i)
        i += 1
      }
    }
    tile
  }

  test("render a JPG from a Double tile and ensure it (roughly) matches what is read in by ImageIO when written") {
    val tileNW =
      createValueTile(50, 1)
    val tileNE =
      createValueTile(50, 2)
    val tileSW =
      createValueTile(50, 3)
    val tileSE =
      createValueTile(50, 4)

    val tile =
      CompositeTile(Seq(tileNW, tileNE, tileSW, tileSE), TileLayout(2, 2, 50, 50))
        .convert(DoubleConstantNoDataCellType)
        .toArrayTile


    val colorMap =
      ColorMap(
        Map(
          1.0 -> RGB(255, 0, 0).int,
          2.0 -> RGB(0, 255, 0).int,
          3.0 -> RGB(0, 0, 255).int,
          4.0 -> RGB(0, 255, 255).int
        )
      )

    val jpg = tile.renderJpg(colorMap)

    testJpg(jpg, tile, colorMap, threshold=250.0)
  }

  test("render a JPG from an Int tile and ensure it (roughly) matches what is read in by ImageIO when written") {
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
          1 -> RGB(255, 0, 0).int,
          2 -> RGB(0, 255, 0).int,
          3 -> RGB(0, 0, 255).int,
          4 -> RGB(0, 255, 255).int
        )
      )

    val jpg = tile.renderJpg(colorMap)

    testJpg(jpg, tile, colorMap, threshold=250.0)
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
          1 -> RGB(255, 0, 0).int,
          2 -> RGB(0, 255, 0).int,
          3 -> RGB(0, 0, 255).int,
          4 -> RGB(0, 255, 255).int
        )
      )

    val doubleColorMap =
      ColorMap(
        Map(
          1.0 -> RGB(255, 0, 0).int,
          2.0 -> RGB(0, 255, 0).int,
          3.0 -> RGB(0, 0, 255).int,
          4.0 -> RGB(0, 255, 255).int
        )
    )

    val intJpg = intTile.renderJpg(intColorMap)
    val doubleJpg = doubleTile.renderJpg(doubleColorMap)

    val intImg = ImageIO.read(new ByteArrayInputStream(intJpg.bytes))
    val doubleImg = ImageIO.read(new ByteArrayInputStream(doubleJpg.bytes))

    cfor(0)(_ < intImg.getWidth, _ + 1) { col =>
      cfor(0)(_ < intImg.getHeight, _ + 1) { row =>
        intImg.getRGB(col, row) should be (doubleImg.getRGB(col, row))
      }
    }
  }

  test("should render nodata to black (0x000000)") {
    val tile = IntArrayTile.fill(Int.MinValue, 256, 256)

    val colorMap =
      ColorMap(
        Map(
          1 -> RGB(255, 0, 0).int,
          2 -> RGB(0, 255, 0).int,
          3 -> RGB(0, 0, 255).int,
          4 -> RGB(0, 255, 255).int
        )
      )

    val jpg = tile.renderJpg(colorMap)
    val img = ImageIO.read(new ByteArrayInputStream(jpg.bytes))

    img.getWidth should be (tile.cols)
    img.getHeight should be (tile.rows)

    var distances = 0.0
    cfor(0)(_ < img.getWidth, _ + 1) { col =>
      cfor(0)(_ < img.getHeight, _ + 1) { row =>
        img.getRGB(col, row).isTransparent should be (true)
      }
    }
  }
}
