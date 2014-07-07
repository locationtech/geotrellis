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

import scala.collection.mutable.ListBuffer

import geotrellis.io.geotiff.decompression._
import geotrellis.io.geotiff._

import geotrellis.io.geotiff.decompression.HuffmanColor._
import geotrellis.io.geotiff.decompression.HuffmanCodeTree._

object HuffmanDecompression {

  implicit class Huffman(matrix: Vector[Vector[Byte]]) {

    def uncompressHuffman(): Vector[Byte] =
      matrix.map(row => transform(decodeSegment(row, valueRoot),
        Nil)).flatten.toVector

    private def decodeSegment(segment: Vector[Byte], start: Node): List[Value] =
      segment match {
      case (byte +: bs) => {
        var buf = ListBuffer[Value]()
        var current = start

        for (i <- 7 to 0 by -1) {

          val next = if ((byte & (1 << i)) != 0) current.right else
            current.left

          next match {
            case Some(node) => {
              current = node
            }
            case None => current.value match {
              case Some(value) => {
                buf += value
                current = valueRoot
              }
              case None => //might be wrong
                throw new MalformedGeoTiffException("bad huffman encoding")
            }
          }
        }

        buf.toList ::: decodeSegment(bs, current)
      }
      case Vector() => Nil
    }

    def transform(list: List[Value], stack: List[Value]): List[Byte] =
      list match {
        case (x :: xs) => if (x.terminating) valueStackToByteList(x ::
            stack, None) ::: transform(xs, Nil)
        else transform(xs, x :: stack)
        case Nil => Nil
      }

    def valueStackToByteList(stack: List[Value],
      optColor: Option[HuffmanColor]): List[Byte] =
      stack match {
        case (x :: xs) => {
          val color = optColor getOrElse x.color
          val byteKind = if (color == Black) 255.toByte else 0.toByte //should write 0 or 1 bits (bitset)
          val segment = (for (i <- 0 until x.value) yield (byteKind)).toList
          segment ::: valueStackToByteList(stack, Some(color))
        }
        case Nil => Nil
      }
  }
}
