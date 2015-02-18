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

package geotrellis.engine.op.focal

import geotrellis.engine._
import geotrellis.raster._
import geotrellis.raster.op.focal._
import geotrellis.raster.histogram._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._
import scala.math._

class MedianSpec extends FunSpec with TestEngine
                                 with TileBuilders
                                 with Matchers {

  describe("Median") {

    it("should get median on raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,1,      1,3,5,      9,8,2,
                9,1,1,      2,2,2,      4,3,5,

                3,8,1,     3, 3,3,      1,2,2,
                2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMedian(Square(1))) match {
        case Complete(result,success) =>
          assertEqual(result,
            Array(7, 1, 1,    1, 2, 3,    4, 4, 4,
                  7, 2, 1,    2, 3, 3,    3, 3, 2,

                  3, 3, 2,    2, 2, 2,    3, 3, 3,
                  3, 3, 3,    3, 3, 3,    2, 2, 2))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should run median square 3 on tiled raster in catalog") {
      val name = "SBN_inc_percap"

      val source = RasterSource(name)
      val rasterExtent = source.rasterExtent.get
      val r = source.get

      val expected = source.focalMedian(Square(3)).get

      val tileLayout =
        TileLayout(
          (rasterExtent.cols / 256) + 1,
          (rasterExtent.rows / 256) + 1,
          256,
          256
        )

      val rs = RasterSource(CompositeTile.wrap(r,tileLayout,cropped = false), rasterExtent.extent)

      rs.focalMedian(Square(3)).run match {
        case Complete(value,hist) =>
          cfor(0)(_ < expected.rows, _ + 1) { row =>
            cfor(0)(_ < expected.cols, _ + 1) { col =>
              withClue (s"Value different at $col,$row: ") {
                val v1 = expected.getDouble(col,row)
                val v2 = value.getDouble(col,row)
                if(isNoData(v1)) isNoData(v2) should be (true)
                else if(isNoData(v2)) isNoData(v1) should be (true)
                else v1 should be (v2)
              }
            }
          }
        case Error(message,trace) =>
          println(message)
          assert(false)
      }
    }
  }
}
