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
import geotrellis.raster.op.focal._
import geotrellis.testkit._
import org.scalatest._

class ModeSpec extends FunSpec with Matchers
                               with TileBuilders
                               with TestEngine {

  describe("Mode") {
    it("should get mode for raster source") {
      val rs1 = createRasterSource(
        Array(
          nd,7,1,    1, 3,5,      9,8,2,
          9,1,1,     2, 2,2,      4,3,5,

          3,8,1,     3, 3,3,      1,2,2,
          2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMode(Square(1))) match {
        case Complete(result,success) =>
          assertEqual(result, Array(
            nd, 1, 1,    1, 2, 2,   nd,nd,nd,
            nd, 1, 1,    1, 3, 3,   nd, 2, 2,

            nd, 1, 1,    1,nd,nd,   nd,nd,nd,
            nd,nd, 1,   nd, 3,nd,    1, 2, 2))


        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }
  }
}
