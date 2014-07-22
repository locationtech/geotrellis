/*
 * Copyright (c) 2014 Azavea.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package geotrellis.raster.op.focal

import geotrellis.raster._
import geotrellis.engine._
import geotrellis.feature.Extent
import geotrellis.raster.op._
import geotrellis.raster.render._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._

class HillshadeSpec extends FunSuite 
                       with TestEngine 
                       with TileBuilders {
  def grayscale(n: Int) = {
    val ns = (1 to 128).toArray
    val limits = ns.map(i => i * n)
    val colors = ns.map(i => ((i * 65536 * 2 + i * 256 * 2 + i * 2) << 8) | 255)
    ColorBreaks(limits, colors)
  }

  def time() = System.currentTimeMillis()

  test("should get the same result for split raster") {
    val rasterExtent = RasterSource(LayerId("test:fs", "elevation")).rasterExtent.get
    val rOp = getRaster("elevation")
    val nonTiledSlope = get(rOp).hillshade(rasterExtent.cellSize, 315.0, 45.0, 1.0)

    val tiled =
      rOp.map { r =>
        val (tcols, trows) = (11, 20)
        val pcols = r.cols / tcols
        val prows = r.rows / trows
        val tl = TileLayout(tcols, trows, pcols, prows)
        CompositeTile.wrap(r, tl)
      }

    val rs = RasterSource(tiled, rasterExtent.extent)
    run(rs.hillshade(315.0, 45.0, 1.0)) match {
      case Complete(result, success) =>
        //          println(success)
        assertEqual(result, nonTiledSlope)
      case Error(msg, failure) =>
        println(msg)
        println(failure)
        assert(false)
    }
  }

  test("should run Hillshade square 3 on tiled raster in catalog") {
    val name = "SBN_inc_percap"

    val source = RasterSource(name)

    val r = source.get
    val rasterExtent = source.rasterExtent.get
    val tileLayout =
      TileLayout(
        (rasterExtent.cols + 255) / 256,
        (rasterExtent.rows + 255) / 256,
        256,
        256
      )

    val extent = rasterExtent.adjustTo(tileLayout).extent

    val rs = RasterSource(CompositeTile.wrap(r, tileLayout, cropped = false), extent)

    val expected = source.hillshade.get
    rs.hillshade.run match {
      case Complete(value, hist) =>
        // Dont check last col or last row. 
        // Reason is, because the offsetting of the tiles, the tiled version
        // pulls in NoData's where the non tiled just has the edge value,
        // so the calculations produce different results. Which makes sense.
        // So if your going for accuracy don't tile something that create NoData 
        // borders.
        cfor(0)(_ < expected.cols - 1, _ + 1) { col =>
          cfor(0)(_ < expected.rows - 1, _ + 1) { row =>
            withClue (s"Value different at $col, $row: ") {
              val v1 = value.getDouble(col, row)
              val v2 = expected.getDouble(col, row)
              if(isNoData(v1)) isNoData(v2) should be (true)
              else if(isNoData(v2)) isNoData(v1) should be (true)
              else v1 should be (v2)
            }
          }
        }
      case Error(message, trace) =>
        println(message)
        assert(false)
    }
  }
}
