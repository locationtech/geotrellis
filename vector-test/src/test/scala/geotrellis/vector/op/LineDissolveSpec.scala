package geotrellis.vector.op

import geotrellis.vector._
import geotrellis.vector.json._

import org.scalatest._

class LineDissolveSpec extends FunSpec
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

  describe("LineDissolve") {

    it("should handle two line segments not overlapping") {
      val s = List(Line((0, 0), (1, 1)), Line((2, 2), (3, 3)))
      new LineDissolveWrapper(s).dissolve.sortBy(_.hashCode) should be(List(
        Line(Point(0, 0), Point(1, 1)),
        Line(Point(2, 2), Point(3, 3))
      ).sortBy(_.hashCode))
    }

    it("should merge two lines that are coincidental at the middle") {
      val s = 
        List(
          Line( (0, 0), (1, 1), (1, 2), (2, 3)),
          Line( (0, 3), (1, 2), (1, 1), (2, 0))
        )

      s.dissolve.sortBy(_.hashCode) should be(
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
      Seq(s).dissolve.sortBy(_.hashCode) should be(List(
        Line(Point(0, 0), Point(1, 1)),
        Line(Point(1, 1), Point(2, 2)),
        Line(Point(2, 2), Point(3, 3))
      ).sortBy(_.hashCode))
    }

    it("should handle two line segments overlapping") {
      val s = List(Line((0, 0), (1, 1)), Line((1, 1), (0, 0)))
      s.dissolve.sortBy(_.hashCode) should be(List(
        Line(Point(0, 0), Point(1, 1))
      ).sortBy(_.hashCode))
    }

    it("should handle two line segments intersecting") {
      val s = List(Line((0, 0), (1, 1)), Line((1, 0), (0, 1)))
      s.dissolve.toList.sortBy(_.points.head.x) should be(List(
        Line(Point(0, 0), Point(1, 1)),
        Line(Point(1, 0), Point(0, 1))
      ).sortBy(_.points.head.x))
    }

    it("should not throw exceptions when dissolving a multiline that throws a topology exception in JTS for .union call") {
      val testCase = readFile("vector-test/data/topologyException.json")
      an[com.vividsolutions.jts.geom.TopologyException] should be thrownBy { MultiLine(testCase).union }
      MultiLine(testCase.dissolve)
    }

  }

  describe("should read larger files and benchmark faster than MultiLine.union") {

//    val seattleLines = readFile("vector-test/data/seattle.json") // (Taken out because the `union` call takes too long).

    val septaRailLines = readFile("vector-test/data/septaRail.geojson")

    // it("should read seattle lines") {
    //   Timer.timedTask("seattle.json MultiLine.union") {
    //     MultiLine(seattleLines).union
    //   }

    //   Timer.timedTask("seattle.json fast de-duplication") {
    //     seattleLines.dissolve
    //   }
    // }

    it("should read septa rail lines") {
      Timer.timedTask("septaRail.geojson MultiLine.union") {
        MultiLine(septaRailLines).union
      }

      Timer.timedTask("septaRail.geojson dissolve") {
        septaRailLines.dissolve
      }
    }
  }

}
