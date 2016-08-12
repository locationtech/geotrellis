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

package geotrellis.vectortile.protobuf

import geotrellis.vector._
import geotrellis.vectortile.protobuf.internal.ProtobufGeom
import scala.collection.mutable.ListBuffer
import vector_tile.{vector_tile => vt}

import scala.annotation.tailrec

// --- //

case class IncompatibleCommandSequence(e: String) extends Exception

package object internal {

  import com.vividsolutions.jts.geom.LineString
  import java.lang.IllegalArgumentException

  /**
   * Expand a collection of diffs from some reference point into that
   * of `Point` values. The default initial reference point is (0,0).
   */
  // TODO Use a State Monad to carry the cursor value.
  private def expand(diffs: Array[(Int, Int)], curs: (Int, Int) = (0, 0)): ListBuffer[(Int, Int)] = {
    var cursor: (Int, Int) = curs
    val points = new ListBuffer[(Int, Int)]
    var i = 0

    diffs.foreach({
      case (dx, dy) =>
        val here = (dx + cursor._1, dy + cursor._2)

        points.append(here)
        cursor = here
    })

    points
  }

  /**
   * Collapse a collection of Points into that of diffs, relative to
   * the previous point in the sequence.
   */
  private def collapse(points: Array[(Int, Int)], curs: (Int, Int) = (0, 0)): Array[(Int, Int)] = {
    var cursor: (Int, Int) = curs
    val diffs = new Array[(Int, Int)](points.length)
    var i = 0

    while (i < points.length) {
      val curr = points(i)
      val here = (curr._1 - cursor._1, curr._2 - cursor._2)

      diffs.update(i, here)

      cursor = curr

      i += 1
    }

    diffs
  }

  implicit val protoPoint = new ProtobufGeom[Point, MultiPoint] {
    def fromCommands(cmds: Seq[Command]): Either[Point, MultiPoint] = cmds match {
      case MoveTo(ps) +: Nil => {
        val points = expand(ps).map({ case (x, y) => Point(x.toDouble, y.toDouble) })

        if (points.length == 1) Left(points.head) else Right(MultiPoint(points))
      }
      case _ => throw IncompatibleCommandSequence("Expected: [ MoveTo(ps) ]")
    }

    def toCommands(point: Either[Point, MultiPoint]): Seq[Command] = point match {
      case Left(p) => Seq(MoveTo(Array((p.x.toInt, p.y.toInt))))
      case Right(mp) => Seq(MoveTo(
        collapse(mp.points.map(p => (p.x.toInt, p.y.toInt)))
      ))
    }
  }

  implicit val protoLine = new ProtobufGeom[Line, MultiLine] {
    def fromCommands(cmds: Seq[Command]): Either[Line, MultiLine] = {
      @tailrec def work(cs: Seq[Command], lines: ListBuffer[Line], cursor: (Int, Int)): ListBuffer[Line] = cs match {
        case MoveTo(p) +: LineTo(ps) +: rest => {
          val line = Line(expand(p ++ ps, cursor).map({ case (x, y) => (x.toDouble, y.toDouble) }))
          val endPoint: Point = line.points.last
          val nextCursor: (Int, Int) = (endPoint.x.toInt, endPoint.y.toInt)

          work(rest, lines += line, nextCursor)
        }
        case Nil => lines
        case _ => throw IncompatibleCommandSequence("Expected: [ MoveTo(p +: Nil), LineTo(ps), ... ]")
      }

      val lines = work(cmds, new ListBuffer[Line], (0, 0))

      if (lines.length == 1) Left(lines.head) else Right(MultiLine(lines))
    }

    def toCommands(line: Either[Line, MultiLine]): Seq[Command] = {
      def work(lines: Array[Line]): Seq[Command] = {
        var curs: (Int, Int) = (0, 0)
        var buff = new ListBuffer[Command]

        lines.foreach({l =>
          val diffs: Array[(Int, Int)] = collapse(l.points.map(p => (p.x.toInt, p.y.toInt)), curs)

          /* Find new cursor position */
          val endPoint: Point = l.points.last
          curs = (endPoint.x.toInt, endPoint.y.toInt)

          buff.appendAll(Seq(MoveTo(Array(diffs.head)), LineTo(diffs.tail)))
        })

        buff.toSeq
      }

      line match {
        case Left(l) => work(Array(l))
        case Right(ml) => work(ml.lines)
      }
    }
  }

  implicit val protoPolygon = new ProtobufGeom[Polygon, MultiPolygon] {
    def fromCommands(cmds: Seq[Command]): Either[Polygon, MultiPolygon] = {
      @tailrec def work(cs: Seq[Command], lines: ListBuffer[Line], cursor: (Int, Int)): ListBuffer[Line] = cs match {
        case MoveTo(p) +: LineTo(ps) +: ClosePath +: rest => {
          /* `ClosePath` does not move the cursor, so we have to be
           * clever about how we manage the cursor and the closing point
           * of the Polygon.
           */
          val here: (Int, Int) = (p.head._1 + cursor._1, p.head._2 + cursor._2)
          val points = expand(p ++ ps, cursor)
          val nextCursor: (Int, Int) = (points.last._1, points.last._2)

          /* Add the starting point to close the Line into a Polygon */
          points.append(here)

          val line = Line(points.map({ case (x, y) => (x.toDouble, y.toDouble) }))

          work(rest, lines += line, nextCursor)
        }
        case Nil => lines
        case _ => throw IncompatibleCommandSequence("Expected: [MoveTo(p +: Nil), LineTo(ps), ClosePath, ... ]")
      }

      /* Collect all rings, whether external or internal */
      val lines: ListBuffer[Line] = work(cmds, new ListBuffer[Line], (0, 0))

      /* Process interior rings */
      var polys = new ListBuffer[Polygon]
      var currL: Line = lines.head
      var holes = new ListBuffer[Line]

      lines.tail.foreach({ line =>
        val area = surveyor(line)

        if (area < 0) { /* New Interior Rings */
          holes.append(line)
        } else { /* New Exterior Ring */
          /* Save the current state */
          polys.append(Polygon(currL, holes))

          /* Reset the state */
          currL = line
          holes = new ListBuffer[Line]
        }
      })

      /* Save the final state */
      polys.append(Polygon(currL, holes))

      if (polys.length == 1) Left(polys.head) else Right(MultiPolygon(polys))
    }

    def toCommands(poly: Either[Polygon, MultiPolygon]): Seq[Command] = {
      def work(polys: Array[Line]): Seq[Command] = {
        var curs: (Int, Int) = (0, 0)
        var buff = new ListBuffer[Command]

        polys.foreach({ l =>
          /* Exclude the final point via `init` */
          val diffs = collapse(l.points.init.map(p => (p.x.toInt, p.y.toInt)), curs)

          /* Find new cursor position */
          val endPoint: Point = l.points.init.last
          curs = (endPoint.x.toInt, endPoint.y.toInt)

          buff.appendAll(Seq(MoveTo(Array(diffs.head)), LineTo(diffs.tail), ClosePath))
        })

        buff
      }

      poly match {
        case Left(p) => work(p.exterior +: p.holes)
        case Right(mp) => work(mp.polygons.flatMap(p => p.exterior +: p.holes))
      }
    }
  }

  /**
   * The surveyor's formula for calculating the area of a [[Polygon]].
   * If the value reported here is negative, then the [[Polygon]] should be
   * considered an Interior Ring.
   */
  private def surveyor(l: Line): Double = {
    val ps: Array[Point] = l.points.init
    val xs = ps.map(_.x)
    val yns = (ps :+ ps.head).tail.map(_.y)
    val yps = (ps.last +: ps).init.map(_.y)

    var sum: Double = 0
    var i: Int = 0

    while (i < ps.length) {
      sum += xs(i) * (yns(i) - yps(i))

      i += 1
    }

    sum
  }

  implicit def protoVal(value: vt.Tile.Value): Value = {
    if (value.stringValue.isDefined) {
      St(value.stringValue.get)
    } else if (value.floatValue.isDefined) {
      Fl(value.floatValue.get)
    } else if (value.doubleValue.isDefined) {
      Do(value.doubleValue.get)
    } else if (value.intValue.isDefined) {
      I64(value.intValue.get)
    } else if (value.uintValue.isDefined) {
      W64(value.uintValue.get)
    } else if (value.sintValue.isDefined) {
      S64(value.sintValue.get)
    } else if (value.boolValue.isDefined) {
      Bo(value.boolValue.get)
    } else {
      throw new IllegalArgumentException("No legal Protobuf Value given.")
    }
  }
}
