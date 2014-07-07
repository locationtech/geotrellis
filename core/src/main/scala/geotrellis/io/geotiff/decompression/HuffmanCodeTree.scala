/*
 * Copyright (c) 2014 Azavea.
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

package geotrellis.io.geotiff.decompression

import java.util.BitSet

import geotrellis.io.geotiff._

object HuffmanColor extends Enumeration {

  type HuffmanColor = Value

  val Black, White, Both = Value

}

import HuffmanColor._

case class Value(value: Int, color: HuffmanColor, terminating: Boolean)

object Node {

  val empty = Node(None, None, None)

}

case class Node(left: Option[Node], right: Option[Node],
  value: Option[Value]) {

  import Node._

  def add(sequence: List[Int], seqValue: Value): Node = {
    sequence match {
      case 0 :: xs =>
        Node(Some(left.getOrElse(empty).add(xs, seqValue)), right, value)
      case 1 :: xs =>
        Node(left, Some(right.getOrElse(empty).add(xs, seqValue)), value)
      case _ => Node(left, right, Some(seqValue))
    }
  }

}

object ModeCode {

  val P = 1
  val H = 2
  val V0 = 3
  val VR1 = 4
  val VR2 = 5
  val VR3 = 6
  val VL1 = 7
  val VL2 = 8
  val VL3 = 9
  val EXT2D = 10
  val EXT1D = 11
  val EOL = 12

}

object HuffmanCodeTree { //alternative => print the tree and then have it on
                         //compile time?!

  import ModeCode._

  val modeValues = List(
    (List(0, 0, 0, 1) -> Value(P, Both, true)),
    (List(0, 0, 1) -> Value(H, Both, true)),
    (List(1) -> Value(V0, Both, true)),
    (List(0, 1, 1) -> Value(VR1, Both, true)),
    (List(0, 0, 0, 0, 1, 1) -> Value(VR2, Both, true)),
    (List(0, 0, 0, 0, 0, 1, 1) -> Value(VR3, Both, true)),
    (List(0, 1, 0) -> Value(VL1, Both, true)),
    (List(0, 0, 0, 0, 1, 0) -> Value(VL2, Both, true)),
    (List(0, 0, 0, 0, 0, 1, 0) -> Value(VL3, Both, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 1) -> Value(EXT2D, Both, true)),
    (List(0, 0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1) -> Value(EXT1D, Both, true)),
    (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1) -> Value(EOL, Both, true))
  )

  val values = List(
    (List(0, 0, 1, 1, 0, 1, 0, 1) -> Value(0, White, true)),
    (List(0, 0, 0, 1, 1, 1) -> Value(1, White, true)),
    (List(0, 1, 1, 1) -> Value(2, White, true)),
    (List(1, 0, 0, 0) -> Value(3, White, true)),
    (List(1, 0, 1, 1) -> Value(4, White, true)),
    (List(1, 1, 0, 0) -> Value(5, White, true)),
    (List(1, 1, 1, 0) -> Value(6, White, true)),
    (List(1, 1, 1, 1) -> Value(7, White, true)),
    (List(1, 0, 0, 1, 1) -> Value(8, White, true)),
    (List(1, 0, 1, 0, 0) -> Value(9, White, true)),
    (List(0, 0, 1, 1, 1) -> Value(10, White, true)),
    (List(0, 1, 0, 0, 0) -> Value(11, White, true)),
    (List(0, 0, 1, 0, 0, 0) -> Value(12, White, true)),
    (List(0, 0, 0, 0, 1, 1) -> Value(13, White, true)),
    (List(1, 1, 0, 1, 0, 0) -> Value(14, White, true)),
    (List(1, 1, 0, 1, 0, 1) -> Value(15, White, true)),
    (List(1, 0, 1, 0, 1, 0) -> Value(16, White, true)),
    (List(1, 0, 1, 0, 1, 1) -> Value(17, White, true)),
    (List(0, 1, 0, 0, 1, 1, 1) -> Value(18, White, true)),
    (List(0, 0, 0, 1, 1, 0, 0) -> Value(19, White, true)),
    (List(0, 0, 0, 1, 0, 0, 0) -> Value(20, White, true)),
    (List(0, 0, 1, 0, 1, 1, 1) -> Value(21, White, true)),
    (List(0, 0, 0, 0, 0, 1, 1) -> Value(22, White, true)),
    (List(0, 0, 0, 0, 1, 0, 0) -> Value(23, White, true)),
    (List(0, 1, 0, 1, 0, 0, 0) -> Value(24, White, true)),
    (List(0, 1, 0, 1, 0, 1, 1) -> Value(25, White, true)),
    (List(0, 0, 1, 0, 0, 1, 1) -> Value(26, White, true)),
    (List(0, 1, 0, 0, 1, 0, 0) -> Value(27, White, true)),
    (List(0, 0, 1, 1, 0, 0, 0) -> Value(28, White, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0) -> Value(29, White, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 1) -> Value(30, White, true)),
    (List(0, 0, 0, 1, 1, 0, 1, 0) -> Value(31, White, true)),
    (List(0, 0, 0, 1, 1, 0, 1, 1) -> Value(32, White, true)),
    (List(0, 0, 0, 1, 0, 0, 1, 0) -> Value(33, White, true)),
    (List(0, 0, 0, 1, 0, 0, 1, 1) -> Value(34, White, true)),
    (List(0, 0, 0, 1, 0, 1, 0, 0) -> Value(35, White, true)),
    (List(0, 0, 0, 1, 0, 1, 0, 1) -> Value(36, White, true)),
    (List(0, 0, 0, 1, 0, 1, 1, 0) -> Value(37, White, true)),
    (List(0, 0, 0, 1, 0, 1, 1, 1) -> Value(38, White, true)),
    (List(0, 0, 1, 0, 1, 0, 0, 0) -> Value(39, White, true)),
    (List(0, 0, 1, 0, 1, 0, 0, 1) -> Value(40, White, true)),
    (List(0, 0, 1, 0, 1, 0, 1, 0) -> Value(41, White, true)),
    (List(0, 0, 1, 0, 1, 0, 1, 1) -> Value(42, White, true)),
    (List(0, 0, 1, 0, 1, 1, 0, 0) -> Value(43, White, true)),
    (List(0, 0, 1, 0, 1, 1, 0, 1) -> Value(44, White, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 0) -> Value(45, White, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1) -> Value(46, White, true)),
    (List(0, 0, 0, 0, 1, 0, 1, 0) -> Value(47, White, true)),
    (List(0, 0, 0, 0, 1, 0, 1, 1) -> Value(48, White, true)),
    (List(0, 1, 0, 1, 0, 0, 1, 0) -> Value(49, White, true)),
    (List(0, 1, 0, 1, 0, 0, 1, 1) -> Value(50, White, true)),
    (List(0, 1, 0, 1, 0, 1, 0, 0) -> Value(51, White, true)),
    (List(0, 1, 0, 1, 0, 1, 0, 1) -> Value(52, White, true)),
    (List(0, 0, 1, 0, 0, 1, 0, 0) -> Value(53, White, true)),
    (List(0, 0, 1, 0, 0, 1, 0, 1) -> Value(54, White, true)),
    (List(0, 1, 0, 1, 1, 0, 0, 0) -> Value(55, White, true)),
    (List(0, 1, 0, 1, 1, 0, 0, 1) -> Value(56, White, true)),
    (List(0, 1, 0, 1, 1, 0, 1, 0) -> Value(57, White, true)),
    (List(0, 1, 0, 1, 1, 0, 1, 1) -> Value(58, White, true)),
    (List(0, 1, 0, 0, 1, 0, 1, 0) -> Value(59, White, true)),
    (List(0, 1, 0, 0, 1, 0, 1, 1) -> Value(60, White, true)),
    (List(0, 0, 1, 1, 0, 0, 1, 0) -> Value(61, White, true)),
    (List(0, 0, 1, 1, 0, 0, 1, 1) -> Value(62, White, true)),
    (List(0, 0, 1, 1, 0, 1, 0, 0) -> Value(63, White, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 1, 1) -> Value(0, Black, true)),
    (List(0, 1, 0) -> Value(1, Black, true)),
    (List(1, 1) -> Value(2, Black, true)),
    (List(1, 0) -> Value(3, Black, true)),
    (List(0, 1, 1) -> Value(4, Black, true)),
    (List(0, 0, 1, 1) -> Value(5, Black, true)),
    (List(0, 0, 1, 0) -> Value(6, Black, true)),
    (List(0, 0, 0, 1, 1) -> Value(7, Black, true)),
    (List(0, 0, 0, 1, 0, 1) -> Value(8, Black, true)),
    (List(0, 0, 0, 1, 0, 0) -> Value(9, Black, true)),
    (List(0, 0, 0, 0, 1, 0, 0) -> Value(10, Black, true)),
    (List(0, 0, 0, 0, 1, 0, 1) -> Value(11, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 1) -> Value(12, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 0) -> Value(13, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 1) -> Value(14, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 0) -> Value(15, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 1, 1) -> Value(16, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 0, 0) -> Value(17, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 0) -> Value(18, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1) -> Value(19, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0) -> Value(20, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0) -> Value(21, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1) -> Value(22, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0) -> Value(23, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1) -> Value(24, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 0) -> Value(25, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 0) -> Value(26, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1, 1) -> Value(27, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 0) -> Value(28, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0, 1) -> Value(29, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 0) -> Value(30, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1) -> Value(31, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0) -> Value(32, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1) -> Value(33, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 0) -> Value(34, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 0, 1, 1) -> Value(35, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 0) -> Value(36, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 0, 1) -> Value(37, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 0) -> Value(38, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 0, 1, 1, 1) -> Value(39, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0) -> Value(40, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1) -> Value(41, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 0) -> Value(42, Black, true)),
    (List(0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1, 1) -> Value(43, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0) -> Value(44, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1) -> Value(45, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 0) -> Value(46, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1, 1) -> Value(47, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0) -> Value(48, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1) -> Value(49, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0) -> Value(50, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1) -> Value(51, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 0) -> Value(52, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 1) -> Value(53, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 0) -> Value(54, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 1) -> Value(55, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0) -> Value(56, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 0) -> Value(57, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0, 1) -> Value(58, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 1) -> Value(59, Black, true)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 0) -> Value(60, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0) -> Value(61, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 0) -> Value(62, Black, true)),
    (List(0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1, 1) -> Value(63, Black, true)),
    (List(1, 1, 0, 1, 1) -> Value(64, White, false)),
    (List(1, 0, 0, 1, 0) -> Value(128, White, false)),
    (List(0, 1, 0, 1, 1, 1) -> Value(192, White, false)),
    (List(0, 1, 1, 0, 1, 1, 1) -> Value(256, White, false)),
    (List(0, 0, 1, 1, 0, 1, 1, 0) -> Value(320, White, false)),
    (List(0, 0, 1, 1, 0, 1, 1, 1) -> Value(384, White, false)),
    (List(0, 1, 1, 0, 0, 1, 0, 0) -> Value(448, White, false)),
    (List(0, 1, 1, 0, 0, 1, 0, 1) -> Value(512, White, false)),
    (List(0, 1, 1, 0, 1, 0, 0, 0) -> Value(576, White, false)),
    (List(0, 1, 1, 0, 0, 1, 1, 1) -> Value(640, White, false)),
    (List(0, 1, 1, 0, 0, 1, 1, 0, 0) -> Value(704, White, false)),
    (List(0, 1, 1, 0, 0, 1, 1, 0, 1) -> Value(768, White, false)),
    (List(0, 1, 1, 0, 1, 0, 0, 1, 0) -> Value(832, White, false)),
    (List(0, 1, 1, 0, 1, 0, 0, 1, 1) -> Value(896, White, false)),
    (List(0, 1, 1, 0, 1, 0, 1, 0, 0) -> Value(960, White, false)),
    (List(0, 1, 1, 0, 1, 0, 1, 0, 1) -> Value(1024, White, false)),
    (List(0, 1, 1, 0, 1, 0, 1, 1, 0) -> Value(1088, White, false)),
    (List(0, 1, 1, 0, 1, 0, 1, 1, 1) -> Value(1152, White, false)),
    (List(0, 1, 1, 0, 1, 1, 0, 0, 0) -> Value(1216, White, false)),
    (List(0, 1, 1, 0, 1, 1, 0, 0, 1) -> Value(1280, White, false)),
    (List(0, 1, 1, 0, 1, 1, 0, 1, 0) -> Value(1344, White, false)),
    (List(0, 1, 1, 0, 1, 1, 0, 1, 1) -> Value(1408, White, false)),
    (List(0, 1, 0, 0, 1, 1, 0, 0, 0) -> Value(1472, White, false)),
    (List(0, 1, 0, 0, 1, 1, 0, 0, 1) -> Value(1536, White, false)),
    (List(0, 1, 0, 0, 1, 1, 0, 1, 0) -> Value(1600, White, false)),
    (List(0, 1, 1, 0, 0, 0) -> Value(1664, White, false)),
    (List(0, 1, 0, 0, 1, 1, 0, 1, 1) -> Value(1728, White, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1) -> Value(-1, White, false)), //WHAT IS THIS?
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 1) -> Value(64, Black, false)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 0) -> Value(128, Black, false)),
    (List(0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0, 1) -> Value(192, Black, false)),
    (List(0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1) -> Value(256, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 1) -> Value(320, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 0) -> Value(384, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 0, 1) -> Value(448, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 0) -> Value(512, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 1, 1, 0, 1) -> Value(576, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 0) -> Value(640, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0, 1, 1) -> Value(704, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 0) -> Value(768, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1, 0, 1) -> Value(832, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 0) -> Value(896, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0, 1, 1) -> Value(960, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 0) -> Value(1024, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 0, 1) -> Value(1088, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 0) -> Value(1152, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1, 1, 1) -> Value(1216, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 0) -> Value(1280, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 1, 1) -> Value(1344, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 0) -> Value(1408, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1, 0, 1) -> Value(1472, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 0) -> Value(1536, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0, 1, 1) -> Value(1600, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 0) -> Value(1664, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 1, 1, 0, 0, 1, 0, 1) -> Value(1728, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0) -> Value(-1, Black, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0) -> Value(1792, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 0) -> Value(1856, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 0, 1) -> Value(1920, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 0) -> Value(1984, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 1, 1) -> Value(2048, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0) -> Value(2112, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 0, 1) -> Value(2176, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 0) -> Value(2240, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 0, 1, 1, 1) -> Value(2304, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 0) -> Value(2368, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 0, 1) -> Value(2432, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 0) -> Value(2496, Both, false)),
    (List(0, 0, 0, 0, 0, 0, 0, 1, 1, 1, 1, 1) -> Value(2560, Both, false))
  )

  import Node._

  private def iterate(codeTreeValues: List[(List[Int], Value)]): Node
  = codeTreeValues match {
    case x :: xs => iterate(xs).add(x._1, x._2)
    case Nil => empty
  }

  lazy val modeRoot = iterate(modeValues)

  def getMode(bitSet: BitSet, index: Int): (Int, Int) = {
    val res = getNextValue(modeRoot, bitSet, index, Both)
    (res._1.value, res._2)
  }

  lazy val valueRoot = iterate(values)

  def getNextValue(bitSet: BitSet, index: Int, color: HuffmanColor):
      (Value, Int) = {

    def recurse(currentOption: Option[Value], index: Int): (Value, Int) = {
      val (nextValue, nextIndex) = getNextValue(valueRoot, bitSet, index, color)
      val result = currentOption match {
        case Some(current) => Value(
          current.value + nextValue.value,
          if (current.color == Both) nextValue.color else current.color,
          nextValue.terminating
        )
        case None => nextValue
      }

      if (result.terminating) (result, nextIndex)
      else recurse(Some(result), nextIndex)
    }

    recurse(None, index)
  }

  private def getNextValue(current: Node, bitSet: BitSet, index: Int,
    color: HuffmanColor):  (Value, Int) = {
    current.value match {
      case Some(value) if (value.color == color) => (value, index)
      case _ => (if (bitSet.get(index)) current.right else current.left) match {
        case Some(next) => getNextValue(next, bitSet, index + 1, color)
        case None => throw new MalformedGeoTiffException(
          "bad code word for CCITT encoding"
        )
      }
    }
  }

}
