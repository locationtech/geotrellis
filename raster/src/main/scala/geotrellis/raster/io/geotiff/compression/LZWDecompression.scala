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

package geotrellis.raster.io.geotiff.compression

import geotrellis.raster.io.geotiff.reader._
import geotrellis.raster.io.geotiff.tags._

import monocle.syntax._

import scala.collection.immutable.HashMap

import java.util.BitSet

import spire.syntax.cfor._

object LZWDecompressor {
  def apply(segmentSizes: Array[Int]): LZWDecompressor =
    new LZWDecompressor(segmentSizes)
}

class TokenTableEntry(val firstByte: Byte, val thisByte: Byte, val length: Int, val prev: TokenTableEntry = null) {
  def concat(nextByte: Byte): TokenTableEntry =
    new TokenTableEntry(firstByte, nextByte, length + 1, this)
}

object TokenTable {
  val tableLimit = 4096

  def initial(): Array[TokenTableEntry]= {
    val arr = Array.ofDim[TokenTableEntry](tableLimit)
    cfor(0)(_ < 256, _ + 1) { i =>
      arr(i) = new TokenTableEntry(i.toByte, i.toByte, 1)
    }

    arr
  }

  def writeToOutput(entry: TokenTableEntry, outputArray: Array[Byte], outputArrayIndex: Int): Int = {
    var curr = entry
    while(curr != null) {
      outputArray(outputArrayIndex + curr.length - 1) = curr.thisByte
      curr = curr.prev
    }
    entry.length + outputArrayIndex
  }
}


class LZWDecompressor(segmentSizes: Array[Int]) extends Decompressor {
  val tableLimit = 4096

  val ClearCode = 256
  val EoICode = 257

  def decompress(segment: Array[Byte], segmentIndex: Int): Array[Byte] = {
    val bis = new LZWBitInputStream(segment)

    var tokenTable = TokenTable.initial
    var tokenTableIndex = 258

    var outputArrayIndex = 0
    val size = segmentSizes(segmentIndex)
    val outputArray = Array.ofDim[Byte](size)

    var threshold = 9

    def initializeTokenTable = {
      tokenTable = TokenTable.initial
      tokenTableIndex = 258
      threshold = 9
    }

    def addEntry(entry: TokenTableEntry): Unit = {
      tokenTable(tokenTableIndex) = entry
      tokenTableIndex += 1

      if (tokenTableIndex == 511) threshold = 10
      if (tokenTableIndex == 1023) threshold = 11
      if (tokenTableIndex == 2047) threshold = 12
    }

    var code = 0
    var oldCode = 0

    var break = false
    while (!break && { code = bis.get(threshold); code != EoICode } && outputArrayIndex < size) {
      if (code == ClearCode) {
        initializeTokenTable
        code = bis.get(threshold)

        if (code == EoICode) {
          break = true
        } else {
          outputArrayIndex = 
            TokenTable.writeToOutput(tokenTable(code), outputArray, outputArrayIndex)
        }
      } else if (code < tokenTableIndex) {
        // if the code is in the table
        val entry = tokenTable(code)
        outputArrayIndex =
          TokenTable.writeToOutput(entry, outputArray, outputArrayIndex)

        val oldEntry = tokenTable(oldCode)
        addEntry(oldEntry.concat(entry.firstByte))

      } else {
        val oldEntry = tokenTable(oldCode)
        val newEntry = oldEntry.concat(oldEntry.firstByte)
        outputArrayIndex =
          TokenTable.writeToOutput(newEntry, outputArray, outputArrayIndex)

        addEntry(newEntry)

      }

      oldCode = code
    }

    outputArray
  }

  private class LZWBitInputStream(arr: Array[Byte]) {

    val len = arr.length

    private var index = 0

    def get(next: Int): Int = {
      if (next + index > len * 8)
        EoICode
      else {

        val start = index / 8
        val end = (index + next - 1) / 8
        val ebi = (index + next - 1) % 8

        val res =
          if (end - start == 2) {
            val c = ((arr(start) & 0xff) << 16) | ((arr(end - 1) & 0xff) << 8) | (arr(end) & 0xff)
            (c >> (7 - ebi)) & (0xffffff >> (24 - next))
          } else {
            val c = ((arr(start) & 0xff) << 8) | (arr(end) & 0xff)
            (c >> (7 - ebi)) & (0xffff >> (16 - next))
          }

        index += next

        res
      }
    }
  }
}
