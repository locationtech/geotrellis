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
import geotrellis.vectortile.protobuf.{Command, ProtobufGeom}
import org.scalatest._

// --- //

class ProtobufGeomSpec extends FunSpec with Matchers {
  describe("Geometry Decoding") {
    it("Point") {
      val ns = Seq(9, 4, 4)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(ns))

      p shouldBe Left(Point(2, 2))
    }

    it("MultiPoint") {
      val ns = Seq(17, 4, 4, 6, 6)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(Command.commands(ns))

      p shouldBe Right(MultiPoint(Point(2, 2), Point(5, 5)))
    }

    it("Line") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(Command.commands(ns))

      l shouldBe Left(Line((2, 2), (5, 4), (2, 6)))
    }

    it("MultiLine") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 9, 4, 4, 18, 6, 4, 5, 4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(Command.commands(ns))

      l shouldBe Right(MultiLine(
        Line((2, 2), (5, 4), (2, 6)),
        Line((4, 8), (7, 10), (4, 12))
      ))
    }

    it("Polygon - One Solid Poly") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(Command.commands(ns))

      p shouldBe Left(Polygon((2, 2), (5, 4), (2, 6), (2, 2)))
    }

    it("Polygon - One Holed Poly") {
      val ns = Seq(9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15, 9, 2, 3, 26, 0, 2, 2, 0, 0, 1, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(Command.commands(ns))

      p shouldBe Left(Polygon(
        exterior = Line((2, 2), (5, 2), (5, 5), (2, 5), (2, 2)),
        holes = Seq(Line((3, 3), (3, 4), (4, 4), (4, 3), (3, 3)))
      ))
    }

    it("MultiPolygon - Two Solid Polys") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 15, 9, 4, 4, 18, 6, 4, 5, 4, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(Command.commands(ns))

      p shouldBe Right(MultiPolygon(
        Polygon((2, 2), (5, 4), (2, 6), (2, 2)),
        Polygon((4, 8), (7, 10), (4, 12), (4, 8))
      ))
    }

    it("MultiPolygon - One Holed, One Solid") {
      val ns = Seq(
        9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15, 9, 2, 3, 26, 0, 2, 2, 0, 0, 1, 15,
        9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15
      )
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(Command.commands(ns))

      p shouldBe Right(MultiPolygon(
        Polygon(
          exterior = Line((2, 2), (5, 2), (5, 5), (2, 5), (2, 2)),
          holes = Seq(Line((3, 3), (3, 4), (4, 4), (4, 3), (3, 3)))
        ),
        Polygon(
          (6, 5), (9, 5), (9, 8), (6, 8), (6, 5)
        )
      ))
    }
  }
}
