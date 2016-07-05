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
import geotrellis.vectortile.protobuf.ProtobufGeom
import scala.collection.mutable.ListBuffer

// --- //

case class IncompatibleCommandSequence(e: String) extends Exception

package object protobuf {

  import com.vividsolutions.jts.geom.LineString

  /**
   * Expand a collection of diffs from some reference point into that
   * of `Point` values. The default initial reference point is (0,0).
   */
  // TODO Use a State Monad to carry the cursor value.
  def expand(diffs: Array[(Int, Int)], curs: (Int, Int) = (0, 0)): Array[(Int, Int)] = {
    var cursor: (Int, Int) = curs
    val points = new Array[(Int, Int)](diffs.length)
    var i = 0

    while (i < diffs.length) {
      val curr = diffs(i)
      val here = (curr._1 + cursor._1, curr._2 + cursor._2)

      points.update(i, here)

      cursor = here

      i += 1
    }

    points
  }

  /**
   * Collapse a collection of Points into that of diffs, relative to
   * the previous point in the sequence.
   */
  def collapse(points: Array[(Int, Int)]): Array[(Int, Int)] = {
    var cursor: (Int, Int) = (0, 0)
    val diffs = new Array[(Int, Int)](points.length)
    var i = 0

    while (i < points.length) {
      val curr = points(i)
      val here = (curr._1 - cursor._1, curr._2 - cursor._2)

      diffs.update(i, here)

      cursor = here

      i += 1
    }

    diffs
  }

  implicit val protoPoint = new ProtobufGeom[Point, MultiPoint] {
    def fromCommands(cmds: Seq[Command]): Either[Point, MultiPoint] = cmds match {
      // TODO Use (::) instead of (+:)? May be faster for pattern matching.
      case MoveTo(ps) +: Nil if ps.length > 0 => {
        val points = expand(ps).map({ case (x, y) => Point(x.toDouble, y.toDouble) })

        if (points.length == 1) Left(points.head) else Right(MultiPoint(points))
      }
      case _ => throw IncompatibleCommandSequence("Expected: [ MoveTo(ps) ]")
    }

    def toCommands(p: Either[Point, MultiPoint]): Seq[Command] = ???
  }

  /*
  implicit val protoLine = new ProtobufGeom[Line] {
    def fromCommands(cmds: Seq[Command]): Line = cmds match {
      case MoveTo(p) +: LineTo(ps) +: Nil => {
        // TODO (++) is bad.
        Line(expand(p ++ ps).map({ case (x,y) => (x.toDouble, y.toDouble) }))
      }
      case _ => throw IncompatibleCommandSequence("Expected: [ MoveTo(p +: Nil), LineTo(ps) ]")
    }

    def toCommands(l: Line): Seq[Command] = ???
  }
   */
  implicit val protoLine = new ProtobufGeom[Line, MultiLine] {
    def fromCommands(cmds: Seq[Command]): Either[Line, MultiLine] = {
      // TODO Make tail recursive?
      def work(cs: Seq[Command], cursor: (Int, Int)): ListBuffer[Line] = cs match {
        case MoveTo(p) +: LineTo(ps) +: rest => {
          val line = Line(expand(p ++ ps, cursor).map({ case (x, y) => (x.toDouble, y.toDouble) }))
          val endPoint: Point = Point(line.jtsGeom.getEndPoint)
          val nextCursor: (Int, Int) = (endPoint.x.toInt, endPoint.y.toInt)

          line +=: work(rest, nextCursor)
        }
        case Nil => new ListBuffer[Line]
        case _ => throw IncompatibleCommandSequence("Expected: [ MoveTo(p +: Nil), LineTo(ps), ... ]")
      }

      val lines = work(cmds, (0, 0))

      if (lines.length == 1) Left(lines.head) else Right(MultiLine(lines))
    }

    def toCommands(ml: Either[Line, MultiLine]): Seq[Command] = ???
  }

  /*
  implicit val protoPolygon = new ProtobufGeom[Polygon] {
    def fromCommands(cmds: Seq[Command]): Polygon = ???

    def toCommands(p: Polygon): Seq[Command] = ???
  }

  implicit class ProtobufMultiPolygon(mp: MultiPolygon) extends ProtobufGeom[MultiPolygon] {
    def fromCommands(cmds: Seq[Command]): MultiPolygon = ???

    def toCommands: Seq[Command] = ???
  }
  */
}
