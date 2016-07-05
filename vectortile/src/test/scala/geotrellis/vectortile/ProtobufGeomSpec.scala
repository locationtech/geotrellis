/*
 * Copyright (c) 2016 Azavea.
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

package geotrellis.vectortile

import geotrellis.vector._
import geotrellis.vectortile.protobuf.{ Command, ProtobufGeom }
import org.scalatest._

// --- //

class ProtobufGeomSpec extends FunSpec with Matchers {
  describe("Geometry Decoding") {
    it("Point") {
      val ns = Seq(9,4,4)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(ns))

      p shouldBe Left(Point(2,2))
    }

    it("MultiPoint") {
      val ns = Seq(17,4,4,6,6)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(ns))

      p shouldBe Right(MultiPoint(Point(2,2), Point(5,5)))
    }

    it("Line") {
      val ns = Seq(9,4,4,18,6,4,5,4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(Command.commands(ns))

      l shouldBe Left(Line((2,2), (5,4), (2,6)))
    }

    it("MultiLine") {
      val ns = Seq(9,4,4,18,6,4,5,4,9,4,4,18,6,4,5,4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(Command.commands(ns))

      l shouldBe Right(MultiLine(
        Line((2,2), (5,4), (2,6)),
        Line((4,8), (7,10), (4,12))
      ))
    }
  }
}
