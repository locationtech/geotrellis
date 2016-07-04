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

// --- //

case class IncompatibleCommandSequence(e: String) extends Exception

package object protobuf {
  /**
   * Expand a collection of diffs from some reference point into that
   * of `Point` values. The initial reference point if (0,0).
   */
  def expand(diffs: Array[(Int, Int)]): Array[(Int, Int)] = {
    var cursor: (Int, Int) = (0, 0)
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

  implicit val protoPoint = new ProtobufGeom[Point] {
    def fromCommands(cmds: Seq[Command]): Point = cmds match {
      case MoveTo(ps) +: Nil if ps.length == 1 => {
        val (x, y): (Int, Int) = expand(ps).head

        Point(x.toDouble, y.toDouble)
      }
      case _ => throw IncompatibleCommandSequence(
        "A Point Feature must contain a single MoveTo command with a parameter count of 1."
      )
    }

    def toCommands(p: Point): Seq[Command] = ???
  }

  implicit val protoMultiPoint = new ProtobufGeom[MultiPoint] {
    def fromCommands(cmds: Seq[Command]): MultiPoint = cmds match {
      case MoveTo(ps) +: Nil if ps.length > 0 => {
        MultiPoint(expand(ps).map({ case (x,y) => (x.toDouble, y.toDouble) }))
      }
      case _ => throw IncompatibleCommandSequence(
        "A MultiPoint Feature must contain a single MoveTo command with a parameter count > 0."
      )
    }

    def toCommands(mp: MultiPoint): Seq[Command] = ???
  }

  /*
  implicit class ProtobufLine(l: Line) extends ProtobufGeom[Line] {
    def fromCommands(cmds: Seq[Command]): Line = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufMultiLine(ml: MultiLine) extends ProtobufGeom[MultiLine] {
    def fromCommands(cmds: Seq[Command]): MultiLine = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufPolygon(p: Polygon) extends ProtobufGeom[Polygon] {
    def fromCommands(cmds: Seq[Command]): Polygon = ???

    def toCommands: Seq[Command] = ???
  }

  implicit class ProtobufMultiPolygon(mp: MultiPolygon) extends ProtobufGeom[MultiPolygon] {
    def fromCommands(cmds: Seq[Command]): MultiPolygon = ???

    def toCommands: Seq[Command] = ???
  }
  */
}
