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

package geotrellis.raster.render.jpg

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.vector.Extent

import scala.io.{Source}

import java.io.{File, FileInputStream}

import org.scalatest._

class JpgEncoderSpec extends FunSpec with Matchers {

  def parseHexByte(s: String) = Integer.parseInt(s, 16).toByte
  def parseHexBytes(cs: Array[String]) = cs.map(parseHexByte _)
  def parseHexString(s: String) = parseHexBytes(s.trim.replace("\n", " ").split(" "))

  val rgb = parseHexString("""
89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52
00 00 00 0a 00 00 00 0a 08 02 00 00 00 02 50 58
ea 00 00 00 06 74 52 4e 53 00 00 00 00 00 00 6e
a6 07 91 00 00 00 3c 49 44 41 54 78 01 7d cc 59
0e 00 20 08 03 d1 92 f4 fe 57 46 8c c6 05 8b 84
bf 37 40 f4 b1 6a 09 38 2c 58 0f 61 fe 29 06 c7
a9 fe b1 58 17 27 8b 22 71 2e 5e be 0a c9 bb a8
78 16 0d 5b 6c 11 3b 06 db 3d 95 00 00 00 00 49
45 4e 44 ae 42 60 82""")

  val w = 10
  val h = 10
  val data = Array.ofDim[Int](h * w)
  for(row <- 0 until h) {
    for(col <- 0 until w) {
      data(row * w + col) = ((row * 0xFF + col) << 8) + (255)
    }
  }

  val tile = ArrayTile(data, w, h)

  describe("JpgEncoder") {
    val fh = File.createTempFile("rgb-", ".jpg")
    val path = fh.getPath

    val encoder = new JpgEncoder

    it("should render a jpg") {
      encoder.writeByteArray(tile)
    }

    it("should write a jpg") {
      encoder.writePath(path, tile)
    }

    it("should write and render the same thing") {
      val bytes1 = encoder.writeByteArray(tile)

      val fis = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)

      bytes1 should be (bytes2)
    }

    it("should produce different results for different compressions") {
      val defaultEncoder = new JpgEncoder
      val losslessEncoder = new JpgEncoder(Settings.LOSSLESS)

      val bytes1 = defaultEncoder.writeByteArray(tile)
      val bytes2 = losslessEncoder.writeByteArray(tile)

      bytes1 should not be (bytes2)
    }

    it("should produce the same results for the same compressions") {
      val enc1 = new JpgEncoder(Settings(0.3, false))
      val enc2 = new JpgEncoder(Settings(0.3, false))

      val bytes1 = enc1.writeByteArray(tile)
      val bytes2 = enc2.writeByteArray(tile)

      bytes1 should be (bytes2)
    }

    it("should produce huffman tables if set to optimize") {
      val enc1 = new JpgEncoder(Settings(0.3, false))
      val enc2 = new JpgEncoder(Settings(0.3, true))

      val bytes1 = enc1.writeByteArray(tile)
      val bytes2 = enc2.writeByteArray(tile)

      bytes1 should not be (bytes2)
    }

    fh.delete()
  }
}
