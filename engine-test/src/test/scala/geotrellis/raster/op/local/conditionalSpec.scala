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

package geotrellis.raster.op.local

import geotrellis.raster._
import geotrellis.engine._

import org.scalatest._

import scala.math.min

import geotrellis.testkit._

class ConditionalSpec extends FunSpec 
                         with Matchers 
                         with TestEngine 
                         with TileBuilders {
  describe("IfCell") {
    it("conditionally combines two tiled RasterSources correctly") {
      val rs1 = RasterSource("quad_tiled")
      val rs2 = RasterSource("quad_tiled2") + 1

      val r1 = get(rs1)
      val r2 = get(rs2)
      run(rs1.localIf(rs2,(a:Int,b:Int)=> a < b, 1, 0)) match {
        case Complete(result,success) =>
//          println(success)
          for(row <- 0 until r1.rows) {
            for(col <- 0 until r1.cols) {
              result.get(col,row) should be (1)
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
