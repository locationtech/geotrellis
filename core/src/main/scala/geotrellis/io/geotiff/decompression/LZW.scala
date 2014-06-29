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

import scala.collection.immutable.HashMap
import scala.util.control.Breaks._

import geotrellis.io.geotiff.decompression._
import geotrellis.io.geotiff._

object LZWDecompression {

  implicit class LZW(matrix: Vector[Vector[Byte]]) {

    val tableLimit = 4096

    val limitMap = HashMap[Int, Int](
      9 -> 512,
      10 -> 1024,
      11 -> 2048,
      12 -> 4096
    )

    val ClearCode = 256
    val EoICode = 257

    def uncompressLZW(): Vector[Byte] =
      matrix.map(uncompressLZWSegment(_)).flatten.toVector

    private def uncompressLZWSegment(segment: Vector[Byte]) = {
      var buf = Vector[Byte]()
      val bitSet = BitSet.valueOf(segment.toArray)

      var index = 0
      var threshold = 9

      def getNextCode = {
        val res = (for {
          i <- index until (threshold + index)
          if bitSet.get(i)
        } yield (1 << (i))).sum

        index += threshold

        res
      }

      val stringTable = Array.fill[Option[Vector[Byte]]](tableLimit) { None }
      var stringTableIndex = 258

      def clearTable = for (i <- 0 until tableLimit)
        if (i < 256) stringTable(i) = Some(Vector(i.toByte))
        else stringTable(i) = None

      def addString(string: Vector[Byte]) = {
        stringTable(stringTableIndex) = Some(string)
        stringTableIndex += 1
        if (stringTableIndex >= limitMap(threshold)) threshold += 1
      }

      clearTable

      var limit = limitMap(threshold)

      var code = 0
      var oldCode = 0
      index += threshold

      breakable {
        while ({code = getNextCode; code != EoICode}) {
          if (code == ClearCode) {
            clearTable
            code = getNextCode
            if (code == EoICode) break
            else buf = buf ++ stringTable(code).get
          } else {
            stringTable(code) match {
              case Some(out) => {
                buf = buf ++ out
                addString(stringTable(oldCode).get :+ out(0))
              }
              case None => {
                val out = stringTable(oldCode).get :+ stringTable(oldCode).
                  get(0)
                buf = buf ++ out
                addString(out)
              }
            }
          }

          oldCode = code
        }
      }

      buf
    }

  }

}
