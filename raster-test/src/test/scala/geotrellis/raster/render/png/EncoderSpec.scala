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

package geotrellis.raster.render.png

import geotrellis.raster._
import geotrellis.raster.render._
import geotrellis.vector.Extent

import scala.io.{Source}

import java.io.{File, FileInputStream}

import org.scalatest._

class EncoderSpec extends FunSpec with Matchers {

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

  val rgba = parseHexString("""
89 50 4e 47 0d 0a 1a 0a 00 00 00 0d 49 48 44 52
00 00 00 0a 00 00 00 0a 08 06 00 00 00 8d 32 cf
bd 00 00 00 42 49 44 41 54 78 01 85 ce 5b 0a 00
20 08 44 d1 11 dc ff 96 4b 2d e8 a5 93 20 fe 1c
2e 2a 80 06 88 1d be 3a 9c 23 3e 0a b1 60 44 39
9e d0 6b fe 41 8d 37 c8 f1 05 6b 9c c0 1c 17 f0
c5 04 9e f8 03 17 ee 04 00 12 3a 47 85 ae b3 00
00 00 00 49 45 4e 44 ae 42 60 82""")

  val w = 10
  val h = 10
  val data = Array.ofDim[Int](h * w)
  for(row <- 0 until h) {
    for(col <- 0 until w) {
      data(row * w + col) = ((row * 0xFF + col) << 8) + (255)
    }
  }

  val tile = ArrayTile(data, w, h)

  describe("RgbaEncoder") {
    val fh = File.createTempFile("rgba-", ".png")
    val path = fh.getPath

    val encoder = Encoder(Settings(Rgba, PaethFilter))

    it("should render a PNG") {
      encoder.writeByteArray(tile)
    }

    it("should write a PNG") {
      encoder.writePath(path, tile)
    }
    
    it("should write and render the same thing") {
      val bytes1 = encoder.writeByteArray(tile)
    
      val fis = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)
      
      bytes1 should be (bytes2)
    }
    
    it("should match the reference") {
      val bytes1 = encoder.writeByteArray(tile)
      bytes1 should be (rgba)
    }

    fh.delete()
  }

  describe("RgbEncoder") {
    val fh = File.createTempFile("rgb-", ".png")
    val path = fh.getPath
  
    val encoder = Encoder(Settings(Rgb(0), PaethFilter))
  
    // map RGBA values into RGB
    val tile2 = tile.map(_ >> 8)
  
    it("should write a PNG") {
      encoder.writePath(path, tile2)
    }
  
    it("should render a PNG") {
      encoder.writeByteArray(tile2)
    }
  
    it("should write and render the same thing") {
      val bytes1 = encoder.writeByteArray(tile2)
  
      val fis = new FileInputStream(fh)
      val bytes2 = Array.ofDim[Byte](fh.length.toInt)
      fis.read(bytes2)
      
      bytes1 should be (bytes2)
    }
  
    it("should match the reference") {
      val bytes1 = encoder.writeByteArray(tile2)
      println(bytes1.length)
      println(tile2.cols * tile2.rows)
      println(bytes1.toSeq)
      println(rgb.toSeq)
      bytes1 should be (rgb)
    }
  
    fh.delete()
  }
}
