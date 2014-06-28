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
import geotrellis.raster.stats._
import geotrellis.testkit._

import org.scalatest._

import spire.syntax.cfor._
import scala.math._

class MedianSpec extends FunSpec with TestEngine
                                 with FocalOpSpec
                                 with Matchers {

  describe("Median") {
    it("should match worked out results") {
      val r = createTile(Array(3, 4, 1, 1, 1,
                               7, 4, 0, 1, 0,
                               3, 3, 7, 7, 1,
                               0, 7, 2, 0, 0,
                               6, 6, 6, 5, 5))

      var result = get(Median(r,Square(1)))

      def median(s:Int*) = {
        if(s.length % 2 == 0) {
        val middle = (s.length/2) -1          
          (s(middle) + s(middle+1)) / 2         
        } else {
          s(s.length/2)
        }
      }

      result.get(0,0) should equal (median(2,4,4,7))
      result.get(1,0) should equal (median(0,1,3,4,4,7))
      result.get(2,0) should equal (median(0,1,1,1,4,4))
      result.get(3,0) should equal (median(0,0,1,1,1,1))
      result.get(4,0) should equal (median(0,1,1,1))
      result.get(0,1) should equal (median(3,3,3,4,4,7))
      result.get(1,1) should equal (median(0,1,3,3,3,4,4,7,7))
      result.get(2,1) should equal (median(0,1,1,1,3,4,4,7,7))
      result.get(3,1) should equal (median(0,0,1,1,1,1,1,7,7))
      result.get(4,1) should equal (median(0,1,1,1,1,7))
      result.get(0,2) should equal (median(0,3,3,4,7,7))
      result.get(1,2) should equal (median(0,0,2,3,3,4,7,7,7))
      result.get(2,2) should equal (median(0,0,1,2,3,4,7,7,7))
      result.get(3,2) should equal (median(0,0,0,0,1,1,2,7,7))
      result.get(4,2) should equal (median(0,0,0,1,1,7))
      result.get(0,3) should equal (median(0,3,3,6,6,7))
      result.get(1,3) should equal (median(0,2,3,3,6,6,6,7,7))
      result.get(2,3) should equal (median(0,2,3,5,6,6,7,7,7))
      result.get(3,3) should equal (median(0,0,1,2,5,5,6,7,7))
      result.get(4,3) should equal (median(0,0,1,5,5,7))
      result.get(0,4) should equal (median(0,6,6,7))
      result.get(1,4) should equal (median(0,2,6,6,6,7))
      result.get(2,4) should equal (median(0,2,5,6,6,7))
      result.get(3,4) should equal (median(0,0,2,5,5,6))
      result.get(4,4) should equal (median(0,0,5,5))
    }

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
//          println(success)
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

      val tileLayout = TileLayout.fromTileDimensions(rasterExtent,256,256)
      val rs = RasterSource(CompositeTile.wrap(r,tileLayout,cropped = false), rasterExtent.extent)

      rs.focalMedian(Square(3)).run match {
        case Complete(value,hist) =>
          cfor(0)(_ < expected.cols, _ + 1) { col =>
            cfor(0)(_ < expected.rows, _ + 1) { row =>
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
