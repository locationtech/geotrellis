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

import geotrellis.vector._
import geotrellis.vectortile.internal.{Command, ProtobufGeom}
import org.scalatest._

// --- //

class ProtobufGeomSpec extends FunSpec with Matchers {
  /* Naive `LayoutDefinition` values */
  val topLeft = Point(0, 4096)
  val resolution: Double = 1

  describe("Geometry Isomorphisms") {
    it("Point") {
      val ns = Seq(9, 4, 4)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Left(Point(2, 4094))
      Command.uncommands(implicitly[ProtobufGeom[Point, MultiPoint]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("MultiPoint") {
      val ns = Seq(25, 4, 4, 6, 6, 3, 3)
      val p = implicitly[ProtobufGeom[Point, MultiPoint]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Right(MultiPoint(Point(2, 4094), Point(5, 4091), Point(3, 4093)))
      Command.uncommands(implicitly[ProtobufGeom[Point, MultiPoint]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("Line") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      l shouldBe Left(Line((2, 4094), (5, 4092), (2, 4090)))
      Command.uncommands(implicitly[ProtobufGeom[Line, MultiLine]].toCommands(
        l,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("MultiLine") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 9, 4, 4, 18, 6, 4, 5, 4)
      val l = implicitly[ProtobufGeom[Line, MultiLine]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      l shouldBe Right(MultiLine(
        Line((2, 4094), (5, 4092), (2, 4090)),
        Line((4, 4088), (7, 4086), (4, 4084))
      ))
      Command.uncommands(implicitly[ProtobufGeom[Line, MultiLine]].toCommands(
        l,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("Polygon - One Solid Poly") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Left(Polygon((2, 4094), (5, 4092), (2, 4090), (2, 4094)))
      Command.uncommands(implicitly[ProtobufGeom[Polygon, MultiPolygon]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("Polygon - One Holed Poly") {
      val ns = Seq(9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15, 9, 2, 3, 26, 0, 2, 2, 0, 0, 1, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Left(Polygon(
        exterior = Line((2, 4094), (5, 4094), (5, 4091), (2, 4091), (2, 4094)),
        holes = Seq(Line((3, 4093), (3, 4092), (4, 4092), (4, 4093), (3, 4093)))
      ))
      Command.uncommands(implicitly[ProtobufGeom[Polygon, MultiPolygon]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("MultiPolygon - Two Solid Polys") {
      val ns = Seq(9, 4, 4, 18, 6, 4, 5, 4, 15, 9, 4, 4, 18, 6, 4, 5, 4, 15)
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Right(MultiPolygon(
        Polygon((2, 4094), (5, 4092), (2, 4090), (2, 4094)),
        Polygon((4, 4088), (7, 4086), (4, 4084), (4, 4088))
      ))
      Command.uncommands(implicitly[ProtobufGeom[Polygon, MultiPolygon]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }

    it("MultiPolygon - One Holed, One Solid") {
      val ns = Seq(
        9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15, 9, 2, 3, 26, 0, 2, 2, 0, 0, 1, 15,
        9, 4, 4, 26, 6, 0, 0, 6, 5, 0, 15
      )
      val p = implicitly[ProtobufGeom[Polygon, MultiPolygon]].fromCommands(
        Command.commands(ns),
        topLeft,
        resolution
      )

      p shouldBe Right(MultiPolygon(
        Polygon(
          exterior = Line((2, 4094), (5, 4094), (5, 4091), (2, 4091), (2, 4094)),
          holes = Seq(Line((3, 4093), (3, 4092), (4, 4092), (4, 4093), (3, 4093)))
        ),
        Polygon(
          (6, 4091), (9, 4091), (9, 4088), (6, 4088), (6, 4091)
        )
      ))
      Command.uncommands(implicitly[ProtobufGeom[Polygon, MultiPolygon]].toCommands(
        p,
        topLeft,
        resolution
      )) shouldBe ns
    }
  }
}
