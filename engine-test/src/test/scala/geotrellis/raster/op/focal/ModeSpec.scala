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
import geotrellis.testkit._

import org.scalatest._
import org.scalatest.matchers.{Matcher, MatchResult}

import scala.math._

class ModeSpec extends FunSpec with FocalOpSpec
                               with Matchers 
                               with TileBuilders
                               with TestEngine {

  val getModeResult = Function.uncurried((getCursorResult _).curried((r,n) => Mode(r,n)))

  describe("Mode") {
    it("should match worked out results") {
      val r = createTile(Array(
         3, 4, 1, 1, 1,
         7, 4, 0, 1, 0,
         3, 3, 7, 7, 1,
         0, 7, 2, 0, 0,
         6, 6, 6, 5, 5
      ))

      //val result = get(Mode(r,Square(1)))
      val result = r.focalMode(Square(1))
      val beIn:Seq[Int]=>Matcher[Int] = seq => Matcher { x =>
        MatchResult(seq.contains(x), s"${x} was not in ${seq}", s"${x} was in ${seq}")
      }

      result.get(0,0) should equal (4)
      result.get(1,0) should equal (4)
      result.get(2,0) should equal (1)
      result.get(3,0) should equal (1)
      result.get(4,0) should equal (1)
      result.get(0,1) should equal (3)
      result.get(1,1) should equal (3)
      result.get(2,1) should equal (1)
      result.get(3,1) should equal (1)
      result.get(4,1) should equal (1)
      result.get(0,2) should equal (nd)
      result.get(1,2) should equal (7)
      result.get(2,2) should equal (7)
      result.get(3,2) should equal (0)
      result.get(4,2) should equal (0)
      result.get(0,3) should equal (nd)
      result.get(1,3) should equal (6)
      result.get(2,3) should equal (7)
      result.get(3,3) should equal (nd)
      result.get(4,3) should equal (nd)
      result.get(0,4) should equal (6)
      result.get(1,4) should equal (6)
      result.get(2,4) should equal (6)
      result.get(3,4) should equal (nd)
      result.get(4,4) should equal (nd)
    }

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
//          println(success)
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
