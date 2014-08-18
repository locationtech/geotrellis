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

package geotrellis.raster.io.geotiff.reader.decompression

import geotrellis.raster.io.geotiff.reader.MalformedGeoTiffException
import geotrellis.raster.io.geotiff.reader.utils.ByteInverterUtils._

object TiffFaxDecompressor {

  def apply(fillOrder: Int, width: Int, height: Int) =
    new TiffFaxDecompressor(fillOrder, width, height)

  val table1: Array[Int] = Array(
    0x00,
    0x01,
    0x03,
    0x07,
    0x0f,
    0x1f,
    0x3f,
    0x7f,
    0xff
  )

  val table2: Array[Int] = Array(
    0x00,
    0x80,
    0xc0,
    0xe0,
    0xf0,
    0xf8,
    0xfc,
    0xfe,
    0xff
  )

  val flipTable = reverseTable //TODO

  val white: Array[Short] = Array(
    6430,   6400,   6400,   6400,   3225,   3225,   3225,   3225,
    944,    944,    944,    944,    976,    976,    976,    976,
    1456,   1456,   1456,   1456,   1488,   1488,   1488,   1488,
    718,    718,    718,    718,    718,    718,    718,    718,
    750,    750,    750,    750,    750,    750,    750,    750,
    1520,   1520,   1520,   1520,   1552,   1552,   1552,   1552,
    428,    428,    428,    428,    428,    428,    428,    428,
    428,    428,    428,    428,    428,    428,    428,    428,
    654,    654,    654,    654,    654,    654,    654,    654,
    1072,   1072,   1072,   1072,   1104,   1104,   1104,   1104,
    1136,   1136,   1136,   1136,   1168,   1168,   1168,   1168,
    1200,   1200,   1200,   1200,   1232,   1232,   1232,   1232,
    622,    622,    622,    622,    622,    622,    622,    622,
    1008,   1008,   1008,   1008,   1040,   1040,   1040,   1040,
    44,     44,     44,     44,     44,     44,     44,     44,
    44,     44,     44,     44,     44,     44,     44,     44,
    396,    396,    396,    396,    396,    396,    396,    396,
    396,    396,    396,    396,    396,    396,    396,    396,
    1712,   1712,   1712,   1712,   1744,   1744,   1744,   1744,
    846,    846,    846,    846,    846,    846,    846,    846,
    1264,   1264,   1264,   1264,   1296,   1296,   1296,   1296,
    1328,   1328,   1328,   1328,   1360,   1360,   1360,   1360,
    1392,   1392,   1392,   1392,   1424,   1424,   1424,   1424,
    686,    686,    686,    686,    686,    686,    686,    686,
    910,    910,    910,    910,    910,    910,    910,    910,
    1968,   1968,   1968,   1968,   2000,   2000,   2000,   2000,
    2032,   2032,   2032,   2032,     16,     16,     16,     16,
    10257,  10257,  10257,  10257,  12305,  12305,  12305,  12305,
    330,    330,    330,    330,    330,    330,    330,    330,
    330,    330,    330,    330,    330,    330,    330,    330,
    330,    330,    330,    330,    330,    330,    330,    330,
    330,    330,    330,    330,    330,    330,    330,    330,
    362,    362,    362,    362,    362,    362,    362,    362,
    362,    362,    362,    362,    362,    362,    362,    362,
    362,    362,    362,    362,    362,    362,    362,    362,
    362,    362,    362,    362,    362,    362,    362,    362,
    878,    878,    878,    878,    878,    878,    878,    878,
    1904,   1904,   1904,   1904,   1936,   1936,   1936,   1936,
    -18413, -18413, -16365, -16365, -14317, -14317, -10221, -10221,
    590,    590,    590,    590,    590,    590,    590,    590,
    782,    782,    782,    782,    782,    782,    782,    782,
    1584,   1584,   1584,   1584,   1616,   1616,   1616,   1616,
    1648,   1648,   1648,   1648,   1680,   1680,   1680,   1680,
    814,    814,    814,    814,    814,    814,    814,    814,
    1776,   1776,   1776,   1776,   1808,   1808,   1808,   1808,
    1840,   1840,   1840,   1840,   1872,   1872,   1872,   1872,
    6157,   6157,   6157,   6157,   6157,   6157,   6157,   6157,
    6157,   6157,   6157,   6157,   6157,   6157,   6157,   6157,
    -12275, -12275, -12275, -12275, -12275, -12275, -12275, -12275,
    -12275, -12275, -12275, -12275, -12275, -12275, -12275, -12275,
    14353,  14353,  14353,  14353,  16401,  16401,  16401,  16401,
    22547,  22547,  24595,  24595,  20497,  20497,  20497,  20497,
    18449,  18449,  18449,  18449,  26643,  26643,  28691,  28691,
    30739,  30739, -32749, -32749, -30701, -30701, -28653, -28653,
    -26605, -26605, -24557, -24557, -22509, -22509, -20461, -20461,
    8207,   8207,   8207,   8207,   8207,   8207,   8207,   8207,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    72,     72,     72,     72,     72,     72,     72,     72,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    104,    104,    104,    104,    104,    104,    104,    104,
    4107,   4107,   4107,   4107,   4107,   4107,   4107,   4107,
    4107,   4107,   4107,   4107,   4107,   4107,   4107,   4107,
    4107,   4107,   4107,   4107,   4107,   4107,   4107,   4107,
    4107,   4107,   4107,   4107,   4107,   4107,   4107,   4107,
    266,    266,    266,    266,    266,    266,    266,    266,
    266,    266,    266,    266,    266,    266,    266,    266,
    266,    266,    266,    266,    266,    266,    266,    266,
    266,    266,    266,    266,    266,    266,    266,    266,
    298,    298,    298,    298,    298,    298,    298,    298,
    298,    298,    298,    298,    298,    298,    298,    298,
    298,    298,    298,    298,    298,    298,    298,    298,
    298,    298,    298,    298,    298,    298,    298,    298,
    524,    524,    524,    524,    524,    524,    524,    524,
    524,    524,    524,    524,    524,    524,    524,    524,
    556,    556,    556,    556,    556,    556,    556,    556,
    556,    556,    556,    556,    556,    556,    556,    556,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    136,    136,    136,    136,    136,    136,    136,    136,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    168,    168,    168,    168,    168,    168,    168,    168,
    460,    460,    460,    460,    460,    460,    460,    460,
    460,    460,    460,    460,    460,    460,    460,    460,
    492,    492,    492,    492,    492,    492,    492,    492,
    492,    492,    492,    492,    492,    492,    492,    492,
    2059,   2059,   2059,   2059,   2059,   2059,   2059,   2059,
    2059,   2059,   2059,   2059,   2059,   2059,   2059,   2059,
    2059,   2059,   2059,   2059,   2059,   2059,   2059,   2059,
    2059,   2059,   2059,   2059,   2059,   2059,   2059,   2059,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    200,    200,    200,    200,    200,    200,    200,    200,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232,
    232,    232,    232,    232,    232,    232,    232,    232
  )

  val additionalMakeup: Array[Short] = Array(
    28679,         28679,         31752,         32777.toShort,
    33801.toShort, 34825.toShort, 35849.toShort, 36873.toShort,
    29703.toShort, 29703.toShort, 30727.toShort, 30727.toShort,
    37897.toShort, 38921.toShort, 39945.toShort, 40969.toShort
  )

  val initBlack: Array[Short] = Array(
    3226,  6412,    200,    168,    38,     38,    134,    134,
    100,    100,    100,    100,    68,     68,     68,     68
  )

  val twoBitBlack: Array[Short] = Array(292, 260, 226, 226)

  val black: Array[Short] = Array(
    62,     62,     30,     30,     0,      0,      0,      0,
    0,      0,      0,      0,      0,      0,      0,      0,
    0,      0,      0,      0,      0,      0,      0,      0,
    0,      0,      0,      0,      0,      0,      0,      0,
    3225,   3225,   3225,   3225,   3225,   3225,   3225,   3225,
    3225,   3225,   3225,   3225,   3225,   3225,   3225,   3225,
    3225,   3225,   3225,   3225,   3225,   3225,   3225,   3225,
    3225,   3225,   3225,   3225,   3225,   3225,   3225,   3225,
    588,    588,    588,    588,    588,    588,    588,    588,
    1680,   1680,  20499,  22547,  24595,  26643,   1776,   1776,
    1808,   1808, -24557, -22509, -20461, -18413,   1904,   1904,
    1936,   1936, -16365, -14317,    782,    782,    782,    782,
    814,    814,    814,    814, -12269, -10221,  10257,  10257,
    12305,  12305,  14353,  14353,  16403,  18451,   1712,   1712,
    1744,   1744,  28691,  30739, -32749, -30701, -28653, -26605,
    2061,   2061,   2061,   2061,   2061,   2061,   2061,   2061,
    424,    424,    424,    424,    424,    424,    424,    424,
    424,    424,    424,    424,    424,    424,    424,    424,
    424,    424,    424,    424,    424,    424,    424,    424,
    424,    424,    424,    424,    424,    424,    424,    424,
    750,    750,    750,    750,   1616,   1616,   1648,   1648,
    1424,   1424,   1456,   1456,   1488,   1488,   1520,   1520,
    1840,   1840,   1872,   1872,   1968,   1968,   8209,   8209,
    524,    524,    524,    524,    524,    524,    524,    524,
    556,    556,    556,    556,    556,    556,    556,    556,
    1552,   1552,   1584,   1584,   2000,   2000,   2032,   2032,
    976,    976,   1008,   1008,   1040,   1040,   1072,   1072,
    1296,   1296,   1328,   1328,    718,    718,    718,    718,
    456,    456,    456,    456,    456,    456,    456,    456,
    456,    456,    456,    456,    456,    456,    456,    456,
    456,    456,    456,    456,    456,    456,    456,    456,
    456,    456,    456,    456,    456,    456,    456,    456,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    326,    326,    326,    326,    326,    326,    326,    326,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    358,    358,    358,    358,    358,    358,    358,    358,
    490,    490,    490,    490,    490,    490,    490,    490,
    490,    490,    490,    490,    490,    490,    490,    490,
    4113,   4113,   6161,   6161,    848,    848,    880,    880,
    912,    912,    944,    944,    622,    622,    622,    622,
    654,    654,    654,    654,   1104,   1104,   1136,   1136,
    1168,   1168,   1200,   1200,   1232,   1232,   1264,   1264,
    686,    686,    686,    686,   1360,   1360,   1392,   1392,
    12,     12,     12,     12,     12,     12,     12,     12,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390,
    390,    390,    390,    390,    390,    390,    390,    390
  )

  val twoDCodes: Array[Byte] = Array(
    80,     88,     23,     71,     30,     30,     62,     62,
    4,      4,      4,      4,      4,      4,      4,      4,
    11,     11,     11,     11,     11,     11,     11,     11,
    11,     11,     11,     11,     11,     11,     11,     11,
    35,     35,     35,     35,     35,     35,     35,     35,
    35,     35,     35,     35,     35,     35,     35,     35,
    51,     51,     51,     51,     51,     51,     51,     51,
    51,     51,     51,     51,     51,     51,     51,     51,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41,
    41,     41,     41,     41,     41,     41,     41,     41
  )
}

class TiffFaxDecompressor(var fillOrder: Int, width: Int, height: Int) {

  import TiffFaxDecompressor._

  var bitPointer = 0
  var bytePointer = 0
  var inputData: Array[Byte] = _

  var changingElementsSize = 0
  var previousChangingElements = Array.ofDim[Int](width)
  var currentChangingElements = Array.ofDim[Int](width)

  var lastChangingElement = 0

  var compression = 2

  var uncompressedMode = 0
  var fillBits = 0
  var oneD = 0

  def decode1D(buffer: Array[Byte], input: Array[Byte], startX: Int, height: Int) = {

    var lineOffset = 0
    val scanlineStride = (width + 7) / 8

    inputData = input

    bitPointer = 0
    bytePointer = 0

    for (i <- 0 until height) {
      decodeNextScanline(buffer, lineOffset, startX)
      lineOffset += scanlineStride
    }
  }

  def decodeNextScanline(buffer: Array[Byte], lineOffset: Int, offset: Int) {
    var bits, code, isT, current, entry, twoBits, dstEnd = 0
    var bitOffset = offset
    changingElementsSize = 0
    var isWhite = true

    while (bitOffset < width) {
      while (isWhite) {
        current = nextNBits(10)
        entry = white(current)

        isT = entry & 0x0001
        bits = (entry >>> 1) & 0x0f

        if (bits == 12) {
          twoBits = nextLesserThan8Bits(2)
          current = ((current << 2) & 0x000c) | twoBits
          entry = additionalMakeup(current)
          bits = (entry >>> 1) & 0x07
          code = (entry >>> 4) & 0x0fff
          bitOffset += code

          updatePointer(4 - bits)
        }
        else if (bits == 0) throw new MalformedGeoTiffException("invalid code encountered.")
        else if (bits == 15) throw new MalformedGeoTiffException(
          "EOL code word encountered in white run."
        )
        else {
          code = (entry >>> 5) & 0x07ff
          bitOffset += code

          updatePointer(10 - bits)
          if (isT == 0) {
            isWhite = false
            currentChangingElements(changingElementsSize) = bitOffset
            changingElementsSize += 1
          }
        }
      }

      val done = if (bitOffset == width) {
        if (compression == 2) advancePointer
        true
      } else false

      while (isWhite == false && !done) {
        current = nextLesserThan8Bits(4)
        entry = initBlack(current)

        isT = entry & 0x0001
        bits = (entry >>> 1) & 0x000f
        code = (entry >>> 5) & 0x07ff

        if (code == 100) {
          current = nextNBits(9)
          entry = black(current)

          isT = entry & 0x0001
          bits = (entry >>> 1) & 0x000f
          code = (entry >>> 5) & 0x07ff

          if (bits == 12) {
            updatePointer(5)
            current = nextLesserThan8Bits(4)
            entry = additionalMakeup(current)

            bits = (entry >>> 1) & 0x07
            code = (entry >>> 4) & 0x0fff

            setToBlack(buffer, lineOffset, bitOffset, code)
            bitOffset += code

            updatePointer(4 - bits)
          } else if (bits == 15) throw new MalformedGeoTiffException(
            "EOL code word encountered in Black run."
          )
          else {
            setToBlack(buffer, lineOffset, bitOffset, code)
            bitOffset += code

            updatePointer(9 - bits)

            if (isT == 0) {
              isWhite = true
              currentChangingElements(changingElementsSize) = bitOffset
              changingElementsSize += 1
            }
          }
        } else if (code == 200) {
          current = nextLesserThan8Bits(2)
          entry = twoBitBlack(current)

          code = (entry >>> 5) & 0x07ff
          bits = (entry >>> 1) & 0x0f

          setToBlack(buffer, lineOffset, bitOffset, code)
          bitOffset += code

          updatePointer(2 - bits)
          isWhite = true
          currentChangingElements(changingElementsSize) = bitOffset
          changingElementsSize += 1
        } else {
          setToBlack(buffer, lineOffset, bitOffset, code)
          bitOffset += code

          updatePointer(4 - bits)
          isWhite = true

          currentChangingElements(changingElementsSize) = bitOffset
          changingElementsSize += 1
        }
      }

      if (bitOffset == width && compression == 2) advancePointer
    }

    currentChangingElements(changingElementsSize) = bitOffset
    changingElementsSize += 1
  }

  def decode2D(buffer: Array[Byte], input: Array[Byte], startX: Int,
    height: Int, tiffT4Options: Long) = {

    inputData = input

    compression = 3

    bitPointer = 0
    bytePointer = 0

    val scanlineStride = (width + 7) / 8

    var a0, a1, b1, b2 = 0
    val b = Array.ofDim[Int](2)

    var entry, code, bits, color = 0

    var isWhite = false
    var currentIndex = 0

    oneD = (tiffT4Options & 0x01).toInt
    uncompressedMode = ((tiffT4Options & 0x02) >> 1).toInt
    fillBits = ((tiffT4Options & 0x04) >> 2).toInt

    if (readEOL(true) != 1) throw new MalformedGeoTiffException(
      "First scanline must be 1D encoded."
    )

    var lineOffset, bitOffset = 0

    decodeNextScanline(buffer, lineOffset, startX)
    lineOffset += scanlineStride

    for (line <- 1 until height) {
      if (readEOL(false) == 0) {
        val temp = previousChangingElements
        previousChangingElements = currentChangingElements
        currentChangingElements = temp
        currentIndex = 0

        a0 = -1
        isWhite = true
        bitOffset = startX

        lastChangingElement = 0

        while (bitOffset < width) {
          getNextChangingElement(a0, isWhite, b)

          b1 = b(0)
          b2 = b(1)

          entry = nextLesserThan8Bits(7)

          entry = twoDCodes(entry) & 0x0fff

          code = (entry & 0x78) >>> 3
          bits = entry & 0x07

          if (code == 0) {
            if (!isWhite) setToBlack(buffer, lineOffset, bitOffset, b2 - bitOffset)

            a0 = b2
            bitOffset = b2

            updatePointer(7 - bits)
          } else if (code == 1) {
            updatePointer(7 - bits)

            if (isWhite) {
              val whiteNumber = decodeWhiteCodeWord
              bitOffset += whiteNumber
              currentChangingElements(currentIndex) = bitOffset
              currentIndex += 1

              val blackNumber = decodeBlackCodeWord
              setToBlack(buffer, lineOffset, bitOffset, blackNumber)
              bitOffset += blackNumber
              currentChangingElements(currentIndex) = bitOffset
              currentIndex += 1
            } else {
              val blackNumber = decodeBlackCodeWord
              setToBlack(buffer, lineOffset, bitOffset, blackNumber)
              bitOffset += blackNumber
              currentChangingElements(currentIndex) = bitOffset
              currentIndex += 1

              val whiteNumber = decodeWhiteCodeWord
              bitOffset += whiteNumber
              currentChangingElements(currentIndex) = bitOffset
              currentIndex += 1
            }

            a0 = bitOffset
          } else if (code <= 8) {

            a1 = b1 + (code - 5)

            currentChangingElements(currentIndex) = a1
            currentIndex += 1

            if (!isWhite) setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset)

            a0 = a1
            bitOffset = a1
            isWhite = !isWhite

            updatePointer(7 - bits)
          } else throw new MalformedGeoTiffException(
            "Invalid code encountered while decoding 2D group 3 compressed data."
          )
        }

        currentChangingElements(currentIndex) = bitOffset
        currentIndex += 1
        changingElementsSize = currentIndex
      } else decodeNextScanline(buffer, lineOffset, startX)

      lineOffset += scanlineStride
    }
  }

  def decodeT6(buffer: Array[Byte], input: Array[Byte], startX: Int,
    height: Int, tiffT6Options: Long) = {

    inputData = input

    compression = 4

    bitPointer = 0
    bytePointer = 0

    val scanlineStride = (width + 7) / 8
    var bufferOffset = 0

    var a0, a1, b1, b2, entry, code, bits = 0
    var color: Byte = 0
    var isWhite = false
    var currentIndex = 0

    val b: Array[Int] = Array.ofDim[Int](2)

    uncompressedMode = ((tiffT6Options & 0x02) >> 1).toInt

    if (uncompressedMode != 0) println(
      "Uncompressed mode is untested in Sun's FaxDecoder code."
    )

    var cce = currentChangingElements

    changingElementsSize = 0
    cce(changingElementsSize) = width
    changingElementsSize += 1
    cce(changingElementsSize) = width
    changingElementsSize += 1

    var lineOffset = 0
    var bitOffset = 0

    for (lines <- 0 until height) {
      a0 = -1
      isWhite = true

      val temp = previousChangingElements
      previousChangingElements = currentChangingElements
      currentChangingElements = temp
      cce = temp
      currentIndex = 0

      bitOffset = startX

      lastChangingElement = 0

      while (bitOffset < width) {
        getNextChangingElement(a0, isWhite, b)
        b1 = b(0)
        b2 = b(1)

        entry = nextLesserThan8Bits(7)
        entry = twoDCodes(entry) & 0xff

        code = (entry & 0x78) >>> 3
        bits = entry & 0x07

        if (code == 0) {
          if (!isWhite) setToBlack(buffer, lineOffset, bitOffset, b2 - bitOffset)

          a0 = b2
          bitOffset = b2

          updatePointer(7 - bits)
        } else if (code == 1) {
          updatePointer(7 - bits)

          if (isWhite) {
            val whiteNumber = decodeWhiteCodeWord
            bitOffset += whiteNumber
            cce(currentIndex) = bitOffset
            currentIndex += 1

            val blackNumber = decodeBlackCodeWord
            setToBlack(buffer, lineOffset, bitOffset, blackNumber)
            bitOffset += blackNumber
            cce(currentIndex) = bitOffset
            currentIndex += 1
          } else {
            val blackNumber = decodeBlackCodeWord
            setToBlack(buffer, lineOffset, bitOffset, blackNumber)
            bitOffset += blackNumber
            cce(currentIndex) = bitOffset
            currentIndex += 1

            val whiteNumber = decodeWhiteCodeWord
            bitOffset += whiteNumber
            cce(currentIndex) = bitOffset
            currentIndex += 1
          }
        } else if (code <= 8) {
          a1 = b1 + (code - 5)
          cce(currentIndex) = a1
          currentIndex += 1

          if (!isWhite) setToBlack(buffer, lineOffset, bitOffset, a1 - bitOffset)

          a0 = a1
          bitOffset = a1
          isWhite = !isWhite

          updatePointer(7 - bits)
        } else if (code == 11) {
          if (nextLesserThan8Bits(1) != 1) throw new MalformedGeoTiffException(
            "Invalid code encountered while decoding 2D group 4 compressed data."
          )

          var zeroes = 0
          var exit = false

          while (!exit) {
            while(nextLesserThan8Bits(1) != 1) zeroes += 1

            if (zeroes > 5) {
              zeroes = zeroes - 6

              if (!isWhite && zeroes > 0) {
                cce(currentIndex) = bitOffset
                currentIndex += 1
              }

              bitOffset += zeroes

              if (zeroes > 0) isWhite = true

              if (nextLesserThan8Bits(1) == 0) {
                if (!isWhite) {
                  cce(currentIndex) = bitOffset
                  currentIndex += 1
                }

                isWhite = true
              } else {
                if (isWhite) {
                  cce(currentIndex) = bitOffset
                  currentIndex += 1
                }

                isWhite = false
              }

              exit = true
            }

            if (zeroes == 5) {
              if (!isWhite) {
                cce(currentIndex) = bitOffset
                currentIndex += 1
              }

              bitOffset += zeroes

              isWhite = true
            } else {
              bitOffset += zeroes

              cce(currentIndex) = bitOffset
              currentIndex += 1
              setToBlack(buffer, lineOffset, bitOffset, 1)
              bitOffset += 1

              isWhite = false
            }
          }
        } else throw new MalformedGeoTiffException(
          "Invalid code encountered while decoding 2D group 4 compressed data."
        )
      }

      if (currentIndex < cce.length) {
        cce(currentIndex) = bitOffset
        currentIndex += 1
      }

      changingElementsSize = currentIndex
      lineOffset += scanlineStride
    }
  }

  private def setToBlack(buffer: Array[Byte], lineOffset: Int, bitOffset: Int,
    numBits: Int) {

    var bitNum = 8 * lineOffset + bitOffset
    val lastBit = bitNum + numBits

    var byteNum = bitNum >> 3

    val shift = bitNum & 0x07

    if (shift > 0) {
      var maskVal = 1 << (7 - shift)
      var value: Byte = buffer(byteNum)

      while (maskVal > 0 && bitNum < lastBit) {
        value = (value | maskVal).toByte
        maskVal = maskVal >> 1
        bitNum += 1
      }

      buffer(byteNum) = value
    }

    byteNum = bitNum >> 3
    while (bitNum < lastBit - 7) {
      buffer(byteNum) = 255.toByte
      byteNum += 1
      bitNum += 8
    }

    while (bitNum < lastBit) {
      byteNum = bitNum >> 3
      buffer(byteNum) = (buffer(byteNum) | (1 << (7 - (bitNum & 0x7)))).toByte
      bitNum += 1
    }
  }

  private def decodeWhiteCodeWord: Int = {
    var current, entry, bits, isT, twoBits, code = -1
    var runLength = 0
    var isWhite = true

    while (isWhite) {
      current = nextNBits(10)
      entry = white(current)

      isT = entry & 0x0001
      bits = (entry >>> 1) & 0x0f

      if (bits == 12) {
        twoBits = nextLesserThan8Bits(2)

        current = ((current << 2) & 0x000c) | twoBits
        entry = additionalMakeup(current)

        bits = (entry >>> 1) & 0x07
        code = (entry >>> 4) & 0x0fff

        runLength += code
        updatePointer(4 - bits)
      }
      else if (bits == 0) throw new MalformedGeoTiffException(
        "Invalid code encountered."
      )
      else if (bits == 15) throw new MalformedGeoTiffException(
        "EOL code word encountered in White run."
      )
      else {
        code = (entry >>> 5) & 0x07ff
        runLength += code
        updatePointer(10 - bits)

        if (isT == 0) isWhite = false
      }
    }

    runLength
  }

  private def decodeBlackCodeWord: Int = {
    var current, entry, bits, isT, twoBits, code = -1
    var runLength = 0
    var isWhite = false

    while (!isWhite) {
      current = nextLesserThan8Bits(4)
      entry = initBlack(current)

      isT = entry & 0x0001
      bits = (entry >>> 1) & 0x000f
      code = (entry >>> 5) & 0x07ff

      if (code == 100) {
        current = nextNBits(9)
        entry = black(current)

        isT = entry & 0x0001
        bits = (entry >>> 1) & 0x000f
        code = (entry >>> 5) & 0x07ff

        if (bits == 12) {
          updatePointer(5)

          current = nextLesserThan8Bits(4)

          entry = additionalMakeup(current)
          bits = (entry >>> 1) & 0x07
          code  = (entry >>> 4) & 0x0fff

          runLength += code

          updatePointer(4 - bits)
        }  else if (bits == 15) throw new MalformedGeoTiffException(
          "EOL code word encountered in Black run."
        ) else {
          runLength += code
          updatePointer(9 - bits)

          if (isT == 0) isWhite = true
        }
      } else if (code == 200) {
        current = nextLesserThan8Bits(2)

        entry = twoBitBlack(current)
        code = (entry >>> 5) & 0x07ff
        runLength += code

        bits = (entry >>> 1) & 0x0f
        updatePointer(2 - bits)

        isWhite = true
      } else {
        runLength += code
        updatePointer(4 - bits)
        isWhite = true
      }
    }

    runLength
  }

  private def readEOL(isFirstEOL: Boolean): Int = {
    var result = 0

    if (fillBits == 0) {
      val next12Bits = nextNBits(12)

      if (isFirstEOL && next12Bits == 0 && nextNBits(4) == 1) {
        fillBits = 1
        result = 1
      } else if (next12Bits != 1) throw new MalformedGeoTiffException(
        "Scanline must begin with EOL code word."
      )
    } else if (fillBits == 1) {
      var bitsLeft = 8 - bitPointer

      if (nextNBits(bitsLeft) != 0) throw new MalformedGeoTiffException(
        "All fill bits preceding EOL code must be 0."
      )

      if (bitsLeft < 4 && nextNBits(8) != 0) throw new MalformedGeoTiffException(
        "All fill bits preceding EOL code must be 0."
      )

      var n = 0
      while ({ n = nextNBits(8); n != 1 }) {

        if (n != 0) throw new MalformedGeoTiffException(
          "All fill bits preceding EOL code must be 0."
        )
      }
    }

    if (result == 1 || oneD == 0) 1
    else nextLesserThan8Bits(1)
  }

  private def getNextChangingElement(a0: Int, isWhite: Boolean, ret: Array[Int]) {
    val pce: Array[Int] = previousChangingElements
    val ces: Int = changingElementsSize

    var index = if (lastChangingElement > 0) lastChangingElement - 1 else 0

    if (isWhite) index = index & ~0x1
    else index = index | 0x1

    while (index < ces) {
      var temp = pce(index)

      if (temp > a0) {
        lastChangingElement = index
        ret(0) = temp
        index = ces
      }

      index += 2
    }

    if (index + 1 < ces) ret(1) = pce(index + 1)
  }

  private def nextNBits(bitsToGet: Int): Int = {
    var b, next, next2next: Byte = 0
    var l = inputData.length - 1
    var bp = bytePointer

    if (fillOrder == 1) {
      b = inputData(bp)

      if (bp == l) {
        next = 0x00
        next2next = 0x00
      } else if (bp + 1 == l) {
        next = inputData(bp + 1)
        next2next = 0x00
      } else {
        next = inputData(bp + 1)
        next2next = inputData(bp + 2)
      }
    } else if (fillOrder == 2) {
      b = flipTable(inputData(bp) & 0xff)

      if (bp == l) {
        next = 0x00
        next2next = 0x00
      } else if (bp + 1 == l) {
        next = flipTable(inputData(bp + 1) & 0xff)
        next2next = 0x00
      } else {
        next = flipTable(inputData(bp + 1) & 0xff)
        next2next = flipTable(inputData(bp + 2) & 0xff)
      }
    } else throw new MalformedGeoTiffException(
      "TIFF_FILL_ORDER tag must be either 1 or 2."
    )

    var bitsLeft = 8 - bitPointer
    var bitsFromNextByte = bitsToGet - bitsLeft
    var bitsFromNext2NextByte = 0

    if (bitsFromNextByte > 8) {
      bitsFromNext2NextByte = bitsFromNextByte - 8
      bitsFromNextByte = 8
    }

    bytePointer += 1

    var i1 = (b & table1(bitsLeft)) << (bitsToGet - bitsLeft)
    var i2 = (next & table2(bitsFromNextByte)) >>> (8 - bitsFromNextByte)

    var i3 = 0

    if (bitsFromNext2NextByte != 0) {
      i2 = i2 << bitsFromNext2NextByte
      i3 = (next2next & table2(bitsFromNext2NextByte)) >>> (8 - bitsFromNext2NextByte)
      i2 = i2 | i3

      bytePointer += 1
      bitPointer = bitsFromNext2NextByte
    } else {
      if (bitsFromNextByte == 8) {
        bitPointer = 0
        bytePointer += 1
      } else bitPointer = bitsFromNextByte
    }

    i1 | i2
  }

  private def nextLesserThan8Bits(bitsToGet: Int): Int = {
    var b, next: Byte = 0
    var l = inputData.length - 1
    var bp = bytePointer

    if (fillOrder == 1) {
      b = inputData(bp)

      if (bp == l) next = 0x00
      else next = inputData(bp + 1)
    } else if (fillOrder == 2) {
      b = flipTable(inputData(bp) & 0xff)

      if (bp == l) next = 0x00
      else next = flipTable(inputData(bp + 1) & 0xff)
    } else throw new MalformedGeoTiffException(
      "TIFF_FILL_ORDER tag must be either 1 or 2."
    )

    var bitsLeft = 8 - bitPointer
    var bitsFromNextByte = bitsToGet - bitsLeft

    var shift = bitsLeft - bitsToGet
    var i1, i2 = 0

    if (shift >= 0) {
      i1 = (b & table1(bitsLeft)) >>> shift
      bitPointer += bitsToGet

      if (bitPointer == 8) {
        bitPointer = 0
        bytePointer += 1
      }
    } else {
      i1 = (b & table1(bitsLeft)) << (-shift)
      i2 = (next & table2(bitsFromNextByte)) >>> (8 - bitsFromNextByte)

      i1 = i1 | i2
      bytePointer += 1
      bitPointer = bitsFromNextByte
    }

    i1
  }

  private def updatePointer(bitsToMoveBack: Int) {
    val i = bitPointer - bitsToMoveBack

    if (i < 0) {
      bytePointer -= 1
      bitPointer = 8 + 1
    } else bitPointer = i
  }

  private def advancePointer = if (bitPointer != 0) {
    bytePointer += 1
    bitPointer = 0
  }

}
