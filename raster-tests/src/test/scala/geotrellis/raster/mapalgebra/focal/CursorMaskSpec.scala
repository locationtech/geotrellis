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

package geotrellis.raster.mapalgebra.focal


import scala.collection.mutable.Set

import Movement._

import org.scalatest.matchers.should.Matchers
import org.scalatest.funspec.AnyFunSpec

class CursorMaskSpec extends AnyFunSpec with Matchers {
  val testArray = Array(Array( 1,  2,  3,  4),
                        Array( 5,  6,  7,  8),
                        Array( 9, 10, 11, 12),
                        Array(13, 14, 15, 16))

  describe("CursorMask") {
    it("should mask correctly for center square shape") {
      val mask = TestCursor.maskFromString("""
             X X X X
             X 0 0 X
             X 0 0 X
             X X X X
                                           """)

      val s = Set[Int]()

      for(y <- 0 to 3) {
        mask.foreachX(y) { x => s += testArray(y)(x) }
      }

      s.toSeq.sorted should equal (Seq(6,7,10,11))
    }

    it("should calculate masked values correctly when moving up") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 X 0 0 X
                 X X X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()

      mask.foreachMasked(Up) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(10,11))
    }

    it("should calculated masked values correctly when moving down") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)
      val s = Set[Int]()
      mask.foreachMasked(Down) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(1,2,4,7,12))
    }

    it("should calculate unmasked values correctly when moving up") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 X 0 0 X
                 X X X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()

      mask.foreachUnmasked(Up) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(6,7,14,15))
    }

    it("should calculated unmasked values correctly when moving down") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)
      val s = Set[Int]()

      mask.foreachUnmasked(Down) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,8,11))
    }

    it("should calculate masked values correctly when moving left") {
      val mask = TestCursor.maskFromString("""
                 X 0 X X
                 X 0 X 0
                 X 0 X X
                 X 0 0 X
                                           """)
      val s = Set[Int]()
      mask.foreachMasked(Left) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(3,7,11,16))
    }

    it("should calculated masked values correctly when moving right") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachMasked(Right) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(7,9,13,15))
    }

    it("should calculate unmasked values correctly when moving left") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 0 X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachUnmasked(Left) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(8,10,14,16))
    }

    it("should calculated unmasked values correctly when moving right") {
      val mask = TestCursor.maskFromString("""
                 X X X X
                 0 X X 0
                 X 0 0 X
                 X 0 X 0
                                           """)

      val s = Set[Int]()

      mask.foreachUnmasked(Right) { (x,y) => s += testArray(y)(x) }
      s.toSeq.sorted should equal (Seq(5,11,14))
    }

    it("should calculate correct west column for mask") {
      val mask = TestCursor.maskFromString("""
                    0 X 0 X 0
                    0 0 X 0 X
                    0 X X X 0
                    X 0 X 0 0
                    0 X 0 X 0
                                           """)
      val s = Set[Int]()
      
      mask.foreachWestColumn { v => s += v }

      s.toSeq.sorted should equal (Seq(0,1,2,4))
    }

    it("should calculate correct east column for mask") {
      val mask = TestCursor.maskFromString("""
                    0 X 0 X 0
                    0 0 X 0 X
                    0 X X X 0
                    X 0 X 0 0
                    0 X 0 X X
                                           """)
      val s = Set[Int]()
      
      mask.foreachEastColumn { v => s += v }

      s.toSeq.sorted should equal (Seq(0,2,3))
    }
  }
}
