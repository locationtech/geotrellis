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

package geotrellis.engine.op.local

import geotrellis.raster._
import geotrellis.engine._

import org.scalatest._

import geotrellis.testkit._

class UndefinedSpec extends FunSpec 
                       with Matchers 
                       with TestEngine 
                       with TileBuilders {
  describe("Undefined") {
    it("returns correct result for a double raster source correctly") {
      val rs = RasterSource("mtsthelens_tiled")

      val r = get(rs)
      run(rs.localUndefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r.rows / 3) {
            for(col <- 0 until r.cols / 3) {
              val z = r.getDouble(col,row)
              val rz = result.get(col,row)
              if(isNoData(z))
                rz should be (1)
              else 
                rz should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("returns correct result for a int raster source") {
      val rs = createRasterSource(
        Array( NODATA,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1,

               1,1,1, 1,1,1, 1,1,1,
               1,1,1, 1,1,1, 1,1,1),
        3,2,3,2)

      run(rs.localUndefined) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until 4) {
            for(col <- 0 until 9) {
              if(row == 0 && col == 0)
                result.get(col,row) should be (1)
              else
                result.get(col,row) should be (0)
            }
          }
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
