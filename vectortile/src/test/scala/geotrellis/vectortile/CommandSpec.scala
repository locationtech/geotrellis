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

package geotrellis.vectortile

import org.scalatest._
import geotrellis.vectortile.internal.Command._
import geotrellis.vectortile.internal.{ MoveTo, LineTo, ClosePath }

// --- //

class CommandSpec extends FunSpec with Matchers {
  describe("Z-encoding") {
    val ns: Array[Int] = Array(0,-1,1,-2,2,-3,3)

    it("zig should succeed") {
      ns.map(n => zig(n)) shouldBe Array(0,1,2,3,4,5,6)
    }

    it("zig/unzig should form an isomorphism") {
      ns.map(n => unzig(zig(n))) shouldBe ns
    }
  }

  describe("Command Parsing") {
    it("commands/uncommands should form an isomorphism") {
      val ns: Array[Int] = Array(9,4,4,18,6,4,5,4,15)

      uncommands(commands(ns)) shouldBe ns
    }

    it("polygon parse") {
      val ns: Array[Int] = Array(9,4,4,18,6,4,5,4,15)
      val res = commands(ns)

      res(0) match {
        case MoveTo(ds) => ds shouldBe Array((2,2))
        case _ => fail
      }

      res(1) match {
        case LineTo(ds) => ds shouldBe Array((3,2),(-3,2))
        case _ => fail
      }

      res(2) shouldBe ClosePath
    }
  }
}
