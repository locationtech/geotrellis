package geotrellis.vector

import geotrellis.vector.LineDeDuplication._
import geotrellis.vector.json._

import org.scalatest._

class LineDeDuplicationSpec extends FunSpec
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

  describe("should work as expected") {

    it("should handle two line segments not overlapping") {
      val s = List(Line((0, 0), (1, 1)), Line((2, 2), (3, 3)))
      s.deDuplicate.sortBy(_.hashCode) should be(List(
        (Point(0, 0), Point(1, 1)),
        (Point(2, 2), Point(3, 3))
      ).sortBy(_.hashCode))
    }

    it("should handle one line string") {
      val s = Line((0, 0), (1, 1), (2, 2), (3, 3))
      Seq(s).deDuplicate.sortBy(_.hashCode) should be(List(
        (Point(0, 0), Point(1, 1)),
        (Point(1, 1), Point(2, 2)),
        (Point(2, 2), Point(3, 3))
      ).sortBy(_.hashCode))
    }

    it("should handle two line segments overlapping") {
      val s = List(Line((0, 0), (1, 1)), Line((1, 1), (0, 0)))
      s.deDuplicate.sortBy(_.hashCode) should be(List(
        (Point(0, 0), Point(1, 1)),
        (Point(1, 1), Point(0, 0))
      ).sortBy(_.hashCode))
    }

    it("should handle two line segments intersecting") {
      val s = List(Line((0, 0), (1, 1)), Line((1, 0), (0, 1)))
      s.deDuplicate.sortBy(_.hashCode) should be(List(
        (Point(0, 0), Point(1, 1)),
        (Point(1, 0), Point(0, 1))
      ).sortBy(_.hashCode))
    }

  }

  def readFile(path: String): Seq[Line] = {
    val src = scala.io.Source.fromFile(path)
    val lines = src.mkString.parseGeoJson[JsonFeatureCollection].getAllLines
    src.close
    lines
  }

  describe("should read larger files and benchmark faster than MultiLine.union") {

    val seattleLines = readFile("vector-test/data/seattle.json")

    val septaRailLines = readFile("vector-test/data/septaRail.geojson")

    it("should read seattle lines") {
      Timer.timedTask("seattle.json MultiLine.union") {
        MultiLine(seattleLines).union
      }

      Timer.timedTask("seattle.json fast de-duplication") {
        seattleLines.deDuplicate
      }
    }

    it("should read septa rail lines") {
      Timer.timedTask("septaRail.geojson MultiLine.union") {
        MultiLine(septaRailLines).union
      }

      Timer.timedTask("septaRail.geojson fast de-duplication") {
        septaRailLines.deDuplicate
      }
    }
  }

}
