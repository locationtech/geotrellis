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

package geotrellis.vectortile.internal

import scala.collection.mutable.ListBuffer
import scala.annotation.tailrec

// --- //

/** VectorTile geometries are stored as packed lists of "Command Integers".
  * There are currently three legal Commands: MoveTo, LineTo, and ClosePath.
  * Each adhere to the following format:
  *
  * {{{
  * [ ... 00000 | 000 ]
  * -------------------
  *       |        |
  *       |        --- The "Command ID". MoveTo: 001 (1)
  *       |                              LineTo: 010 (2)
  *       |                              ClosePath: 111 (7)
  *       |
  *       --- The remaining 29 bits are the "Parameter Count".
  *           This indicates the number of _pairs_ of ints
  *           that follow that should be interpreted as Z-encoded
  *           deltas from the current "cursor".
  *
  * }}}
  * The "cursor" is the location of the current vertex being considered,
  * and it starts at (0,0) for each Feature. As MoveTo and LineTo commands
  * are read, the cursor moves according the list of parsed delta pairs.
  * ClosePath does not move the cursor, but may in future versions of the spec.
  *
  * Caveats:
  *  - Point features, whether single or multi, will always consist of a single MoveTo.
  *  - Any Polygon in a Polygon feature must have a LineTo with a count of at least 2.
  *  - ClosePath must always have a parameter count of 1.
  */
private[vectortile] sealed trait Command extends Serializable

/** `MoveTo` signals a series of moves from the current cursor (default of `(0,0)`).
  * The parameter pairs that follow don't represent a point to move to,
  * but instead are deltas from the current cursor.
  */
private[vectortile] case class MoveTo(deltas: Array[(Int,Int)]) extends Command

/** `LineTo` indicates that a LineString should be continued from the current
  * cursor.
  */
private[vectortile] case class LineTo(deltas: Array[(Int,Int)]) extends Command

/** Signals the end of a Polygon. Never has parameters, and doesn't move the cursor. */
private[vectortile] case object ClosePath extends Command

/** Contains convenience functions for handling [[Command]]s. */
private[vectortile] object Command {
  /** Attempt to parse a list of Command/Parameter Integers. */
  def commands(cmds: Seq[Int]): ListBuffer[Command] = {
    @tailrec def work(cmds: Seq[Int], curr: ListBuffer[Command]): ListBuffer[Command] = cmds match {
      case Nil => curr
      case ns => parseCmd(ns.head) match {
        case (1,count) => {
          val (ps,rest) = ns.tail.splitAt(count * 2)
          val res = new Array[(Int,Int)](count)
          var i = 0

          while(i < count) {
            res.update(i, (unzig(ps(i*2)), unzig(ps(i*2+1))))

            i += 1
          }

          work(rest, curr += MoveTo(res))
        }
        case (2,count) => {
          val (ps,rest) = ns.tail.splitAt(count * 2)
          val res = new Array[(Int,Int)](count)
          var i = 0

          while(i < count) {
            res.update(i, (unzig(ps(i*2)), unzig(ps(i*2+1))))

            i += 1
          }

          work(rest, curr += LineTo(res))
        }
        case (7,_) => work(ns.tail, curr += ClosePath)
      }
    }

    work(cmds, new ListBuffer[Command])
  }

  /** Convert a list of parsed Commands back into their original Command
    * and Z-encoded Parameter Integer forms.
    */
  def uncommands(cmds: Seq[Command]): Seq[Int] = {
    cmds.flatMap({
      case MoveTo(ds) => unparseCmd(1, ds.length) +=: params(ds)
      case LineTo(ds) => unparseCmd(2, ds.length) +=: params(ds)
      case ClosePath  => ListBuffer(unparseCmd(7, 1))
    }).toSeq
  }

  /** Divide a Command Integer into its Command ID and parameter count. */
  private def parseCmd(n: Int): (Int,Int) = {
    val cmd = n & 7
    val count = n >> 3

    cmd match {
      case 1 if count > 0 => (cmd,count)
      case 2 if count > 0 => (cmd,count)
      case 7 if count == 1 => (cmd,count)
      case _ => throw CommandError(cmd, count)
    }
  }

  /** Recombine a Command ID and parameter count into a Command Integer. */
  private def unparseCmd(cmd: Int, count: Int): Int = (cmd & 7) | (count << 3)

  /** Transform an array of point deltas into one of Z-encoded Parameter Ints. */
  private def params(ns: Array[(Int,Int)]): ListBuffer[Int] = {
    val res = new ListBuffer[Int]

    ns.foreach { case (dx,dy) => res.append(zig(dx), zig(dy)) }

    res
  }

  // (>>) is arithemic shift, (>>>) is logical shift.

  /** Z-encode a 32-bit Int. The result should always be positive. */
  def zig(n: Int): Int = (n << 1) ^ (n >> 31)

  /** Decode a Z-encoded Int back into its original form. */
  def unzig(n: Int): Int = (n >> 1) ^ (-(n & 1))
}
