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
import geotrellis.vectortile.internal.ProtobufGeom
import geotrellis.vectortile.internal.{vector_tile => vt}

import org.locationtech.jts.geom.LineString
import java.lang.IllegalArgumentException
import scala.annotation.tailrec
import scala.collection.mutable.ListBuffer

// --- //

/** Users need not concern themselves with this subpackage; it handles the
  * details internal to the VectorTile encoding/decoding process.
  */
package object internal {

  /** If an sequence of Commands is given that does not conform to what the
    * Point, LineString, and Polygon decoders expect.
    */
  private[vectortile] case class CommandSequenceError(message: String) extends Exception(message)

  /** If some invalid combination of command id and parameter count are given. */
  private[vectortile] case class CommandError(id: Int, count: Int) extends Exception(s"ID: ${id}, COUNT: ${count}")

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

        points += here
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

  /**
    * Translate coordinates in VectorTile grid space into real CRS coordinates.
    *
    * @param point      A point in a VectorTile geometry, in grid coordinates.
    * @param topLeft    The location in the current CRS of the top-left corner of this Tile.
    * @param resolution How much of the CRS's units are covered by a single VT grid coordinate.
    * @return The [[Point]] projected into the CRS.
    *
    * ===Translation Logic===
    * Extents always exist in ''some'' CRS. Below is information for
    * determining values related to translating fixed VectorTile grid
    * coordinates into map coordinates within the implied CRS.
    *
    * You can find resolution this way. Let:
    * {{{
    * G := Height of grid (# number of cell rows)
    * T := Height of a Tile (default 4096)
    * E := Height of the Extent
    * }}}
    *
    * Then the resolution {{{ R = E / (G * T) }}}
    *
    * This actually allows same-Tile Layers with different integer `extent` values
    * to make sense!
    *
    * '''Finding the top-left corner'''
    * {{{
    * X = xmin + (R * T * SpatialKeyX)
    * Y = ymax - (R * T * SpatialKeyY)
    * }}}
    *
    * Resolution and the top-left corner only need to be calculated once per Layer.
    *
    * '''Shifting the VT grid points'''
    * {{{
    * vtX = X + (R * tileX)
    * vtY = Y - (R * tileY)
    * }}}
    *
    */
  private[vectortile] def toProjection(point: (Int, Int), topLeft: Point, resolution: Double): Point = {
    Point(
      topLeft.x + (resolution * point._1),
      topLeft.y - (resolution * point._2)
    )
  }

  /**
    * Translate [[Point]] coordinates within a CRS to those of a fixed
    * VectorTile grid. The reverse of [[toProjection]].
    *
    * @param point The [[Point]] in CRS space.
    * @param topLeft The CRS coordinates of the top-left corner of this Tile.
    * @param resolution How much of the CRS's units are covered by a single VT grid coordinate.
    * @return Grid coordinates in VectorTile space.
    */
  private[vectortile] def fromProjection(point: Point, topLeft: Point, resolution: Double): (Int, Int) = {
    (
      ((point.x - topLeft.x) / resolution).toInt,
      ((topLeft.y - point.y) / resolution).toInt
    )
  }

  /** Instance definition of the ProtobufGeom typeclass for Points. */
  private[vectortile] implicit val protoPoint = new ProtobufGeom[Point, MultiPoint] {
    def fromCommands(
      cmds: Seq[Command],
      topLeft: Point,
      resolution: Double
    ): Either[Point, MultiPoint] = cmds match {
      case MoveTo(ps) +: Nil => {
        val points = expand(ps).map(p => toProjection(p, topLeft, resolution))

        if (points.length == 1) Left(points.head) else Right(MultiPoint(points))
      }
      case _ => throw CommandSequenceError("Expected: [ MoveTo(ps) ]")
    }

    def toCommands(
      point: Either[Point, MultiPoint],
      topLeft: Point,
      resolution: Double
    ): Seq[Command] = point match {
      case Left(p) => Seq(MoveTo(Array(fromProjection(p, topLeft, resolution))))
      case Right(mp) => Seq(MoveTo(
        collapse(mp.points.map(p => fromProjection(p, topLeft, resolution)))
      ))
    }
  }

  /** Instance definition of the ProtobufGeom typeclass for Lines. */
  private[vectortile] implicit val protoLine = new ProtobufGeom[Line, MultiLine] {
    def fromCommands(
      cmds: Seq[Command],
      topLeft: Point,
      resolution: Double
    ): Either[Line, MultiLine] = {
      @tailrec def work(cs: Seq[Command], lines: ListBuffer[Line], cursor: (Int, Int)): ListBuffer[Line] = cs match {
        case MoveTo(p) +: LineTo(ps) +: rest => {
          val points = expand(p ++ ps, cursor)
          val nextCursor: (Int, Int) = points.last
          val line = Line(points.map(p => toProjection(p, topLeft, resolution)))

          work(rest, lines += line, nextCursor)
        }
        case Nil => lines
        case _ => throw CommandSequenceError("Expected: [ MoveTo(p +: Nil), LineTo(ps), ... ]")
      }

      val lines = work(cmds, new ListBuffer[Line], (0, 0))

      if (lines.length == 1) Left(lines.head) else Right(MultiLine(lines))
    }

    def toCommands(
      line: Either[Line, MultiLine],
      topLeft: Point,
      resolution: Double
    ): Seq[Command] = {
      def work(lines: Array[Line]): Seq[Command] = {
        var curs: (Int, Int) = (0, 0)
        var buff = new ListBuffer[Command]

        lines.foreach({l =>
          val diffs: Array[(Int, Int)] = collapse(
            l.points.map(p => fromProjection(p, topLeft, resolution)),
            curs
          )

          /* Find new cursor position */
          curs = fromProjection(l.last, topLeft, resolution)

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

  /** Instance definition of the ProtobufGeom typeclass for Polygons. */
  private[vectortile] implicit val protoPolygon = new ProtobufGeom[Polygon, MultiPolygon] {
    def fromCommands(
      cmds: Seq[Command],
      topLeft: Point,
      resolution: Double
    ): Either[Polygon, MultiPolygon] = {

      @tailrec def work(
        cs: Seq[Command],
        lines: ListBuffer[ListBuffer[(Int,Int)]],
        cursor: (Int, Int)
      ): ListBuffer[ListBuffer[(Int,Int)]] = cs match {
        case MoveTo(p) +: LineTo(ps) +: ClosePath +: rest => {
          /* `ClosePath` does not move the cursor, so we have to be
           * clever about how we manage the cursor and the closing point
           * of the Polygon.
           */
          val here: (Int, Int) = (p.head._1 + cursor._1, p.head._2 + cursor._2)
          val points = expand(p ++ ps, cursor)
          val nextCursor: (Int, Int) = points.last

          /* Add the starting point to close the Line into a Polygon */
          points += here

          work(rest, lines += points, nextCursor)
        }
        case Nil => lines
        case _ => throw CommandSequenceError("Expected: [MoveTo(p +: Nil), LineTo(ps), ClosePath, ... ]")
      }

      /* Collect all rings, whether external or internal */
      val lines: ListBuffer[ListBuffer[(Int, Int)]] = work(
        cmds,
        new ListBuffer[ListBuffer[(Int,Int)]],
        (0, 0)
      )

      /* Translate a [[Line]] to CRS coordinates */
      def tr(line: ListBuffer[(Int, Int)]): Line =
        Line(line.map(p => toProjection(p, topLeft, resolution)))

      /* Process interior rings */
      var polys = new ListBuffer[Polygon]
      var currL: ListBuffer[(Int,Int)] = lines.head
      var holes = new ListBuffer[Line]

      lines.tail.foreach({ line =>
        val area = surveyor(line)

        if (area < 0) { /* New Interior Rings */
          holes += tr(line)
        } else { /* New Exterior Ring */
          /* Save the current state */
          polys += Polygon(tr(currL), holes)

          /* Reset the state */
          currL = line
          holes = new ListBuffer[Line]
        }
      })

      /* Save the final state */
      polys += Polygon(tr(currL), holes)

      if (polys.length == 1) Left(polys.head) else Right(MultiPolygon(polys))
    }

    def toCommands(
      poly: Either[Polygon, MultiPolygon],
      topLeft: Point,
      resolution: Double
    ): Seq[Command] = {
      def work(polys: Array[Line]): Seq[Command] = {
        var curs: (Int, Int) = (0, 0)
        var buff = new ListBuffer[Command]

        polys.foreach({ l =>
          /* Exclude the final point via `init` */
          val diffs = collapse(
            l.points.init.map(p => fromProjection(p, topLeft, resolution)),
            curs
          )

          /* Find new cursor position */
          curs = fromProjection(l.points.init.last, topLeft, resolution)

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
  private[vectortile] def surveyor(l: ListBuffer[(Int, Int)]): Double = {
    val ps: ListBuffer[(Int, Int)] = l.init
    val xs = ps.map(_._1)
    val yns = (ps :+ ps.head).tail.map(_._2)
    val yps = (ps.last +: ps).init.map(_._2)

    var sum: Double = 0
    var i: Int = 0

    while (i < ps.length) {
      sum += xs(i) * (yns(i) - yps(i))

      i += 1
    }

    sum
  }

  /** Automatically convert mid-level Protobuf Values into a high-level [[Value]]. */
  private[vectortile] implicit def protoVal(value: vt.Tile.Value): Value = {
    if (value.stringValue.isDefined) {
      VString(value.stringValue.get)
    } else if (value.floatValue.isDefined) {
      VFloat(value.floatValue.get)
    } else if (value.doubleValue.isDefined) {
      VDouble(value.doubleValue.get)
    } else if (value.intValue.isDefined) {
      VInt64(value.intValue.get)
    } else if (value.uintValue.isDefined) {
      VWord64(value.uintValue.get)
    } else if (value.sintValue.isDefined) {
      VSint64(value.sintValue.get)
    } else if (value.boolValue.isDefined) {
      VBool(value.boolValue.get)
    } else {
      throw new IllegalArgumentException("No legal Protobuf Value given.")
    }
  }
}
