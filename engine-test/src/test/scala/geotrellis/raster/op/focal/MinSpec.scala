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

import geotrellis.engine._
import geotrellis.testkit._
import org.scalatest._

class MinSpec extends FunSpec with TileBuilders
                              with Matchers
                              with TestEngine {
  describe("Min") {
    it("should square min for raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,1,      1,3,5,      9,8,2,
                9,1,1,      2,2,2,      4,3,5,

                3,8,1,      3,3,3,      1,2,2,
                2,4,7,     1,nd,1,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMin(Square(1))) match {
        case Complete(result,success) =>
          assertEqual(result,
            Array(1, 1, 1,    1, 1, 2,    2, 2, 2,
                  1, 1, 1,    1, 1, 1,    1, 1, 2,

                  1, 1, 1,    1, 1, 1,    1, 1, 2,
                  2, 1, 1,    1, 1, 1,    1, 1, 2))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)
      }
    }

    it("should square min for raster source with double") {
      val rs1 = createRasterSource(
        Array( NaN,7.1,1.2,      1.4,3.9,5.1,      9.9,8.1,2.2,
               9.4,1.1,1.5,      2.5,2.2,2.9,      4.0,3.3,5.1,

               3.4,8.2,1.9,      3.8,3.1,3.0,      1.3,2.1,2.5,
               2.5,4.9,7.1,      1.4,NaN,1.1,      8.0,4.8,3.0
        ),
        3,2,3,2
      )

      run(rs1.focalMin(Square(1))) match {
        case Complete(result,success) =>
          assertEqual(result,
            Array(1.1, 1.1, 1.1,    1.2, 1.4, 2.2,    2.9, 2.2, 2.2,
                  1.1, 1.1, 1.1,    1.2, 1.4, 1.3,    1.3, 1.3, 2.1,

                  1.1, 1.1, 1.1,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1,
                  2.5, 1.9, 1.4,    1.4, 1.1, 1.1,    1.1, 1.3, 2.1))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should square min with 5x5 neighborhood") {
      val rs1 = createRasterSource(
        Array( nd,7,7,      7,3,5,      9,8,2,
                9,7,7,      2,2,2,      4,3,5,

                3,8,7,      3,3,3,      7,4,5,
                9,4,7,     7,nd,7,      8,4,3
        ),
        3,2,3,2
      )

      run(rs1.focalMin(Square(2))) match {
        case Complete(result,success) =>
          assertEqual(result,
            Array(3, 2, 2,    2, 2, 2,    2, 2, 2,
                  3, 2, 2,    2, 2, 2,    2, 2, 2,

                  3, 2, 2,    2, 2, 2,    2, 2, 2,
                  3, 2, 2,    2, 2, 2,    2, 2, 3))
        case Error(msg,failure) =>
          println(msg)
          println(failure)
          assert(false)

      }
    }

    it("should circle min for raster source") {
      val rs1 = createRasterSource(
        Array( nd,7,4,     5, 4,2,      9,nd,nd,
                9,6,2,     2, 2,2,      5,3,nd,

                3,8,4,     3, 3,3,      3,9,2,
                2,9,7,     4,nd,9,      8,8,4
        ),
        3,2,3,2
      )

      run(rs1.focalMin(Circle(1))) match {
        case Complete(result,success) =>
          assertEqual(result,
            Array(7, 4, 2,    2, 2, 2,    2, 3, nd,
                  3, 2, 2,    2, 2, 2,    2, 3, 2,

                  2, 3, 2,    2, 2, 2,    3, 2, 2,
                  2, 2, 4,    3, 3, 3,    3, 4, 2))
        case Error(msg,failure) =>
          assert(false)
      }
    }
  }
}
