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

import monocle.syntax._
import monocle.Macro._

import geotrellis.io.geotiff._
import geotrellis.io.geotiff.ImageDirectoryLenses._

import geotrellis.io.geotiff.decompression._

import geotrellis.io.geotiff.decompression.ModeCode._
import geotrellis.io.geotiff.decompression.HuffmanColor._
import geotrellis.io.geotiff.decompression.HuffmanCodeTree._

import geotrellis.io.geotiff.decompression.GroupFourState._
import geotrellis.io.geotiff.decompression.GroupFourStateLenses._

case class T6Options(t6Options: Int = 0) {
  if ((t6Options & (1 << 0)) != 0) throw new MalformedGeoTiffException(
    "bit 0 in T6Options must be 0"
  )

  val uncompressedMode = (t6Options & (1 << 1)) != 0
}

object GroupFourDecompression {

  implicit class GroupFour(matrix: Vector[Vector[Byte]]) {

    def uncompressGroupFour(implicit directory: ImageDirectory): Vector[Byte] = {
      implicit val t6Options = directory |-> t6OptionsLens get match {
        case Some(t6OptionsInt) => T6Options(t6OptionsInt)
        case None => throw new MalformedGeoTiffException("no T6Options")
      }

      matrix.zipWithIndex./*par.*/map{ case(segment, i) =>
        uncompressGroupFourSegment(segment, i) }.flatten.toVector
    }

    //Always 2d coding, each segment encoded seperately, all white line first
    private def uncompressGroupFourSegment(segment: Vector[Byte], index: Int)
      (implicit t6Options: T6Options, directory: ImageDirectory) = {

      val bitSet = BitSet.valueOf(segment.toArray)

      val length = directory.rowsInSegment(index)
      val width = directory.rowSize

      val resArray = Array.ofDim[Byte](width * length)

      var resArrayIndex = 0
      var bitSetIndex = 0

      var runLength = 0

      var currentB1 = 0
      var currentA0 = 0

      var switchRefIndex = 1
      val switchRefArray = Array.ofDim[Int](width + 1)
      switchRefArray(0) = width
      var switchCurrentIndex = 0
      val switchCurrentArray = Array.ofDim[Int](width + 1)

      var isWhite = true

      var colorInverted = false

      val white: Byte = if (colorInverted) 0 else 255.toByte
      val black: Byte = if (colorInverted) 255.toByte else 0

      def resetIfNeeded() = if (currentA0 >= width) {
        val tmp = switchRefArray
        Array.copy(switchCurrentArray, 0, switchRefArray, 0, width)
        Array.copy(tmp, 0, switchCurrentArray, 0, width)

        for (i <- switchCurrentIndex until width) switchRefArray(i) = 0
        for (i <- 0 until width) switchCurrentArray(i) = 0

        runLength = 0
        currentA0 = 0
        currentB1 = switchRefArray(0)
        switchRefIndex = 1
        switchCurrentIndex = 0

        isWhite = true
      }

      def detectB1() = if (switchCurrentIndex != 0) {
        while (currentB1 <= currentA0 && currentB1 < width) {
          val r = switchRefArray(switchRefIndex) + switchRefArray(
            switchRefIndex)

          if (r == 0) currentB1 = width
          currentB1 += r
          if (switchRefIndex + 2 < switchRefArray.size) switchRefIndex += 2
        }
      }

      def decodePass() {
        detectB1()

        currentB1 += switchRefArray(switchRefIndex)
        switchRefIndex += 1

        runLength += currentB1 - currentA0

        currentA0 = currentB1

        currentB1 += switchRefArray(switchRefIndex)
        switchRefIndex += 1
      }

      def decodeHorizontal() {
        val color = if (colorInverted) if (isWhite) Black else White
        else if (isWhite) White else Black
        val (value, nextBitSetIndex) = getNextValue(bitSet, bitSetIndex, color)
        bitSetIndex = nextBitSetIndex
        isWhite = !isWhite

        addRun(value.value)
      }

      def addRun(length: Int) {
        val color = if (isWhite) white else black
        for (i <- 0 until length + runLength)
          resArray(resArrayIndex + i) = color

        runLength = 0
        resArrayIndex += length
      }

      def handleVX(x: Int) {
        detectB1
        addRun(currentB1 - currentA0 + x)
        isWhite = !isWhite

       if (x >= 0) {
         currentB1 += switchRefArray(switchRefIndex)
         switchRefIndex += 1
       } else if (switchRefIndex > 0) {
         switchRefIndex -= 1
         currentB1 -= switchRefArray(switchRefIndex)
       }
      }

      def handleH() {
        decodeHorizontal
        decodeHorizontal
        detectB1
      }

      def handleV0() = handleVX(0)
      def handleVR1() = handleVX(1)
      def handleVR2() = handleVX(2)
      def handleVR3() = handleVX(3)
      def handleVL1() = handleVX(-1)
      def handleVL2() = handleVX(-2)
      def handleVL3() = handleVX(-3)

      while (resArrayIndex < resArray.size) {
        val (mode, nextBitSetIndex) = getMode(bitSet, bitSetIndex)
        bitSetIndex = nextBitSetIndex

        mode match {
          case P => decodePass; println("P")
          case H => handleH; println("H")
          case V0 => handleV0; println("V0")
          case VR1 => handleVR1; println("VR1")
          case VR2 => handleVR2; println("VR2")
          case VR3 => handleVR3; println("VR3")
          case VL1 => handleVL1; println("VL1")
          case VL2 => handleVL2; println("VL2")
          case VL3 => handleVL3; println("VL3")
          case EOL => println("EOL REACHED")
          case _ => println("Mode code unknown: " + mode)
        }

        resetIfNeeded
      }

      resArray.drop(255)
    }

  }

}
