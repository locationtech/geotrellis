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

import scala.collection.mutable.ListBuffer


// --- //

/** VectorTile geometries are stored as packed lists of "Command Integers".
  * There are currently three legal Commands: MoveTo, LineTo, and ClosePath.
  * Each adhere to the following format:
  *
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
  * The "cursor" is the location of the current vertex being considered,
  * and it starts at (0,0) for each Feature. As MoveTo and LineTo commands
  * are read, the cursor moves according the list of parsed delta pairs.
  * ClosePath does not move the cursor, but may in future versions of the spec.
  *
  * Caveats:
  * - Point features, whether single or multi, will always consist of a single MoveTo.
  * - Any Polygon in a Polygon feature must have a LineTo with a count of at least 2.
  * - ClosePath must always have a parameter count of 1.
  */
sealed trait Command

case class MoveTo(deltas: Array[(Int,Int)]) extends Command

case class LineTo(deltas: Array[(Int,Int)]) extends Command

case object ClosePath extends Command

case class InvalidCommand(id: Int, count: Int) extends Exception

object Command {
  /** Attempt to parse a list of Command/Parameter Integers. */
  def commands(cmds: Array[Int]): Array[Command] = {
    def work(cmds: Array[Int]): ListBuffer[Command] = cmds match {
      case Array() => new ListBuffer[Command]
      case ns => parseCmd(ns.head) match {
        case (1,count) => {
          val (ps,rest) = ns.tail.splitAt(count * 2)
          val res = new Array[(Int,Int)](count)
          var i = 0

          while(i < count) {
            res.update(i, (unzig(ps(i*2)), unzig(ps(i*2+1))))

            i += 1
          }

          MoveTo(res) +=: work(rest)
        }
        case (2,count) => {
          val (ps,rest) = ns.tail.splitAt(count * 2)
          val res = new Array[(Int,Int)](count)
          var i = 0

          while(i < count) {
            res.update(i, (unzig(ps(i*2)), unzig(ps(i*2+1))))

            i += 1
          }

          LineTo(res) +=: work(rest)
        }
        case (7,_) => ClosePath +=: work(ns.tail)
      }
    }

    work(cmds).toArray
  }

  /** Convert a list of parsed Commands back into their original Command
    * and Z-encoded Parameter Integer forms.
    */
  def uncommands(cmds: Array[Command]): Array[Int] = {
    cmds.flatMap({
      case MoveTo(ds) => unparseCmd(1, ds.length) +=: params(ds)
      case LineTo(ds) => unparseCmd(2, ds.length) +=: params(ds)
      case ClosePath  => Array(unparseCmd(7, 1))
    })
  }

  /** Divide a Command Integer into its Command ID and parameter count. */
  private def parseCmd(n: Int): (Int,Int) = {
    val cmd = n & 7
    val count = n >> 3  // or >> ?

    cmd match {
      case 1 if count > 0 => (cmd,count)
      case 2 if count > 0 => (cmd,count)
      case 7 if count == 1 => (cmd,count)
      case _ => throw InvalidCommand(cmd, count)
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
