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

package geotrellis.vector.dissolve

import geotrellis.vector._
import geotrellis.vector.io._
import geotrellis.vector.io.json.JsonFeatureCollection

import spire.syntax.cfor._

import org.scalatest._

class DissolveMethodsSpec extends FunSpec
    with Matchers {

  object Timer {
    def timedTask[R](msg: String)(block: => R): R = {
      val t0 = System.currentTimeMillis
      val result = block
      val t1 = System.currentTimeMillis
      println(msg + " in " + ((t1 - t0) / 1000.0) + " s")
      result
    }
  }

  def readFile(path: String): Seq[Line] = {
    val src = scala.io.Source.fromFile(path)
    val lines = src.mkString.parseGeoJson[JsonFeatureCollection].getAllLines
    src.close
    lines
  }

  describe("Dissolve") {

    it("should handle two line segments not overlapping") {
      val s = MultiLine(Line((0, 0), (1, 1)), Line((2, 2), (3, 3)))
      s.dissolve.as[MultiLine].get.lines.sortBy(_.hashCode) should be(List(
        Line(Point(0, 0), Point(1, 1)),
        Line(Point(2, 2), Point(3, 3))
      ).sortBy(_.hashCode))
    }

    it("should merge two lines that are coincidental at the middle") {
      val s =
        MultiLine(
          Line( (0, 0), (1, 1), (1, 2), (2, 3)),
          Line( (0, 3), (1, 2), (1, 1), (2, 0))
        )

      s.dissolve.as[MultiLine].get.lines.sortBy(_.hashCode) should be(
        List(
          Line((0, 0), (1, 1)),
          Line((1, 1), (1, 2)),
          Line((0, 3), (1, 2)),
          Line((1, 1), (2, 0)),
          Line((1, 2), (2, 3))
        ).sortBy(_.hashCode)
      )
    }

    it("should handle one line string") {
      val s = Line((0, 0), (1, 1), (2, 2), (3, 3))
      s.dissolve.as[Line].get should be(s)
    }

    it("should handle two line segments overlapping") {
      val s = MultiLine(Line((0, 0), (1, 1)), Line((1, 1), (0, 0)))
      s.dissolve.as[Line].get should be(
        Line(Point(0, 0), Point(1, 1))
      )
    }

    it("should handle two line segments intersecting") {
      val s = MultiLine(Line((0, 0), (1, 1)), Line((1, 0), (0, 1)))
      s.dissolve.as[MultiLine].get.lines.sortBy(_.head.x) should be(List(
        Line(Point(0, 0), Point(1, 1)),
        Line(Point(1, 0), Point(0, 1))
      ).sortBy(_.head.x))
    }

    it("should not throw exceptions when dissolving a multiline that throws a topology exception in JTS for .union call") {
      val testCaseLines = readFile("vector/data/topologyException.json")
      val testCaseMultiLine = MultiLine(testCaseLines)
      an[org.locationtech.jts.geom.TopologyException] should be thrownBy { testCaseMultiLine.union }
      testCaseMultiLine.dissolve
    }

    it("should maintain immutability over dissolve") {
      val s = List(Line((0, 0), (1, 1)), Line((2, 2), (3, 3)))
      val expected = s.map(_.jtsGeom.clone)
      val d = MultiLine(s).dissolve.as[MultiLine].get.lines

      val coord = d(0).jtsGeom.getCoordinate()
      val newCoord = Point(5,5).jtsGeom.getCoordinate()
      coord.setCoordinate(newCoord)

      val js = s.map(_.jtsGeom)
      cfor(0)(_ < expected.length, _ + 1) { i =>
        js(i).equals(expected(i)) should be (true)
      }
    }

    it("should handle an empty MultiLine") {
      val s = MultiLine()
      s.dissolve should be (NoResult)
    }

    it("should handle an empty MultiPolygon") {
      val s = MultiPolygon()
      s.dissolve should be (NoResult)
    }

    it("should handle simple polygon") {
      val l = Line((0, 0), (1, 1), (2, 2), (3, 0), (0, 0))
      val s = Polygon(l)
      s.dissolve.as[Line].get should be(l)
    }
  }

  describe("should read larger files and benchmark faster than MultiLine.union") {

    // (Taken out because the `union` call takes too long).
    ignore("should read seattle lines [long run time]") {
      val seattleLines = readFile("vector/data/seattle.json")

      Timer.timedTask("seattle.json MultiLine.union") {
        MultiLine(seattleLines).union
      }

      Timer.timedTask("seattle.json fast de-duplication") {
        MultiLine(seattleLines).dissolve
      }
    }

    it("should read septa rail lines") {
      val septaRailLines = MultiLine(readFile("vector/data/septaRail.geojson"))

      Timer.timedTask("septaRail.geojson MultiLine.union") {
        septaRailLines.union
      }

      Timer.timedTask("septaRail.geojson dissolve") {
        septaRailLines.dissolve
      }
    }
  }

}
